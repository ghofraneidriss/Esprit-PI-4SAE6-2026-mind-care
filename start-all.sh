#!/bin/bash

# ============================================
#   AlzCare - Launcher (WSL / Ubuntu)
#   Tous les services tournent en arriere-plan
#   Les logs sont dans le dossier logs/
#   NOTE: Lancez d'abord : bash start-db.sh
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

ROOT="$(cd "$(dirname "$0")" && pwd)"
SERVER="$ROOT/server"
FRONT="$ROOT/front"
LOGS="$ROOT/logs"

# Fichier qui stocke les PIDs pour pouvoir tout arreter ensuite
PIDFILE="$ROOT/logs/pids.txt"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   AlzCare - Demarrage (mode background)   ${NC}"
echo -e "${GREEN}============================================${NC}"
echo

# Creer le dossier logs
mkdir -p "$LOGS"

# Vider les anciens logs et PIDs
rm -f "$LOGS"/*.log "$PIDFILE"
echo -e "${YELLOW}Logs sauvegardes dans : $LOGS${NC}"
echo

# -----------------------------------------------
# Fonction : lancer un service en arriere-plan
# Usage: start_service "nom" "dossier" "commande" "logfile"
# -----------------------------------------------
start_service() {
    local NAME="$1"
    local DIR="$2"
    local CMD="$3"
    local LOGFILE="$4"

    echo -e "${CYAN}>>> Demarrage $NAME...${NC}"
    echo -e "    Log : $LOGFILE"

    # Lance en arriere-plan, stdout+stderr -> fichier log
    ( cd "$DIR" && eval "$CMD" >> "$LOGFILE" 2>&1 ) &
    local PID=$!

    # Sauvegarder le PID pour stop-all.sh
    echo "$NAME=$PID" >> "$PIDFILE"
    echo -e "    PID : $PID"
    echo
}

# -----------------------------------------------
# 1. Eureka Server (port 8761)
# -----------------------------------------------
echo -e "${YELLOW}[1/6]${NC}"
start_service "eureka_server" \
    "$SERVER/eureka_server" \
    "mvn spring-boot:run" \
    "$LOGS/eureka.log"

echo -e "${YELLOW}    Attente demarrage Eureka (20s)...${NC}"
sleep 20
echo

# -----------------------------------------------
# 2. Forums Service (port 8086)
# -----------------------------------------------
echo -e "${YELLOW}[2/6]${NC}"
start_service "forums_service" \
    "$SERVER/forums_service" \
    "mvn spring-boot:run" \
    "$LOGS/forums.log"
sleep 5

# -----------------------------------------------
# 3. Incident Service (port 8087)
# -----------------------------------------------
echo -e "${YELLOW}[3/6]${NC}"
start_service "incident_service" \
    "$SERVER/incident_service" \
    "mvn spring-boot:run" \
    "$LOGS/incident.log"
sleep 5

# -----------------------------------------------
# 4. Users Service (port 8081)
# -----------------------------------------------
echo -e "${YELLOW}[4/6]${NC}"
start_service "users_service" \
    "$SERVER/users_service" \
    "mvn spring-boot:run" \
    "$LOGS/users.log"

# -----------------------------------------------
# 5. API Gateway (port 8085)
# -----------------------------------------------
echo -e "${YELLOW}    Attente enregistrement Eureka (25s)...${NC}"
sleep 25
echo -e "${YELLOW}[5/6]${NC}"
start_service "api_gateway" \
    "$SERVER/api_gateway" \
    "mvn spring-boot:run" \
    "$LOGS/gateway.log"
sleep 10

# -----------------------------------------------
# 6. Angular Frontend (port 4200)
# -----------------------------------------------
echo -e "${YELLOW}[6/6]${NC}"
start_service "angular_frontend" \
    "$FRONT" \
    "npm start" \
    "$LOGS/angular.log"

# -----------------------------------------------
# Resume
# -----------------------------------------------
echo
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Tous les services sont lances !          ${NC}"
echo -e "${GREEN}============================================${NC}"
echo
echo -e "${YELLOW}Fichiers de logs :${NC}"
echo "  $LOGS/eureka.log"
echo "  $LOGS/forums.log"
echo "  $LOGS/incident.log"
echo "  $LOGS/users.log"
echo "  $LOGS/gateway.log"
echo "  $LOGS/angular.log"
echo
echo -e "${YELLOW}Suivre un log en temps reel :${NC}"
echo "  tail -f $LOGS/eureka.log"
echo "  tail -f $LOGS/angular.log"
echo "  tail -f $LOGS/gateway.log"
echo
echo -e "${CYAN}  Eureka Dashboard  : http://localhost:8761${NC}"
echo -e "${CYAN}  API Gateway       : http://localhost:8085${NC}"
echo -e "${CYAN}  Angular App       : http://localhost:4200${NC}"
echo -e "${CYAN}  Forums Swagger    : http://localhost:8086/swagger-ui.html${NC}"
echo -e "${CYAN}  Incident Swagger  : http://localhost:8087/swagger-ui.html${NC}"
echo -e "${CYAN}  Users Swagger     : http://localhost:8081/swagger-ui.html${NC}"
echo
echo -e "${YELLOW}Pour tout arreter : bash stop-all.sh${NC}"
echo -e "${YELLOW}PIDs sauvegardes  : $PIDFILE${NC}"
