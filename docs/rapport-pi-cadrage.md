# Cadrage du rapport PI - MindCare

## Sources prises en compte

- Planification: `D:\Planification-PIDEV_2025-2026.xlsx`
- Rapport existant: `D:\rapport-pi.pdf`
- Code source du projet: `D:\PISAng - Copie\mind-care`

## 1. Lecture rapide de la planification

La planification PIDEV 2025-2026 est structurée autour de trois sprints principaux et d'un lancement DevOps.

Repere hebdomadaire releve dans la feuille `Planning detaille`:

- S1: 22 janvier 2026 - lancement Sprint 0
- S2: 29 janvier 2026 - coaching et suivi Sprint 0
- S3: 5 fevrier 2026 - evaluation groupe Sprint 0 et lancement Sprint 1
- S4: 12 fevrier 2026 - coaching Sprint 1
- S5: 19 fevrier 2026 - coaching Sprint 1 + lancement DevOps
- S6: 26 fevrier 2026 - coaching Sprint 1
- S7: 5 mars 2026 - evaluation individuelle Sprint 1 et lancement Sprint 2
- S8: 12 mars 2026 - coaching Sprint 2
- S9: 2 avril 2026 - coaching Sprint 2

Repere releve dans la feuille `Planing globale`:

- Sprint 0: analyse et specification des besoins
- Sprint 1: developpement Spring + Angular
- Sprint 2: developpement Spring + Angular
- Periode de vacances: du 16 mars 2026 au 29 mars 2026
- Jalons cites: conception, soutenance, integration templates, CRUD modules 1 et 2, fonctions avancees module 1, fonctions avancees module 2, sprint DevOps

## 2. Ce que couvre actuellement le PDF

Le PDF `rapport-pi.pdf` contient surtout:

- une introduction generale sur la maladie d'Alzheimer et l'objectif du projet
- un chapitre 1 sur les specifications du projet
- un chapitre 2 sur l'etude, les solutions existantes et les exigences
- une liste des modules fonctionnels cibles:
  - AI-Based MRI Risk Detection
  - Medical Report Management
  - Cognitive Activities Management
  - Progressive Cognitive Monitoring
  - Medical Consultation Management
  - Personalized Recommendation Management
  - Alzheimer Educational Events Management
  - Location and Movement Tracking Management
  - Medical Profile Management

En l'etat, le rapport ressemble donc a une base de `Sprint 0`, avec un peu de cadrage fonctionnel, mais pas encore a un rapport PI complet couvrant la realisation.

## 3. Ecarts constates entre la planification et le rapport

### 3.1 Couverture insuffisante par rapport aux sprints

La planification annonce au minimum:

- Sprint 0: analyse, besoins, conception, soutenance
- Sprint 1: integration templates, entites, associations, CRUD, fonctions avancees module 1
- Sprint 2: fonctions avancees module 2
- DevOps: demarrage a partir du 19 fevrier 2026

Le PDF ne documente quasiment que la partie Sprint 0.

Il manque donc au minimum:

- la conception technique et architecturale
- la presentation des microservices reelement implementes
- la realisation par sprint
- les captures ou descriptions des interfaces frontoffice/backoffice
- la partie tests, validation, limites et perspectives
- la partie DevOps/deploiement

### 3.2 Incoherences de forme

Plusieurs problemes sont visibles dans le PDF actuel:

- melange francais/anglais dans les titres et paragraphes
- problemes d'encodage sur certains caracteres
- faute dans le titre `Chaptre 2`
- numerotation incoherente:
  - `2.3.2` est utilise a la fois pour `Patient Management` et pour `Non-Functional Requirements`
  - `Functional requirements for the Visitor` apparait colle a `2.3.1`
- certaines descriptions de modules semblent inversees:
  - `Cognitive Activities Management` contient plutot des elements de suivi/progression
  - `Progressive Cognitive Monitoring Management` contient plutot des elements de creation/affectation d'activites

### 3.3 Ecart entre rapport et code reel

Le depot `MindCare` montre deja une architecture microservices et un front Angular, ce qui merite d'apparaitre dans le rapport.

Services identifies dans le depot:

- `users_service`
- `medical_report_service`
- `recommendation_service`
- `souvenir_service`
- `movement_service`
- `Activities_service`
- `forums_service`
- `api_gateway`
- `front`

Exemples de controllers visibles:

- `UserController`
- `MedicalReportController`
- `MRIScanController`
- `AIResultController`
- `RecommendationController`
- `MedicalEventController`
- `SouvenirPuzzleController`
- `EntreeSouvenirController`
- `PatientController`

Le rapport actuel parle bien de plusieurs domaines fonctionnels, mais il ne montre pas encore clairement leur traduction technique dans l'architecture du projet.

## 4. Structure recommandee pour un rapport PI coherent

Voici une structure simple et defendable, alignee avec la planification et avec l'etat du projet.

### Chapitre 1 - Presentation generale du projet

- contexte medical et problematique
- organisme d'accueil ou cadre academique
- objectifs du projet
- perimetre fonctionnel de MindCare

### Chapitre 2 - Etude prealable et specification des besoins

- etude de l'existant
- identification des acteurs:
  - visiteur
  - patient
  - medecin
  - administrateur
  - famille/accompagnant
- besoins fonctionnels
- besoins non fonctionnels
- cas d'utilisation principaux

### Chapitre 3 - Conception

- architecture generale microservices
- justification des choix technologiques:
  - Angular
  - Spring Boot
  - API Gateway
  - base de donnees
  - IA / MRI / recommandations
- modelisation:
  - diagrammes de cas d'utilisation
  - diagramme de classes ou entites principales
  - flux entre front, gateway et services

### Chapitre 4 - Realisation par sprint

- Sprint 0:
  - analyse
  - exigences
  - benchmark
  - conception initiale
- Sprint 1:
  - integration frontoffice/backoffice
  - entites, associations, CRUD
  - premiers modules metier
- Sprint 2:
  - fonctions avancees
  - recommandations personnalisees
  - activites cognitives
  - souvenirs/puzzles
  - suivi et enrichissements
- DevOps:
  - execution multi-services
  - base de donnees
  - scripts de lancement
  - eventuel conteneurisation/deploiement

### Chapitre 5 - Validation et resultats

- tests fonctionnels
- tests techniques
- scenarios d'usage
- limites rencontrees
- ameliorations futures

### Conclusion generale

- bilan du travail realise
- apports du projet
- perspectives

## 5. Trame de contenu a reutiliser depuis le projet

Le projet permet deja d'alimenter plusieurs sections du futur rapport.

### Architecture et realisation

- Frontend Angular: `front/`
- Gateway/API: `api_gateway/`
- Services metier Spring Boot:
  - `medical_report_service/`
  - `users_service/`
  - `recommendation_service/`
  - `souvenir_service/`
  - `movement_service/`
  - `Activities_service/`
- Scripts et orchestration:
  - `docker-compose.yml`
  - `start-all.sh`
  - `stop-all.sh`
  - `start-db.sh`

### Fonctionnalites coherentes avec le rapport

- gestion des utilisateurs
- gestion des patients
- rapports medicaux
- upload et traitement MRI
- recommandations personnalisees
- activites cognitives
- suivi mouvement/localisation
- souvenirs et puzzle souvenir
- evenements/ressources selon les services actifs

## 6. Priorites de correction sur le rapport actuel

Ordre recommande:

1. Corriger la forme du rapport existant
- harmoniser la langue
- corriger l'encodage
- corriger la numerotation
- corriger les titres et fautes visibles

2. Ajouter les chapitres manquants
- conception
- architecture
- realisation par sprint
- tests et validation
- conclusion

3. Relier explicitement le rapport au code
- ajouter une vue microservices
- lister les modules reels implementes
- illustrer avec quelques ecrans frontoffice/backoffice

4. Ajouter la chronologie PIDEV
- montrer ce qui a ete fait en Sprint 0, Sprint 1, Sprint 2
- mentionner le jalon DevOps lance le 19 fevrier 2026

## 7. Proposition de prise en charge immediate

Si on continue dans cette direction, la suite la plus utile est:

1. produire un plan detaille complet du rapport final
2. rediger directement les chapitres manquants en s'appuyant sur le code du projet
3. preparer une version propre en Markdown ou en Word selon le format que vous voulez rendre

## 8. Perimetre conseille pour la validation de demain

Si vous arretez le rapport a `Sprint 2`, le document peut etre considere comme coherent a condition qu'il couvre:

- `Sprint 0`: analyse, etude de l'existant, besoins, specification
- `Sprint 1`: conception initiale, architecture, premiers modules, CRUD et integration front/back
- `Sprint 2`: fonctions avancees, enrichissement metier, integration inter-modules, etat d'avancement global

Dans cette logique, la partie `DevOps` peut etre laissee de cote pour la validation de demain si elle n'a pas encore ete finalisee.

## 9. Chapitres manquants si vous vous arretez au Sprint 2

Par rapport au PDF actuel, oui, il y a encore des chapitres ou sections importantes manquantes.

### A ajouter obligatoirement

- un chapitre `Conception`
  - architecture generale de la solution
  - choix technologiques
  - diagrammes ou description de l'architecture microservices

- un chapitre `Realisation`
  - organisation du travail par Sprint 1 et Sprint 2
  - modules developpes
  - principales fonctionnalites implementees
  - captures d'ecran ou description des interfaces

- une section `Etat d'avancement jusqu'au Sprint 2`
  - ce qui est termine
  - ce qui est partiellement termine
  - ce qui est reporte apres validation

- une section `Conclusion`
  - bilan a date
  - resultats obtenus
  - prochaines etapes apres Sprint 2

### Recommande mais pas strictement bloquant pour demain

- une courte section `Tests et validation`
  - tests fonctionnels realises
  - scenarios verifies
  - limites connues

### Pas indispensable si vous bloquez a Sprint 2

- un chapitre complet `DevOps / Deploiement`
- une partie finale tres detaillee sur la production ou l'industrialisation

## 10. Conclusion

Le PDF actuel est une bonne base de cadrage, mais il ne couvre pas encore ce que la planification annonce pour les sprints de developpement et de validation. Le plus important n'est pas seulement de corriger la forme: il faut surtout transformer ce document de `specification/etude` en un vrai rapport PI retraçant l'architecture, la realisation par sprint et les resultats obtenus sur MindCare.
