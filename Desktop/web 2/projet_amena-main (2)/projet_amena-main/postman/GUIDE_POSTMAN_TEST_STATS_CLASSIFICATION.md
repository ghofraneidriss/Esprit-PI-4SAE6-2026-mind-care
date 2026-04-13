# Guide Postman - Test Pas a Pas (Stats + Classification + Auto Reclamation)

Ce document couvre exactement:
- statistiques feedback + reclamation
- classification automatique forum (moderation IA)
- classification automatique reclamation (FAIBLE/MOYEN/FORT)
- creation automatique de reclamation si feedback faible

## 1) Prerequis

1. Demarrer les services:
- eureka-server (8761)
- config-server (8888)
- feedback-ms (8090)
- reclamation-ms (8091)
- gateway (8085)

2. Importer la collection Postman:
- postman/e-learn-new-apis.postman_collection.json

3. Verifier variable:
- baseUrl = http://localhost:8085

## 2) Test classification automatique forum (moderation IA)

### Etape 1 - Feedback normal

Requete:
- POST Create Feedback Normal (set feedbackId)

Verifier dans la reponse:
- moderationStatus = APPROVED
- moderationScore present

### Etape 2 - Feedback avec violence/haine

Requete:
- POST Create Feedback Risky (set feedbackId)

Verifier dans la reponse:
- moderationStatus = REJECTED
- moderationFlagged = true
- moderationScore >= 70
- blockedWords contient "haine" (ou mot bloque equivalent)

### Etape 3 - Verification detail

Requete:
- GET Feedback By Id

Verifier:
- moderationStatus
- moderationScore
- blockedWords
- moderationNote

## 3) Test creation auto reclamation si feedback faible

Requete:
- POST Create Feedback Negative (Auto Reclamation)

Verifier dans la reponse:
- autoReclamationCreated = true
- linkedReclamationId present
- autoReclamationStatus = CREATED

Puis lancer:
- GET Auto Reclamation By Linked Id

Verifier:
- subject == feedbackTitle
- status == OPEN

## 4) Test classification automatique reclamation

### Etape 1 - Cas faible

Requete:
- POST Create Reclamation Low Classification (GPS)

Verifier:
- classificationLevel = FAIBLE
- classificationScore present

### Etape 2 - Cas fort

Requete:
- POST Create Reclamation High Classification (Retard Intervention)

Verifier:
- classificationLevel = FORT
- classificationScore >= 70

### Etape 3 - Analyse sans creation

Requete:
- POST Reclamation Classification Analyze

Verifier:
- level
- score
- matchedKeywords
- reason

### Etape 4 - Filtrer les FORT

Requete:
- GET Reclamations By Classification FORT

Verifier:
- la liste contient des reclamations avec classificationLevel = FORT

## 5) Test statistiques

### Feedback

1. GET Feedback Ratings Summary
- verifier averageRating, feedbackCount, ratedFeedbackCount, rankedCourseCount

2. GET Feedback Ratings By Course
- verifier courseTitle, averageRating, feedbackCount

3. GET Feedback Ratings Ranking (Top 5)
- verifier tri du meilleur vers le plus faible

### Reclamation

1. GET Reclamation Ratings Summary
- verifier averageRating, reclamationCount, ratedReclamationCount, rankedCourseCount

2. GET Reclamation Ratings By Course
- verifier courseTitle, averageRating, reclamationCount

3. GET Reclamation Ratings Ranking (Top 5)
- verifier tri du meilleur vers le plus faible

## 6) En cas de probleme

1. 404:
- verifier gateway et baseUrl

2. 500:
- verifier logs feedback-ms et reclamation-ms

3. variables non remplies:
- relancer les requetes POST de creation pour remplir feedbackId/reclamationId/autoReclamationId
