# MindCare

MindCare is a microservices-based healthcare platform with an Angular frontend, API gateway, and Spring Boot services.

## Tech stack
- Frontend: Angular 21 (`front/`)
- Backend: Spring Boot 3, Spring Cloud Gateway, Eureka, Maven multi-module
- Database: MySQL 8 (Docker)

## Repository structure
- `front/`: Angular web app (backoffice + frontoffice)
- `api_gateway/`: API Gateway (root module)
- `medical_report_service/`: Medical report management and file integrations
- `users_service/`: User management/auth-related APIs
- `server/`: legacy/parallel microservice layout (includes `eureka_server`, `api_gateway`, `users_service`, `forums_service`, `incident_service`)
- `docker-compose.yml`: MySQL container
- `start-db.sh`, `start-all.sh`, `stop-all.sh`: helper scripts (WSL/Linux style)

## Prerequisites
- Java 17
- Maven 3.9+
- Node.js 20+ and npm 10+
- Docker Desktop (or Docker Engine)

## Quick start (recommended with scripts)
From project root:

```bash
bash start-db.sh
bash start-all.sh
```

Stop everything:

```bash
bash stop-all.sh
```

## Frontend only
```bash
cd front
npm install
npm start
```

## Backend modules (manual run)
You can run services directly with Maven (from each module folder):

```bash
mvn spring-boot:run
```

Important ports currently used in this repository:
- `4200`: Angular frontend
- `8761`: Eureka server (`server/eureka_server`)
- `8085`: Gateway (`server/api_gateway`)
- `8081`: Users service (`server/users_service`)
- `8086`: Forums service (`server/forums_service`)
- `8087`: Incident service (`server/incident_service`)
- `8083`: Medical report service (`medical_report_service`)

## API docs
When services are running:
- Users: `http://localhost:8081/swagger-ui.html`
- Forums: `http://localhost:8086/swagger-ui.html`
- Incident: `http://localhost:8087/swagger-ui.html`
- Medical reports: `http://localhost:8083/swagger-ui.html`

## Database
MySQL runs via Docker:
- Host: `localhost`
- Port: `3306`
- User: `root`
- Password: `root` (container default in `docker-compose.yml`)

Databases are initialized from:
- `docker/init.sql`

## Notes
- This repo currently contains both root-level and `server/` service layouts; scripts use the `server/` services.
- Keep secrets out of git (API keys, SMTP passwords, cloud credentials). Use environment variables or local untracked files.
