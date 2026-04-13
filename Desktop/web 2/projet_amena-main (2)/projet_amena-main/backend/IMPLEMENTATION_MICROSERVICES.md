# Backend Microservices Implementation Report

## Goal
Transform the existing single backend into a Spring Cloud microservices architecture aligned with your `brief_microservices.md`.

## What Was Implemented

The backend is now a **multi-module Maven project** with 5 services:

1. `eureka-server` (port `8761`)
2. `config-server` (port `8888`)
3. `gateway` (port `8085`)
4. `cours-ms` (port `8060`)
5. `inscription-ms` (port `8081`)

This keeps your e-learning domain (`Cours`) and adds a second business service (`Inscription`) to demonstrate inter-service communication with OpenFeign.

## New Backend Structure

```text
backend/
├── pom.xml                         (parent aggregator)
├── eureka-server/
├── config-server/
│   └── src/main/resources/config/
│       ├── application.properties
│       ├── cours-ms.properties
│       ├── inscription-ms.properties
│       └── gateway.properties
├── gateway/
├── cours-ms/
└── inscription-ms/
```

## Service Details

### 1) Eureka Server
- Main class with `@EnableEurekaServer`
- Registers service instances for discovery
- Config in `eureka-server/src/main/resources/application.properties`

### 2) Config Server
- Main class with `@EnableConfigServer` and `@EnableDiscoveryClient`
- Uses native mode and serves central properties from `classpath:/config`
- Centralized files:
  - `application.properties` (shared defaults)
  - `cours-ms.properties`
  - `inscription-ms.properties`
  - `gateway.properties`

### 3) API Gateway
- Spring Cloud Gateway + Eureka + Config Client
- Dynamic routing with discovery (`lb://...`) configured centrally:
  - `/cours/**` -> `cours-ms`
  - `/inscriptions/**` -> `inscription-ms`
- CORS configured for Angular (`http://localhost:4200`)

### 4) cours-ms
- Contains your migrated course domain model
- Added complete REST CRUD:
  - `GET /cours`
  - `GET /cours/{id}`
  - `POST /cours`
  - `PUT /cours/{id}`
  - `DELETE /cours/{id}`
  - `GET /cours/{id}/summary` (used by Feign)
- Added:
  - Entity + enums
  - Repository
  - Service layer
  - Controller
  - Not-found exception handling
  - Data initializer with demo data

### 5) inscription-ms
- New service for enrollments
- Added CRUD + detail endpoint:
  - `GET /inscriptions`
  - `GET /inscriptions/{id}`
  - `POST /inscriptions`
  - `PUT /inscriptions/{id}`
  - `DELETE /inscriptions/{id}`
  - `GET /inscriptions/{id}/details`
- Uses OpenFeign client (`@FeignClient(name = "cours-ms")`) to call:
  - `GET /cours/{id}/summary`
- Added:
  - Entity
  - Repository
  - Service layer
  - Controller
  - DTOs
  - Not-found exception handling
  - Demo initializer

## Inter-Service Communication (OpenFeign)

Flow implemented:

1. Client calls `inscription-ms` endpoint `/inscriptions/{id}/details`
2. `inscription-ms` reads inscription from its own DB
3. `inscription-ms` calls `cours-ms` via Feign + Eureka discovery
4. Response merges inscription + course summary

This matches the communication principle from your brief.

## Build Validation

A full backend build was executed from `backend/`:

```bash
./mvnw -DskipTests clean package
```

Result: all modules packaged successfully (`.jar` files generated in each module `target/`).

## Startup Order (Important)

Start services in this order:

1. `eureka-server`
2. `config-server`
3. `cours-ms`
4. `inscription-ms`
5. `gateway`

Frontend can then call only the Gateway (`http://localhost:8085`).

## Run Commands (from `backend/`)

```bash
./mvnw -pl eureka-server spring-boot:run
./mvnw -pl config-server spring-boot:run
./mvnw -pl cours-ms spring-boot:run
./mvnw -pl inscription-ms spring-boot:run
./mvnw -pl gateway spring-boot:run
```

## Quick API Tests Through Gateway

```bash
GET  http://localhost:8085/cours
GET  http://localhost:8085/cours/1
GET  http://localhost:8085/inscriptions
GET  http://localhost:8085/inscriptions/1/details
```

## Notes

- This implementation aligns with the brief architecture (Eureka + Config + Gateway + Feign + separated databases).
- Keycloak/OAuth2 security is not enforced yet in code, but the structure is ready to add it next (Gateway and microservices can be turned into Resource Servers).
- Legacy code under `backend/src` was left unchanged to avoid destructive deletion; it is no longer the active backend runtime path.
