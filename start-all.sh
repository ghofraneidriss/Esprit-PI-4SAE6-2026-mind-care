#!/bin/bash

# ============================================
#   MindCare - Launcher (WSL / Ubuntu)
#   Ordre : Eureka → users → forums → incident → activities → localization → movement
#           → recommendation → souvenir → attente → gateway → Angular
#   Ports : 8761 | 8081..8088 | 8080 (gateway) | 4200
#   Lancez d'abord MySQL (XAMPP ou : bash start-db.sh)
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

PIDFILE="$ROOT/logs/pids.txt"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   MindCare - Demarrage (mode background)   ${NC}"
echo -e "${GREEN}============================================${NC}"
echo

mkdir -p "$LOGS"
rm -f "$LOGS"/*.log "$PIDFILE"
echo -e "${YELLOW}Logs sauvegardes dans : $LOGS${NC}"
echo

start_service() {
    local NAME="$1"
    local DIR="$2"
    local CMD="$3"
    local LOGFILE="$4"

    echo -e "${CYAN}>>> Demarrage $NAME...${NC}"
    echo -e "    Log : $LOGFILE"

    ( cd "$DIR" && eval "$CMD" >> "$LOGFILE" 2>&1 ) &
    local PID=$!

    echo "$NAME=$PID" >> "$PIDFILE"
    echo -e "    PID : $PID"
    echo
}

# [1/10] Eureka (8761)
echo -e "${YELLOW}[1/10] Eureka Server${NC}"
start_service "eureka_server" \
    "$SERVER/eureka_server" \
    "mvn spring-boot:run" \
    "$LOGS/eureka.log"

echo -e "${YELLOW}    Attente demarrage Eureka (20s)...${NC}"
sleep 20
echo

# [2/10] Users (8081)
echo -e "${YELLOW}[2/10] Users Service${NC}"
start_service "users_service" \
    "$SERVER/users_service" \
    "mvn spring-boot:run" \
    "$LOGS/users.log"
sleep 5

# [3/10] Forums (8082)
echo -e "${YELLOW}[3/10] Forums Service${NC}"
start_service "forums_service" \
    "$SERVER/forums_service" \
    "mvn spring-boot:run" \
    "$LOGS/forums.log"
sleep 5

# [4/10] Incident (8083)
echo -e "${YELLOW}[4/10] Incident Service${NC}"
start_service "incident_service" \
    "$SERVER/incident_service" \
    "mvn spring-boot:run" \
    "$LOGS/incident.log"
sleep 5

# [5/10] Activities (8084)
echo -e "${YELLOW}[5/10] Activities Service${NC}"
start_service "activities_service" \
    "$SERVER/activities_service" \
    "mvn spring-boot:run" \
    "$LOGS/activities.log"
sleep 5

# [6/10] Localization (8085)
echo -e "${YELLOW}[6/10] Localization Service${NC}"
start_service "localization_service" \
    "$SERVER/localization_service" \
    "mvn spring-boot:run" \
    "$LOGS/localization.log"
sleep 5

# [7/10] Movement (8086)
echo -e "${YELLOW}[7/10] Movement Service${NC}"
start_service "movement_service" \
    "$SERVER/movement_service" \
    "mvn spring-boot:run" \
    "$LOGS/movement.log"
sleep 5

# [8/10] Recommendation (8087)
echo -e "${YELLOW}[8/10] Recommendation Service${NC}"
start_service "recommendation_service" \
    "$SERVER/recommendation_service/recommendation_service" \
    "mvn spring-boot:run" \
    "$LOGS/recommendation.log"
sleep 5

# [9/10] Souvenir (8088)
echo -e "${YELLOW}[9/10] Souvenir Service${NC}"
start_service "souvenir_service" \
    "$SERVER/souvenir_service/souvenir_service" \
    "mvn spring-boot:run" \
    "$LOGS/souvenir.log"
sleep 5

# [10/10] API Gateway (8080) — après enregistrement Eureka
echo -e "${YELLOW}    Attente enregistrement Eureka (25s)...${NC}"
sleep 25
echo -e "${YELLOW}[10/10] API Gateway${NC}"
start_service "api_gateway" \
    "$SERVER/api_gateway" \
    "mvn spring-boot:run" \
    "$LOGS/gateway.log"
sleep 10

# [11/11] Angular (4200)
echo -e "${YELLOW}[11/11] Angular Frontend${NC}"
start_service "angular_frontend" \
    "$FRONT" \
    "npm start" \
    "$LOGS/angular.log"

echo
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Tous les services sont lances !          ${NC}"
echo -e "${GREEN}============================================${NC}"
echo
echo -e "${YELLOW}Fichiers de logs :${NC}"
echo "  $LOGS/eureka.log"
echo "  $LOGS/users.log"
echo "  $LOGS/forums.log"
echo "  $LOGS/incident.log"
echo "  $LOGS/activities.log"
echo "  $LOGS/localization.log"
echo "  $LOGS/movement.log"
echo "  $LOGS/recommendation.log"
echo "  $LOGS/souvenir.log"
echo "  $LOGS/gateway.log"
echo "  $LOGS/angular.log"
echo
echo -e "${CYAN}  Eureka Dashboard   : http://localhost:8761${NC}"
echo -e "${CYAN}  API Gateway (front) : http://localhost:8080${NC}"
echo -e "${CYAN}  Angular App         : http://localhost:4200${NC}"
echo -e "${CYAN}  Users Swagger       : http://localhost:8081/swagger-ui.html${NC}"
echo -e "${CYAN}  Forums Swagger      : http://localhost:8082/swagger-ui.html${NC}"
echo -e "${CYAN}  Incident Swagger    : http://localhost:8083/swagger-ui.html${NC}"
echo -e "${CYAN}  Activities Swagger  : http://localhost:8084/swagger-ui.html${NC}"
echo -e "${CYAN}  Localization API    : http://localhost:8085${NC}"
echo -e "${CYAN}  Movement API        : http://localhost:8086${NC}"
echo -e "${CYAN}  Recommendation Swagger : http://localhost:8087/swagger-ui.html${NC}"
echo -e "${CYAN}  Souvenir Swagger    : http://localhost:8088/swagger-ui.html${NC}"
echo
echo -e "${YELLOW}Pour tout arreter : bash stop-all.sh${NC}"
echo -e "${YELLOW}PIDs sauvegardes  : $PIDFILE${NC}"
