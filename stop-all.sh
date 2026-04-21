#!/bin/bash

# ============================================
#   MindCare - Arret de tous les services
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

ROOT="$(cd "$(dirname "$0")" && pwd)"
PIDFILE="$ROOT/logs/pids.txt"

echo -e "${YELLOW}============================================${NC}"
echo -e "${YELLOW}   MindCare - Arret de tous les services    ${NC}"
echo -e "${YELLOW}============================================${NC}"
echo

# Tuer les processus via le fichier PIDs
if [ -f "$PIDFILE" ]; then
    while IFS='=' read -r NAME PID; do
        if kill -0 "$PID" 2>/dev/null; then
            echo -e "${RED}>>> Arret $NAME (PID $PID)...${NC}"
            kill "$PID" 2>/dev/null
        else
            echo -e "    $NAME (PID $PID) - deja arrete"
        fi
    done < "$PIDFILE"
    rm -f "$PIDFILE"
else
    echo -e "${YELLOW}Aucun fichier PIDs trouve. Arret par port...${NC}"
    # Fallback : tuer par port
    for PORT in 8761 8081 8082 8083 8080 4200; do
        PID=$(lsof -ti tcp:$PORT 2>/dev/null)
        if [ -n "$PID" ]; then
            echo -e "${RED}>>> Arret port $PORT (PID $PID)${NC}"
            kill "$PID" 2>/dev/null
        fi
    done
fi

echo
echo -e "${YELLOW}Arret MySQL Docker...${NC}"
docker compose -f "$ROOT/docker-compose.yml" down

echo
echo -e "${GREEN}Tous les services sont arretes.${NC}"
