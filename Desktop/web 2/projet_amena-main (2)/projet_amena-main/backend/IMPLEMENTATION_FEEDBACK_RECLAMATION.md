# Implementation - Gestion Feedback et Reclamation

## Objectif
Ajouter deux gestions completes (CRUD) dans une architecture microservices et les lier au frontend Angular via la Gateway.

## Backend - Microservices ajoutes

1. feedback-ms
- Port: 8090
- Service name: feedback-ms
- Dossier: feedback-ms/

2. reclamation-ms
- Port: 8091
- Service name: reclamation-ms
- Dossier: reclamation-ms/

## Gateway
Routes ajoutees dans config serveur:
- /feedbacks/** -> lb://feedback-ms
- /reclamations/** -> lb://reclamation-ms

Fichier:
- config-server/src/main/resources/config/gateway.properties

## Modules Maven
Les modules ont ete ajoutes dans le parent backend/pom.xml:
- feedback-ms
- reclamation-ms

## CRUD Feedback
Base path: /feedbacks

- GET /feedbacks
- GET /feedbacks/{id}
- POST /feedbacks
- PUT /feedbacks/{id}
- DELETE /feedbacks/{id}

Composants backend:
- entity/Feedback.java
- repository/FeedbackRepository.java
- service/FeedbackService.java
- controller/FeedbackController.java

## CRUD Reclamation
Base path: /reclamations

- GET /reclamations
- GET /reclamations/{id}
- POST /reclamations
- PUT /reclamations/{id}
- DELETE /reclamations/{id}

Composants backend:
- entity/Reclamation.java
- entity/ReclamationStatus.java
- repository/ReclamationRepository.java
- service/ReclamationService.java
- controller/ReclamationController.java

## Frontend Angular - Liaison API
Le frontend consomme la Gateway sur:
- http://localhost:8085

Fichier config API:
- frontend/e-learn/src/app/services/api.config.ts

Services Angular:
- feedback.service.ts
- reclamation.service.ts

Pages Angular:
- /feedbacks
- /reclamations

Composants:
- components/feedback-management/*
- components/reclamation-management/*

## Fichiers Angular modifies
- app-module.ts: HttpClientModule + FormsModule + declarations des composants
- app-routing-module.ts: routes feedbacks/reclamations
- app.html: shell + navigation
- app.css et styles.css: styles interface CRUD

## Verification
- Build backend OK (mvn clean package)
- Build frontend OK (npm run build)

## Ordre de demarrage recommande
1. eureka-server
2. config-server
3. feedback-ms
4. reclamation-ms
5. gateway
6. frontend Angular

## Test rapide via Gateway
- GET http://localhost:8085/feedbacks
- GET http://localhost:8085/reclamations
