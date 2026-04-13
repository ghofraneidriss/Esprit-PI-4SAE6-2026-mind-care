# Methode de realisation - Moderation + Rating par cours

Ce document explique la realisation backend et frontend de:
1. Metier avance 1: moderation automatique feedback
2. Metier avance 2: systeme de rating, moyenne et classement par cours
3. Metier avance 2: communication microservice feedback -> reclamation
4. Metier avance 3: classification automatique des reclamations (IA locale)

## 1) Metier avance 1 - Moderation automatique feedback

### 1.1 Objectif

A la creation ou modification d un feedback:
- calcul d un score de risque
- blocage automatique si contenu suspect
- approbation automatique sinon

### 1.2 Regles du mini modele local

Le modele ne depend d aucune API externe.
Il utilise:
- mots bloques configurables
- detection d URL/spam
- ponctuation repetitive
- ratio de majuscules
- longueur de commentaire

Sortie:
- moderationScore
- moderationFlagged
- blockedWords
- recommendation

### 1.3 Decision automatique

- APPROVED si risque faible
- REJECTED si risque eleve

## 2) Metier avance 2 - Rating & moyenne par cours

## 2.1 Feedback

Chaque feedback contient:
- title = titre du cours
- rating = note (1..5)

Le backend calcule automatiquement:
- moyenne globale des notes
- nombre total de feedbacks
- nombre de feedbacks notes
- moyenne par cours
- classement des cours

Endpoints:
- GET /feedbacks/ratings/summary
- GET /feedbacks/ratings/by-course
- GET /feedbacks/ratings/ranking?top=5

## 2.2 Reclamation

Chaque reclamation contient:
- subject = titre du cours
- rating = note (1..5)

Le backend calcule automatiquement:
- moyenne globale des notes reclamation
- nombre total de reclamations
- nombre de reclamations notees
- moyenne par cours
- classement des cours

Endpoints:
- GET /reclamations/ratings/summary
- GET /reclamations/ratings/by-course
- GET /reclamations/ratings/ranking?top=5

## 3) Affichage frontend

Le frontend affiche maintenant:
- moyenne globale
- nombre de cours classes
- top 5 des cours

Ecrans concernes:
- feedback-management
- reclamation-management

## 4) Communication microservice feedback -> reclamation

### 4.1 Regle metier

Si un feedback est negatif (rating <= seuil configure), le backend feedback cree automatiquement une reclamation.

### 4.2 Mapping demande

- title du feedback forum -> subject de la reclamation
- comment du feedback -> description de la reclamation (avec contexte auto)
- status reclamation force a OPEN

### 4.3 Traçabilite backend

Le feedback stocke:
- autoReclamationCreated
- linkedReclamationId
- autoReclamationStatus

Cela permet de verifier facilement en backend/Postman que la communication inter-services a bien fonctionne.

## 5) Classification automatique des reclamations (IA locale)

### 5.1 Regle metier

Chaque reclamation est classee automatiquement lors du create/update:
- FAIBLE
- MOYEN
- FORT

Exemples cibles:
- "GPS ne marche pas" -> FAIBLE
- "retard intervention" -> FORT

### 5.2 Sortie backend stockee

La reclamation stocke:
- classificationLevel
- classificationScore
- classificationKeywords
- classificationReason
- classifiedBy
- classifiedAt

### 5.3 Endpoints de test backend

- POST /reclamations/classification/analyze
- GET /reclamations/classification/{level}

## 6) Fichiers backend modifies

Feedback:
- backend/feedback-ms/src/main/java/tn/esprit/microservice/feedbackms/controller/FeedbackController.java
- backend/feedback-ms/src/main/java/tn/esprit/microservice/feedbackms/service/FeedbackService.java
- backend/feedback-ms/src/main/java/tn/esprit/microservice/feedbackms/entity/Feedback.java
- backend/feedback-ms/src/main/java/tn/esprit/microservice/feedbackms/client/ReclamationClient.java
- backend/feedback-ms/src/main/java/tn/esprit/microservice/feedbackms/FeedbackMsApplication.java
- backend/feedback-ms/src/main/resources/application.properties

Reclamation:
- backend/reclamation-ms/src/main/java/tn/esprit/microservice/reclamationms/controller/ReclamationController.java
- backend/reclamation-ms/src/main/java/tn/esprit/microservice/reclamationms/service/ReclamationService.java
- backend/reclamation-ms/src/main/java/tn/esprit/microservice/reclamationms/entity/Reclamation.java
- backend/reclamation-ms/src/main/java/tn/esprit/microservice/reclamationms/entity/ReclamationClassificationLevel.java
- backend/config-server/src/main/resources/config/reclamation-ms.properties

## 7) Fichiers frontend modifies

- frontend/e-learn/src/app/services/feedback.service.ts
- frontend/e-learn/src/app/services/reclamation.service.ts
- frontend/e-learn/src/app/models/feedback.model.ts
- frontend/e-learn/src/app/models/reclamation.model.ts
- frontend/e-learn/src/app/components/feedback-management/feedback-management.component.ts
- frontend/e-learn/src/app/components/feedback-management/feedback-management.component.html
- frontend/e-learn/src/app/components/feedback-management/feedback-management.component.css
- frontend/e-learn/src/app/components/reclamation-management/reclamation-management.component.ts
- frontend/e-learn/src/app/components/reclamation-management/reclamation-management.component.html
- frontend/e-learn/src/app/components/reclamation-management/reclamation-management.component.css

## 8) Test backend pas a pas (Postman)

Collection:
- e-learn-main/postman/e-learn-new-apis.postman_collection.json

Guide complet de test:
- e-learn-main/postman/GUIDE_POSTMAN_NOUVELLES_APIS.md

Scenario complet:
1. POST Create Feedback Negative (Auto Reclamation)
2. GET Feedback By Id (verifier linkedReclamationId)
3. GET Auto Reclamation By Linked Id (verifier subject == feedbackTitle)
4. POST Create Reclamation Low Classification (GPS)
5. POST Create Reclamation High Classification (Retard Intervention)
6. POST Reclamation Classification Analyze
7. GET Reclamations By Classification FORT
8. GET Feedback Ratings Summary
9. GET Feedback Ratings By Course
10. GET Feedback Ratings Ranking
11. GET Reclamation Ratings Summary
12. GET Reclamation Ratings By Course
13. GET Reclamation Ratings Ranking

## 9) Configuration

- feedback.moderation.blocked-words
- feedback.moderation.auto-block-threshold
- feedback.auto-reclamation.enabled
- feedback.auto-reclamation.negative-threshold
- reclamation.classification.high-keywords
- reclamation.classification.medium-keywords
- reclamation.classification.low-keywords
- reclamation.classification.high-threshold
- reclamation.classification.medium-threshold

Fichiers:
- backend/feedback-ms/src/main/resources/application.properties
- backend/config-server/src/main/resources/config/feedback-ms.properties
- backend/reclamation-ms/src/main/resources/application.properties
- backend/config-server/src/main/resources/config/reclamation-ms.properties
