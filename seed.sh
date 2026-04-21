#!/bin/bash

# ============================================
#   MindCare - Data Seeder
#   Hits services DIRECTLY (bypasses gateway)
#   to avoid routing issues.
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Direct service URLs
USERS="http://localhost:8081"
INCIDENTS="http://localhost:8083"
FORUMS="http://localhost:8082"

# MySQL via Docker container (container name from docker-compose.yml)
MYSQL_CMD="docker exec alzcare-mysql mysql -u root --silent"

ok()   { echo -e "  ${GREEN}[OK]${NC} $1"; }
err()  { echo -e "  ${RED}[FAIL]${NC} $1"; }
sec()  { echo -e "\n${CYAN}━━━ $1 ━━━${NC}"; }
info() { echo -e "  ${YELLOW}$1${NC}"; }

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   MindCare - Seeding Data                  ${NC}"
echo -e "${GREEN}============================================${NC}"

# --------------------------------------------------
# STEP 0 — RESET (optional but recommended)
# --------------------------------------------------
echo
echo -e "${YELLOW}Voulez-vous RESET les bases avant de seeder ?${NC}"
echo -e "  ${RED}(Supprime TOUT : users, incidents, forum)${NC}"
read -p "Reset ? (y/n): " DO_RESET

if [[ "$DO_RESET" == "y" || "$DO_RESET" == "Y" ]]; then
  sec "Resetting databases..."

  if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "alzcare-mysql"; then
    # incident_db
    $MYSQL_CMD -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE incident_db.incident_comment; TRUNCATE incident_db.incident; TRUNCATE incident_db.incident_type; SET FOREIGN_KEY_CHECKS=1;" \
      2>/dev/null && ok "incident_db cleared" || err "incident_db reset failed"

    # forum_db
    $MYSQL_CMD -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE forum_db.comment; TRUNCATE forum_db.post; TRUNCATE forum_db.category; SET FOREIGN_KEY_CHECKS=1;" \
      2>/dev/null && ok "forum_db cleared" || err "forum_db reset failed"

    # alzheimer_db — wipe everything
    $MYSQL_CMD -e "DELETE FROM alzheimer_db.mood_entries WHERE 1=1; DELETE FROM alzheimer_db.users WHERE 1=1;" \
      2>/dev/null && ok "alzheimer_db cleared" || err "alzheimer_db reset failed"

  else
    err "Container alzcare-mysql not running — start Docker first: bash start-db.sh"
    exit 1
  fi
fi

# --------------------------------------------------
# CONNECTIVITY CHECK
# --------------------------------------------------
sec "Checking services..."

check_service() {
  local NAME=$1; local URL=$2
  local STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$URL")
  if [ "$STATUS" != "000" ]; then
    ok "$NAME is UP ($URL) → HTTP $STATUS"
    return 0
  else
    err "$NAME is DOWN ($URL) — is it started?"
    return 1
  fi
}

check_service "users_service   " "$USERS/api/users"
check_service "incident_service" "$INCIDENTS/api/incident-types"
check_service "forums_service  " "$FORUMS/api/categories"

echo
read -p "Continue seeding? (y/n): " CONFIRM
[[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]] && echo "Aborted." && exit 0

# --------------------------------------------------
# Helper: POST and extract numeric ID from response
# --------------------------------------------------
post_get_id() {
  local URL=$1; local BODY=$2; local FIELD=$3
  local RESP=$(curl -s -X POST "$URL" -H "Content-Type: application/json" -d "$BODY")
  local ID=$(echo "$RESP" | grep -o "\"$FIELD\":[0-9]*" | head -1 | grep -o '[0-9]*')
  echo "$ID"
}

# --------------------------------------------------
# Helper: GET or CREATE an incident type by name
# Uses python3 for reliable JSON parsing
# --------------------------------------------------
get_or_create_type() {
  local NAME=$1; local DESC=$2; local SEV=$3; local PTS=$4
  local ALL=$(curl -s "$INCIDENTS/api/incident-types")

  # Try python3 first (reliable)
  local ID=""
  if command -v python3 &> /dev/null; then
    ID=$(echo "$ALL" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    for t in data:
        if t.get('name') == sys.argv[1]:
            print(t['id'])
            break
except: pass
" "$NAME" 2>/dev/null)
  fi

  # Fallback: grep
  if [ -z "$ID" ]; then
    ID=$(echo "$ALL" | grep -o "\"name\":\"$NAME\"" | head -1)
    if [ -n "$ID" ]; then
      ID=$(echo "$ALL" | grep -B2 "\"name\":\"$NAME\"" | grep -o '"id":[0-9]*' | grep -o '[0-9]*' | head -1)
    fi
  fi

  if [ -n "$ID" ]; then
    echo "$ID"
  else
    # Not found — create it
    local BODY="{\"name\":\"$NAME\",\"description\":\"$DESC\",\"defaultSeverity\":\"$SEV\",\"points\":$PTS}"
    local RESP=$(curl -s -X POST "$INCIDENTS/api/incident-types" -H "Content-Type: application/json" -d "$BODY")
    echo "$RESP" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'
  fi
}

# --------------------------------------------------
# Helper: GET or CREATE a forum category by name
# --------------------------------------------------
get_or_create_category() {
  local NAME=$1; local DESC=$2
  local ALL=$(curl -s "$FORUMS/api/categories")
  local ID=""

  if command -v python3 &> /dev/null; then
    ID=$(echo "$ALL" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    for c in data:
        if c.get('name') == sys.argv[1]:
            print(c['id'])
            break
except: pass
" "$NAME" 2>/dev/null)
  fi

  if [ -n "$ID" ]; then
    echo "$ID"
  else
    local RESP=$(curl -s -X POST "$FORUMS/api/categories" -H "Content-Type: application/json" \
      -d "{\"name\":\"$NAME\",\"description\":\"$DESC\"}")
    echo "$RESP" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*'
  fi
}

# --------------------------------------------------
# 1. USERS  (POST /api/users/register)
# --------------------------------------------------
sec "Creating users..."

register_or_login() {
  local LABEL=$1; local BODY=$2; local EMAIL=$3; local PASS=$4
  local ID=$(post_get_id "$USERS/api/users/register" "$BODY" "userId")
  if [ -n "$ID" ]; then
    ok "$LABEL created — ID: $ID" >&2
  else
    ID=$(curl -s -X POST "$USERS/api/users/login?email=$EMAIL&password=$PASS" \
      | grep -o '"userId":[0-9]*' | grep -o '[0-9]*')
    [ -n "$ID" ] && ok "$LABEL already exists — ID: $ID" >&2 || err "$LABEL failed" >&2
  fi
  echo "$ID"
}

ADMIN_ID=$(register_or_login "ADMIN" \
  '{"firstName":"Admin","lastName":"MindCare","email":"admin@alzcare.com","password":"Admin@123","phone":"0600000001","role":"ADMIN"}' \
  "admin@alzcare.com" "Admin@123")

CAREGIVER_ID=$(register_or_login "CAREGIVER" \
  '{"firstName":"Sophie","lastName":"Martin","email":"caregiver@alzcare.com","password":"Caregiver@123","phone":"0600000002","role":"CAREGIVER"}' \
  "caregiver@alzcare.com" "Caregiver@123")

DOCTOR_ID=$(register_or_login "DOCTOR" \
  '{"firstName":"Karim","lastName":"Benali","email":"doctor@alzcare.com","password":"Doctor@123","phone":"0600000003","role":"DOCTOR"}' \
  "doctor@alzcare.com" "Doctor@123")

P1_ID=$(register_or_login "PATIENT 1 (Amena)" \
  '{"firstName":"Amena","lastName":"Jlassi","email":"patient1@alzcare.com","password":"Patient@123","phone":"0600000004","role":"PATIENT"}' \
  "patient1@alzcare.com" "Patient@123")

P2_ID=$(register_or_login "PATIENT 2 (Mohamed)" \
  '{"firstName":"Mohamed","lastName":"Trabelsi","email":"patient2@alzcare.com","password":"Patient@123","phone":"0600000005","role":"PATIENT"}' \
  "patient2@alzcare.com" "Patient@123")

# --------------------------------------------------
# 2. ASSIGN CAREGIVER TO PATIENTS
# --------------------------------------------------
sec "Assigning caregiver to patients..."

if [ -n "$CAREGIVER_ID" ] && [ -n "$P1_ID" ]; then
  curl -s -X PUT "$USERS/api/users/$P1_ID" \
    -H "Content-Type: application/json" \
    -d "{\"firstName\":\"Amena\",\"lastName\":\"Jlassi\",\"phone\":\"0600000004\",\"role\":\"PATIENT\",\"caregiverId\":$CAREGIVER_ID}" > /dev/null
  ok "Amena (ID:$P1_ID) → Sophie (ID:$CAREGIVER_ID)"
fi

if [ -n "$CAREGIVER_ID" ] && [ -n "$P2_ID" ]; then
  curl -s -X PUT "$USERS/api/users/$P2_ID" \
    -H "Content-Type: application/json" \
    -d "{\"firstName\":\"Mohamed\",\"lastName\":\"Trabelsi\",\"phone\":\"0600000005\",\"role\":\"PATIENT\",\"caregiverId\":$CAREGIVER_ID}" > /dev/null
  ok "Mohamed (ID:$P2_ID) → Sophie (ID:$CAREGIVER_ID)"
fi

# --------------------------------------------------
# 3. INCIDENT TYPES  (with scoring)
# --------------------------------------------------
sec "Creating incident types..."

T1_ID=$(get_or_create_type "Chute"                 "Le patient est tombe"                    "HIGH"     25)
[ -n "$T1_ID" ] && ok "Chute (ID:$T1_ID)" || err "Chute failed"

T2_ID=$(get_or_create_type "Comportement agressif" "Manifestation agressive"                 "HIGH"     20)
[ -n "$T2_ID" ] && ok "Comportement agressif (ID:$T2_ID)" || err "Comportement agressif failed"

T3_ID=$(get_or_create_type "Oubli medicament"      "Le patient a oublie ses medicaments"     "MEDIUM"   15)
[ -n "$T3_ID" ] && ok "Oubli medicament (ID:$T3_ID)" || err "Oubli medicament failed"

T4_ID=$(get_or_create_type "Deambulation"          "Le patient erre sans but"                "MEDIUM"   12)
[ -n "$T4_ID" ] && ok "Deambulation (ID:$T4_ID)" || err "Deambulation failed"

T5_ID=$(get_or_create_type "Detresse emotionnelle" "Confusion ou detresse intense"           "HIGH"     18)
[ -n "$T5_ID" ] && ok "Detresse emotionnelle (ID:$T5_ID)" || err "Detresse emotionnelle failed"

# Fallback type ID
FALLBACK_T=${T1_ID:-${T2_ID:-$T3_ID}}

# --------------------------------------------------
# 4. INCIDENTS
# --------------------------------------------------
sec "Creating incidents..."

CG=${CAREGIVER_ID:-1}

if [ -n "$P1_ID" ] && [ -n "$T1_ID" ]; then
  curl -s -X POST "$INCIDENTS/api/incidents" -H "Content-Type: application/json" \
    -d "{\"type\":{\"id\":$T1_ID},\"description\":\"Amena a chute dans le couloir ce matin. Legere douleur au genou gauche.\",\"severityLevel\":\"MEDIUM\",\"status\":\"OPEN\",\"source\":\"CAREGIVER\",\"patientId\":$P1_ID,\"caregiverId\":$CG}" > /dev/null
  ok "Incident Chute → Amena"
fi

if [ -n "$P1_ID" ] && [ -n "$T3_ID" ]; then
  curl -s -X POST "$INCIDENTS/api/incidents" -H "Content-Type: application/json" \
    -d "{\"type\":{\"id\":$T3_ID},\"description\":\"Amena a refuse ses medicaments du soir et s est montree agitee.\",\"severityLevel\":\"HIGH\",\"status\":\"IN_PROGRESS\",\"source\":\"CAREGIVER\",\"patientId\":$P1_ID,\"caregiverId\":$CG}" > /dev/null
  ok "Incident Medicament → Amena"
fi

if [ -n "$P2_ID" ] && [ -n "$T2_ID" ]; then
  curl -s -X POST "$INCIDENTS/api/incidents" -H "Content-Type: application/json" \
    -d "{\"type\":{\"id\":$T2_ID},\"description\":\"Mohamed a montre un comportement agressif envers le personnel.\",\"severityLevel\":\"HIGH\",\"status\":\"OPEN\",\"source\":\"CAREGIVER\",\"patientId\":$P2_ID,\"caregiverId\":$CG}" > /dev/null
  ok "Incident Agressif → Mohamed"
fi

if [ -n "$P2_ID" ] && [ -n "$T4_ID" ]; then
  curl -s -X POST "$INCIDENTS/api/incidents" -H "Content-Type: application/json" \
    -d "{\"type\":{\"id\":$T4_ID},\"description\":\"Mohamed retrouve deambulant dans le jardin a 3h du matin.\",\"severityLevel\":\"MEDIUM\",\"status\":\"RESOLVED\",\"source\":\"PATIENT\",\"patientId\":$P2_ID}" > /dev/null
  ok "Incident Deambulation → Mohamed"
fi

# --------------------------------------------------
# 5. FORUM CATEGORIES
# --------------------------------------------------
sec "Creating forum categories..."

C1_ID=$(get_or_create_category "Conseils et Soins"    "Conseils pour soigner les patients")
[ -n "$C1_ID" ] && ok "Conseils et Soins (ID:$C1_ID)" || err "Conseils et Soins failed"

C2_ID=$(get_or_create_category "Soutien Emotionnel"   "Espace de soutien pour familles et soignants")
[ -n "$C2_ID" ] && ok "Soutien Emotionnel (ID:$C2_ID)" || err "Soutien Emotionnel failed"

C3_ID=$(get_or_create_category "Actualites Medicales" "Recherches sur la maladie Alzheimer")
[ -n "$C3_ID" ] && ok "Actualites Medicales (ID:$C3_ID)" || err "Actualites Medicales failed"

# --------------------------------------------------
# 6. FORUM POSTS
# --------------------------------------------------
sec "Creating forum posts..."

if [ -n "$C1_ID" ] && [ -n "$CAREGIVER_ID" ]; then
  curl -s -X POST "$FORUMS/api/posts" -H "Content-Type: application/json" \
    -d "{\"title\":\"Comment gerer les crises agitation\",\"content\":\"Mon patient fait souvent des crises le soir. La musique douce aide beaucoup. Avez-vous d autres conseils ?\",\"status\":\"PUBLISHED\",\"userId\":$CAREGIVER_ID,\"category\":{\"id\":$C1_ID}}" > /dev/null
  ok "Post: Gestion agitation"
fi

if [ -n "$C2_ID" ] && [ -n "$P1_ID" ]; then
  curl -s -X POST "$FORUMS/api/posts" -H "Content-Type: application/json" \
    -d "{\"title\":\"Vivre avec Alzheimer\",\"content\":\"C est difficile au quotidien mais je reste positif. Les moments de lucidite sont precieux.\",\"status\":\"PUBLISHED\",\"userId\":$P1_ID,\"category\":{\"id\":$C2_ID}}" > /dev/null
  ok "Post: Vivre avec Alzheimer"
fi

if [ -n "$C3_ID" ] && [ -n "$DOCTOR_ID" ]; then
  curl -s -X POST "$FORUMS/api/posts" -H "Content-Type: application/json" \
    -d "{\"title\":\"Nouvelles therapies contre Alzheimer\",\"content\":\"Les dernières etudes montrent des resultats prometteurs avec l immunotherapie. Voici ce qu il faut savoir.\",\"status\":\"PUBLISHED\",\"userId\":$DOCTOR_ID,\"category\":{\"id\":$C3_ID}}" > /dev/null
  ok "Post: Nouvelles therapies"
fi

# --------------------------------------------------
# Summary
# --------------------------------------------------
echo
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Seeding termine !                       ${NC}"
echo -e "${GREEN}============================================${NC}"
echo
echo -e "${YELLOW}IDs crees :${NC}"
echo "  ADMIN=$ADMIN_ID  CAREGIVER=$CAREGIVER_ID  DOCTOR=$DOCTOR_ID"
echo "  PATIENT1=$P1_ID  PATIENT2=$P2_ID"
echo "  TYPES: T1=$T1_ID T2=$T2_ID T3=$T3_ID T4=$T4_ID T5=$T5_ID"
echo
echo -e "${YELLOW}Comptes de test :${NC}"
echo -e "  ${CYAN}ADMIN${NC}      admin@alzcare.com      / Admin@123"
echo -e "  ${CYAN}CAREGIVER${NC}  caregiver@alzcare.com  / Caregiver@123"
echo -e "  ${CYAN}DOCTOR${NC}     doctor@alzcare.com     / Doctor@123"
echo -e "  ${CYAN}PATIENT${NC}    patient1@alzcare.com   / Patient@123"
echo -e "  ${CYAN}PATIENT${NC}    patient2@alzcare.com   / Patient@123"
echo
echo -e "${YELLOW}Testez sur : ${CYAN}http://localhost:4200${NC}"
