# Document des Exigences : Puzzle Therapy (Éléments Manquants)

## Introduction

Ce document couvre uniquement les éléments **non encore implémentés** de la fonctionnalité Puzzle Therapy. Le backend (entités, services, contrôleurs, DTOs) et la majorité du frontend (modèles, service, composant de jeu, routes frontoffice, formulaire de création dans souvenirs) sont déjà en place. Les exigences ci-dessous portent exclusivement sur :

1. Le composant Angular `puzzle-management` (tableau de bord clinique médecin)
2. L'enregistrement de la route backoffice `puzzle-management`
3. La déclaration du composant dans le module backoffice
4. Le lien de navigation dans la sidebar backoffice
5. Le bouton "Commencer l'exercice" dans la liste des recommandations frontoffice

---

## Glossaire

- **PuzzleManagementPage** : Composant Angular du tableau de bord clinique médecin, situé dans `backoffice/puzzle-management/`
- **BackofficeRoutingModule** : Module de routage Angular du back-office (`backoffice-routing-module.ts`)
- **BackofficeModule** : Module Angular principal du back-office (`backoffice-module.ts`)
- **Sidebar** : Composant de navigation latérale du back-office (`backoffice/sidebar/sidebar.html`)
- **RecommendationsPage** : Composant Angular de la liste des recommandations frontoffice (`frontoffice/recommendations/`)
- **PuzzleSession** : Objet représentant une session de jeu d'un patient sur un puzzle
- **Puzzle** : Objet `SouvenirPuzzle` retourné par `GET /api/puzzles/patient/{patientId}`
- **RecommendationService** : Service Angular existant exposant toutes les méthodes d'appel API puzzle
- **DOCTOR** : Rôle utilisateur médecin authentifié via Keycloak
- **PATIENT** : Rôle utilisateur patient authentifié via Keycloak

---

## Exigences

### Exigence 1 : Composant PuzzleManagementPage — Tableau de bord clinique

**User Story :** En tant que médecin, je veux consulter un tableau de bord listant les puzzles de mes patients et leurs statistiques de sessions, afin de suivre leur progression thérapeutique.

#### Critères d'acceptation

1. WHEN le médecin navigue vers `/admin/puzzle-management`, THE PuzzleManagementPage SHALL afficher la liste des puzzles du patient sélectionné en appelant `GET /api/puzzles/patient/{patientId}`
2. WHEN la liste des puzzles est chargée, THE PuzzleManagementPage SHALL afficher pour chaque puzzle : le titre, la difficulté, le statut, le meilleur score (`bestScore`) et le nombre de sessions complétées (`completedSessions`)
3. WHEN le médecin sélectionne un puzzle, THE PuzzleManagementPage SHALL afficher les sessions associées en appelant `GET /api/puzzles/{puzzleId}/sessions/patient/{patientId}`
4. WHEN les sessions sont affichées, THE PuzzleManagementPage SHALL présenter pour chaque session : la date, la durée, le nombre de mouvements, le nombre d'erreurs, les indices utilisés, le pourcentage de complétion et le score
5. IF l'appel à `GET /api/puzzles/patient/{patientId}` échoue, THEN THE PuzzleManagementPage SHALL afficher un message d'erreur et permettre de réessayer
6. WHEN aucun puzzle n'existe pour le patient sélectionné, THE PuzzleManagementPage SHALL afficher un message indiquant l'absence de puzzles

### Exigence 2 : Route backoffice `puzzle-management`

**User Story :** En tant que développeur, je veux que la route `/admin/puzzle-management` soit enregistrée dans le module de routage backoffice, afin que la navigation vers le tableau de bord clinique fonctionne correctement.

#### Critères d'acceptation

1. THE BackofficeRoutingModule SHALL contenir une entrée de route avec `path: 'puzzle-management'` et `component: PuzzleManagementPage`
2. WHEN un utilisateur navigue vers `/admin/puzzle-management`, THE BackofficeRoutingModule SHALL charger le composant PuzzleManagementPage sans erreur de routage

### Exigence 3 : Déclaration du composant dans BackofficeModule

**User Story :** En tant que développeur, je veux que `PuzzleManagementPage` soit déclaré dans le `BackofficeModule`, afin qu'Angular puisse le résoudre lors de la compilation et du routage.

#### Critères d'acceptation

1. THE BackofficeModule SHALL déclarer `PuzzleManagementPage` dans son tableau `declarations`
2. WHEN l'application Angular est compilée, THE BackofficeModule SHALL résoudre `PuzzleManagementPage` sans erreur de compilation

### Exigence 4 : Lien de navigation dans la Sidebar

**User Story :** En tant que médecin, je veux voir un lien vers le tableau de bord des puzzles dans la barre de navigation latérale du back-office, afin d'y accéder directement depuis n'importe quelle page.

#### Critères d'acceptation

1. THE Sidebar SHALL contenir un lien de navigation pointant vers `/admin/puzzle-management`
2. WHEN le médecin clique sur le lien puzzle dans la Sidebar, THE Sidebar SHALL naviguer vers `/admin/puzzle-management`

### Exigence 5 : Bouton "Commencer l'exercice" dans la liste des recommandations

**User Story :** En tant que patient, je veux voir un bouton "Commencer l'exercice" sur les recommandations de type PUZZLE, afin de démarrer directement le jeu depuis ma liste de recommandations.

#### Critères d'acceptation

1. WHEN une recommandation de type `PUZZLE` possède un `generatedMedicalEventId` non nul, THE RecommendationsPage SHALL afficher un bouton "Commencer l'exercice" pour cette recommandation
2. WHEN le patient clique sur "Commencer l'exercice", THE RecommendationsPage SHALL naviguer vers `/puzzle-play/{generatedMedicalEventId}`
3. WHEN une recommandation de type `PUZZLE` ne possède pas de `generatedMedicalEventId`, THE RecommendationsPage SHALL ne pas afficher le bouton "Commencer l'exercice"
4. WHEN une recommandation n'est pas de type `PUZZLE`, THE RecommendationsPage SHALL ne pas afficher le bouton "Commencer l'exercice"
