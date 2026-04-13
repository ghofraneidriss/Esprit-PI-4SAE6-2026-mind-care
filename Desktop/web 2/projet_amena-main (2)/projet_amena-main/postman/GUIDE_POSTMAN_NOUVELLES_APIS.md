# Guide Postman Step by Step - Nouvelles APIs Backend

Ce guide permet de tester directement le backend:
- moderation automatique feedback
- systeme de rating par cours (feedback + reclamation)
- classification automatique des reclamations (IA locale)
- communication microservice (feedback negatif => reclamation auto)
- lecture, dictee, QR code, PDF

Guide rapide cible (stats + classifications + auto reclamation):
- e-learn-main/postman/GUIDE_POSTMAN_TEST_STATS_CLASSIFICATION.md

## 1) Prerequis

1. Java et Maven installes.
2. Postman installe.
3. Services demarres:
   1. eureka-server (8761)
   2. config-server (8888)
   3. feedback-ms (8090)
   4. reclamation-ms (8091)
   5. gateway (8085)

## 2) Import Postman

1. Ouvre Postman.
2. Clique Import.
3. Importe le fichier:
   - e-learn-main/postman/e-learn-new-apis.postman_collection.json
4. Verifie la variable:
   - baseUrl = http://localhost:8085

Variables utiles:
- baseUrl
- feedbackId
- reclamationId
- autoReclamationId
- feedbackTitle

## 3) Metier avance 1 - Moderation auto feedback

Dossier: Feedbacks - Moderation Automatique + Rating

### Etape 1 - Feedback normal

1. Lance: POST Create Feedback Normal (set feedbackId)
2. Verifie:
   - status = 200
   - moderationStatus = APPROVED
   - moderationScore present

### Etape 2 - Feedback risque

1. Lance: POST Create Feedback Risky (set feedbackId)
2. Verifie:
   - status = 200
   - moderationStatus = REJECTED
   - moderationFlagged = true

### Etape 3 - Verification detail

1. Lance: GET Feedback By Id
2. Verifie:
   - moderationStatus
   - moderationScore
   - blockedWords

### Etape 4 - Analyse seule

1. Lance: POST Feedback Moderation Analyze
2. Verifie:
   - score
   - flagged
   - blockedWords
   - recommendation

## 4) Metier avance 2 - Rating par cours feedback

### Etape 1 - Resume global

1. Lance: GET Feedback Ratings Summary
2. Verifie:
   - averageRating
   - feedbackCount
   - ratedFeedbackCount
   - rankedCourseCount

### Etape 2 - Moyenne par cours

1. Lance: GET Feedback Ratings By Course
2. Verifie pour chaque ligne:
   - courseTitle
   - averageRating
   - feedbackCount

### Etape 3 - Classement des cours

1. Lance: GET Feedback Ratings Ranking (Top 5)
2. Verifie:
   - tri du meilleur rating vers le plus faible

### Etape 4 - Listes de moderation

1. Lance: GET Approved Feedbacks
2. Lance: GET Blocked Feedbacks

## 5) Metier avance 2 - Communication microservice (feedback negatif)

Objectif: verifier que le backend cree automatiquement une reclamation si le feedback est negatif.

### Etape 1 - Creer un feedback negatif

1. Lance: POST Create Feedback Negative (Auto Reclamation)
2. Verifie dans la reponse:
   - autoReclamationCreated = true
   - linkedReclamationId present
   - autoReclamationStatus = CREATED

### Etape 2 - Verifier le feedback lie

1. Lance: GET Feedback By Id
2. Verifie:
   - linkedReclamationId present
   - autoReclamationStatus present

### Etape 3 - Verifier la reclamation creee automatiquement

1. Lance: GET Auto Reclamation By Linked Id
2. Verifie:
   - subject == feedbackTitle (mapping title forum -> subject reclamation)
   - status == OPEN

### Etape 4 - Verification directe reclamation

1. Lance: GET Reclamation By Id (avec reclamationId)
2. Verifie l objet retourne.

## 6) Classification automatique des reclamations (IA locale)

Objectif: classer automatiquement les reclamations en FAIBLE/MOYEN/FORT selon le texte.

### Etape 1 - Cas faible (GPS)

1. Lance: POST Create Reclamation Low Classification (GPS)
2. Verifie:
   - classificationLevel = FAIBLE
   - classificationScore present

### Etape 2 - Cas fort (retard intervention)

1. Lance: POST Create Reclamation High Classification (Retard Intervention)
2. Verifie:
   - classificationLevel = FORT
   - classificationScore >= 70

### Etape 3 - Analyse seule sans creation

1. Lance: POST Reclamation Classification Analyze
2. Verifie:
   - level
   - score
   - matchedKeywords
   - reason

### Etape 4 - Filtrer les reclamations classees FORT

1. Lance: GET Reclamations By Classification FORT
2. Verifie la liste retournee.

## 7) Metier avance 2 - Rating par cours reclamation

Dossier: Reclamations - Rating + Nouvelles APIs

### Etape 1 - Creer reclamation avec note

1. Lance: POST Create Reclamation (set reclamationId)
2. Verifie:
   - status = 200
   - rating present

### Etape 2 - Resume global reclamation

1. Lance: GET Reclamation Ratings Summary
2. Verifie:
   - averageRating
   - reclamationCount
   - ratedReclamationCount
   - rankedCourseCount

### Etape 3 - Moyenne par cours reclamation

1. Lance: GET Reclamation Ratings By Course
2. Verifie:
   - courseTitle
   - averageRating
   - reclamationCount

### Etape 4 - Classement cours reclamation

1. Lance: GET Reclamation Ratings Ranking (Top 5)
2. Verifie le tri.

## 8) APIs annexes feedback/reclamation

Feedback:
1. GET Feedback Lecture Text
2. POST Feedback Dictation
3. GET Feedback QR Code (PNG)
4. GET Feedback PDF

Reclamation:
1. GET Reclamation Lecture Text
2. POST Reclamation Dictation
3. GET Reclamation QR Code (PNG)
4. GET Reclamation PDF

## 9) Endpoints backend utilises

Feedbacks:
- GET /feedbacks
- GET /feedbacks/{id}
- POST /feedbacks
- PUT /feedbacks/{id}
- DELETE /feedbacks/{id}
- GET /feedbacks/approved
- GET /feedbacks/blocked
- GET /feedbacks/ratings/summary
- GET /feedbacks/ratings/by-course
- GET /feedbacks/ratings/ranking?top=5
- POST /feedbacks/moderation/analyze
- GET /feedbacks/{id}/lecture
- POST /feedbacks/dictation
- GET /feedbacks/{id}/qrcode
- GET /feedbacks/{id}/pdf

Reclamations:
- GET /reclamations
- GET /reclamations/{id}
- POST /reclamations
- PUT /reclamations/{id}
- DELETE /reclamations/{id}
- POST /reclamations/classification/analyze
- GET /reclamations/classification/{level}
- GET /reclamations/ratings/summary
- GET /reclamations/ratings/by-course
- GET /reclamations/ratings/ranking?top=5
- GET /reclamations/{id}/lecture
- POST /reclamations/dictation
- GET /reclamations/{id}/qrcode
- GET /reclamations/{id}/pdf

## 10) Config utiles

feedback:
- feedback.moderation.blocked-words
- feedback.moderation.auto-block-threshold
- feedback.auto-reclamation.enabled
- feedback.auto-reclamation.negative-threshold

reclamation:
- reclamation.classification.high-keywords
- reclamation.classification.medium-keywords
- reclamation.classification.low-keywords
- reclamation.classification.high-threshold
- reclamation.classification.medium-threshold

## 11) Erreurs frequentes

1. 404 Not Found
- verifier gateway et baseUrl

2. Variables vides
- relancer POST create feedback/reclamation pour remplir feedbackId/reclamationId

3. 500 Internal Server Error
- verifier logs feedback-ms et reclamation-ms
