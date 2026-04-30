# Docker Setup for MindCare - Lost Item Service

Complete Docker and container orchestration setup for the Lost Item Microservice.

## Overview

This document describes the Docker containerization and deployment strategy for the Lost Item Service in the MindCare platform.

### Architecture

```
┌─────────────────────────────────────────────────────┐
│         Docker Compose Network (mindcare-network)   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  │
│  │   MySQL DB   │  │  Eureka      │  │ Lost     │  │
│  │   :3306      │  │  :8761       │  │ Item     │  │
│  │              │  │              │  │ :8082    │  │
│  └──────────────┘  └──────────────┘  └──────────┘  │
│       ↑                   ↑                  ↑       │
│       └───────────────────┴──────────────────┘       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Build and Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f lost-item-service
```

### 2. Build Docker Image Manually

```bash
# Navigate to lost-item-service
cd server/lost-item-service

# Build image
docker build -t ghofraneidriss/lost-item-service:latest .

# Run container
docker run -d \
  -p 8082:8080 \
  --name lost-item-service \
  --network mindcare-network \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/lost_item_db \
  ghofraneidriss/lost-item-service:latest
```

### 3. Using the Build Script

```bash
# Make script executable
chmod +x docker/build-and-push.sh

# Build and push to Docker Hub
./docker/build-and-push.sh lost-item-service ghofraneidriss latest

# Build locally only
./docker/build-and-push.sh lost-item-service local
```

## Services Configuration

### Lost Item Service

**Port**: 8082  
**Internal Port**: 8080  
**Health Check**: `http://localhost:8082/actuator/health`  
**Logs**: `docker-compose logs lost-item-service`

#### Environment Variables

```yaml
SPRING_APPLICATION_NAME: lost-item-service
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/lost_item_db
SPRING_DATASOURCE_USERNAME: root
SPRING_DATASOURCE_PASSWORD: (empty)
SPRING_JPA_HIBERNATE_DDL_AUTO: update
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
```

### MySQL Database

**Port**: 3306  
**Container**: alzcare-mysql  
**Volume**: mysql_data (persistent)  
**Init Script**: `docker/init.sql`

#### Credentials

- **Username**: root
- **Password**: (empty - dev environment)
- **Databases**: 
  - lost_item_db
  - eureka_db
  - config_db

### Eureka Service Registry

**Port**: 8761  
**URL**: `http://localhost:8761`  
**Container**: alzcare-eureka

## Common Tasks

### View Service Logs

```bash
# Lost Item Service logs
docker-compose logs lost-item-service

# Follow logs in real-time
docker-compose logs -f lost-item-service

# Last 100 lines
docker-compose logs --tail=100 lost-item-service
```

### Stop Services

```bash
# Stop specific service
docker-compose stop lost-item-service

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Restart Services

```bash
# Restart specific service
docker-compose restart lost-item-service

# Restart all services
docker-compose restart
```

### Check Service Health

```bash
# View container status
docker-compose ps

# Check health status
docker ps --filter "name=lost-item-service" --format "table {{.Names}}\t{{.Status}}"

# Manual health check
curl http://localhost:8082/actuator/health
```

### Connect to Database

```bash
# Connect to MySQL
docker-compose exec mysql mysql -u root

# Query specific database
docker-compose exec mysql mysql -u root lost_item_db

# Execute SQL file
docker-compose exec mysql mysql -u root < docker/init.sql
```

### Clean Up

```bash
# Remove containers
docker-compose down

# Remove images
docker rmi ghofraneidriss/lost-item-service:latest

# Remove volumes
docker volume rm alzcare-mysql-data mindcare-network

# Complete cleanup (caution: removes all containers and images)
docker system prune -a
```

## Jenkins Pipeline Integration

The Jenkinsfile includes complete Docker automation:

1. **Build Stage**: Compiles Maven project
2. **Test Stage**: Runs unit tests
3. **SonarQube**: Code quality analysis with quality gate
4. **Docker Build**: Creates Docker image with metadata
5. **Docker Push**: Pushes to registry (only on jaaferFinal branch)
6. **Deploy**: Updates running container via docker-compose
7. **Health Check**: Validates service availability

### Pipeline Requirements

- Docker installed on Jenkins agent
- Docker credentials configured (`docker-hub-credentials`)
- Docker socket access or Docker daemon
- Maven and Java 17 tools configured

### Triggering the Pipeline

```bash
# Build will automatically trigger on push to jaaferFinal
git push origin jaaferFinal

# Push will only happen on jaaferFinal branch
# Deployment will follow if quality gate passes
```

## Docker Image Details

### Image Specifications

- **Base Image**: `eclipse-temurin:17.0.11_9-jre-alpine`
- **Size**: ~200MB (optimized with multi-stage build)
- **User**: appuser (non-root, UID: 1000)
- **Health Check**: Every 30s with 3-minute startup grace period

### Build Process

```dockerfile
# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
# Build application

# Stage 2: Runtime
FROM eclipse-temurin:17.0.11_9-jre-alpine
# Copy jar from builder
# Run as non-root user
```

### Image Metadata

Built images include labels:
- `build.number`: Jenkins build number
- `git.commit`: Git commit hash
- `jenkins.job`: Jenkins job name

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker-compose logs lost-item-service

# Check database connectivity
docker-compose exec lost-item-service \
  curl -v http://mysql:3306

# Rebuild image
docker-compose build --no-cache lost-item-service
docker-compose up -d lost-item-service
```

### Health Check Failing

```bash
# Manual health check
docker-compose exec lost-item-service \
  curl -v http://localhost:8080/actuator/health

# Check if port 8080 is accessible
docker-compose exec lost-item-service \
  netstat -tuln | grep 8080
```

### Database Connection Issues

```bash
# Verify MySQL is running
docker-compose logs mysql

# Check MySQL status
docker-compose exec mysql \
  mysqladmin -u root ping

# Test connection from service
docker-compose exec lost-item-service \
  nc -zv mysql 3306
```

### Out of Disk Space

```bash
# Clean up Docker resources
docker system prune -a --volumes

# Remove specific images
docker rmi $(docker images -q 'ghofraneidriss/lost-item-service')
```

## Performance Optimization

### Memory Limits

Set memory constraints in docker-compose.yml:

```yaml
services:
  lost-item-service:
    deploy:
      resources:
        limits:
          memory: 512M
        reservations:
          memory: 256M
```

### JVM Tuning

Adjust in docker-compose.yml:

```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
```

### Database Optimization

```sql
-- Add indexes for better query performance
CREATE INDEX idx_patient_id ON lost_item(patient_id);
CREATE INDEX idx_status ON lost_item(status);
CREATE INDEX idx_created_at ON lost_item(created_at);
```

## Security Considerations

- ✅ Non-root user execution (appuser)
- ✅ Alpine Linux for minimal attack surface
- ✅ Health checks for availability
- ✅ Environment variable secrets support
- ⚠️ Use Docker secrets for production credentials
- ⚠️ Enable TLS for inter-service communication
- ⚠️ Implement rate limiting and API authentication

### Production Security

```bash
# Use Docker secrets (Swarm/Kubernetes)
docker secret create db_password -

# Or use environment files
docker-compose --env-file .env.production up
```

## Scaling

### Horizontal Scaling

Run multiple instances:

```bash
# Scale service to 3 replicas
docker-compose up -d --scale lost-item-service=3

# Note: Requires load balancer (nginx, HAProxy, etc.)
```

### Load Balancing

Add to docker-compose.yml:

```yaml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    depends_on:
      - lost-item-service
    # Configuration for load balancing...
```

## References

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker](https://spring.io/guides/gs/spring-boot-docker/)
- [Alpine Linux](https://alpinelinux.org/)

## Support

For issues or questions:
1. Check logs: `docker-compose logs`
2. Review this documentation
3. Check SonarQube quality gate
4. Verify Jenkins pipeline logs

---

**Last Updated**: 2026-04-30  
**Maintained by**: MindCare Team
