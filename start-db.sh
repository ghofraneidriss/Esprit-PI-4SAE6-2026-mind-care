#!/bin/bash

# ============================================
#   MindCare - Démarrage base de données MySQL
#   Lance le conteneur Docker MySQL uniquement
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

ROOT="$(cd "$(dirname "$0")" && pwd)"
LOGS="$ROOT/logs"

mkdir -p "$LOGS"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   MindCare - Démarrage MySQL (Docker)      ${NC}"
echo -e "${GREEN}============================================${NC}"
echo

if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERREUR] Docker non installé. Lancez : sudo apt install docker.io${NC}"
    exit 1
fi

if ! docker info &> /dev/null; then
    echo -e "${RED}[ERREUR] Docker ne tourne pas. Lancez : sudo service docker start${NC}"
    exit 1
fi

docker compose -f "$ROOT/docker-compose.yml" up -d >> "$LOGS/mysql-docker.log" 2>&1

echo -e "${YELLOW}    Attente MySQL (5s)...${NC}"
sleep 5

echo -e "${GREEN}    [OK] MySQL prêt sur le port 3306${NC}"
echo -e "${CYAN}    Log : $LOGS/mysql-docker.log${NC}"
echo
echo -e "${YELLOW}Lancez ensuite : bash start-all.sh${NC}"
