#!/bin/bash

# Startup script for MindCare Lost Item Service Stack
# Usage: ./docker/startup.sh [start|stop|restart|status|logs]

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
COMPOSE_FILE="docker-compose.yml"
PROJECT_NAME="mindcare"
SERVICES=("mysql" "eureka-server" "lost-item-service")

# Check if docker-compose.yml exists
if [ ! -f "$COMPOSE_FILE" ]; then
    echo -e "${RED}Error: $COMPOSE_FILE not found${NC}"
    exit 1
fi

function print_header() {
    echo -e "\n${BLUE}════════════════════════════════════════${NC}"
    echo -e "${BLUE}MindCare Lost Item Service Stack${NC}"
    echo -e "${BLUE}════════════════════════════════════════${NC}\n"
}

function print_separator() {
    echo -e "\n${YELLOW}────────────────────────────────────────${NC}\n"
}

function start_services() {
    print_header
    echo -e "${YELLOW}[1/4] Pulling images...${NC}"
    docker-compose pull 2>/dev/null || echo -e "${YELLOW}Some images not available in registry, building locally...${NC}"

    print_separator
    echo -e "${YELLOW}[2/4] Building services...${NC}"
    docker-compose build

    print_separator
    echo -e "${YELLOW}[3/4] Starting services...${NC}"
    docker-compose up -d

    print_separator
    echo -e "${YELLOW}[4/4] Waiting for services to be healthy...${NC}"

    # Wait for MySQL
    echo -e "${YELLOW}Waiting for MySQL...${NC}"
    for i in {1..30}; do
        if docker-compose exec -T mysql mysqladmin ping -u root 2>/dev/null | grep -q "mysqld is alive"; then
            echo -e "${GREEN}✓ MySQL is healthy${NC}"
            break
        fi
        echo "  Attempt $i/30..."
        sleep 2
    done

    # Wait for Eureka
    echo -e "${YELLOW}Waiting for Eureka...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:8761 | grep -q "Eureka" 2>/dev/null; then
            echo -e "${GREEN}✓ Eureka is healthy${NC}"
            break
        fi
        echo "  Attempt $i/30..."
        sleep 2
    done

    # Wait for Lost Item Service
    echo -e "${YELLOW}Waiting for Lost Item Service...${NC}"
    for i in {1..30}; do
        if curl -s http://localhost:8082/actuator/health | grep -q "UP" 2>/dev/null; then
            echo -e "${GREEN}✓ Lost Item Service is healthy${NC}"
            break
        fi
        echo "  Attempt $i/30..."
        sleep 2
    done

    print_separator
    echo -e "${GREEN}All services started successfully!${NC}\n"
    print_service_info
}

function stop_services() {
    print_header
    echo -e "${YELLOW}Stopping services...${NC}"
    docker-compose down
    echo -e "${GREEN}Services stopped${NC}"
}

function restart_services() {
    stop_services
    sleep 2
    start_services
}

function print_status() {
    print_header
    echo -e "${YELLOW}Service Status:${NC}\n"
    docker-compose ps

    print_separator
    echo -e "${YELLOW}Network Status:${NC}\n"
    docker network inspect mindcare-network 2>/dev/null | \
        grep -A 20 '"Containers"' | \
        grep -E '"Name"|"IPv4Address"' || echo "Network not found"

    print_separator
    echo -e "${YELLOW}Volume Status:${NC}\n"
    docker volume ls | grep mindcare || echo "No volumes found"
}

function print_logs() {
    SERVICE=${1:-lost-item-service}
    print_header
    echo -e "${YELLOW}Logs for $SERVICE (last 100 lines):${NC}\n"
    docker-compose logs --tail=100 "$SERVICE"
}

function print_service_info() {
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}Service Information${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}\n"

    echo -e "${BLUE}MySQL Database:${NC}"
    echo -e "  URL: jdbc:mysql://localhost:3306/lost_item_db"
    echo -e "  Username: root"
    echo -e "  Password: (empty)"

    echo -e "\n${BLUE}Eureka Service Registry:${NC}"
    echo -e "  URL: http://localhost:8761"

    echo -e "\n${BLUE}Lost Item Service:${NC}"
    echo -e "  URL: http://localhost:8082"
    echo -e "  Health Check: http://localhost:8082/actuator/health"
    echo -e "  API Docs: http://localhost:8082/swagger-ui.html"

    echo -e "\n${BLUE}Useful Commands:${NC}"
    echo -e "  View logs: docker-compose logs -f lost-item-service"
    echo -e "  Stop services: docker-compose down"
    echo -e "  Restart services: docker-compose restart"
    echo -e "  Database access: docker-compose exec mysql mysql -u root"

    echo -e "\n${GREEN}════════════════════════════════════════${NC}\n"
}

# Main logic
case "${1:-start}" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    status)
        print_status
        ;;
    logs)
        print_logs "${2:-lost-item-service}"
        ;;
    *)
        echo -e "Usage: $0 {start|stop|restart|status|logs [service]}"
        echo -e ""
        echo -e "Examples:"
        echo -e "  $0 start              # Start all services"
        echo -e "  $0 stop               # Stop all services"
        echo -e "  $0 restart            # Restart all services"
        echo -e "  $0 status             # Show service status"
        echo -e "  $0 logs               # Show lost-item-service logs"
        echo -e "  $0 logs mysql         # Show MySQL logs"
        exit 1
        ;;
esac
