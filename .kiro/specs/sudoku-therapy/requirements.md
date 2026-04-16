# Document de Exigences — Sudoku Thérapeutique

## Introduction

Cette fonctionnalité ajoute le **Sudoku thérapeutique** comme nouveau type d'événement médical dans la plateforme MindCare, dédiée aux patients atteints de la maladie d'Alzheimer. Le Sudoku 4×4 (niveau EASY) est reconnu comme exercice de stimulation cognitive adapté aux patients en phase légère à modérée. La fonctionnalité s'intègre dans le microservice `recommendation_service` (port 8085) existant, en suivant les mêmes patterns architecturaux que le puzzle-souvenir déjà implémenté.

Le médecin crée un jeu Sudoku depuis le backoffice, une recommandation de type SUDOKU est générée, et le patient y accède depuis la page des recommandations du frontoffice pour jouer une session chronométrée.

---

## Glossaire

- **SudokuService** : Service Spring Boot responsable de la génération de grilles, de la gestion des sessions et du calcul des scores.
- **SudokuController** : Contrôleur REST Spring Boot exposant les endpoints de l'API Sudoku.
- **SudokuGame** : Entité JPA représentant un jeu Sudoku assigné à un patient (grille puzzle + solution).
- **SudokuSession** : Entité JPA représentant une session de jeu d'un patient sur un SudokuGame.
- **SudokuPlayComponent** : Composant Angular du frontoffice permettant au patient de jouer une session Sudoku.
- **SudokuBackofficeComponent** : Section du backoffice Angular permettant au médecin de créer un jeu Sudoku.
- **Grille 4×4** : Grille Sudoku de 16 cellules (4 lignes × 4 colonnes), divisée en quatre sous-grilles 2×2.
- **Puzzle** : Grille 4×4 avec 6 cellules vidées (valeur 0), présentée au patient.
- **Solution** : Grille 4×4 complète et valide, stockée côté serveur.
- **Score** : Valeur entière calculée à partir de la durée, du nombre d'erreurs et des indices utilisés.
- **MedicalEvent** : Entité représentant un événement médical dans le système MindCare.
- **Recommendation** : Entité représentant une recommandation thérapeutique liée à un MedicalEvent.
- **RecommendationService** : Service Angular du frontoffice consommant l'API du `recommendation_service`.
- **DifficultyLevel.EASY** : Niveau de difficulté correspondant à une grille 4×4 avec 6 cellules vides.
- **ObjectMapper** : Bean Jackson configuré dans `RestClientConfig` pour la sérialisation/désérialisation JSON des grilles.

---

## Exigences

### Exigence 1 : Génération de grille Sudoku 4×4

**User Story :** En tant que médecin, je veux créer un jeu Sudoku 4×4 pour un patient, afin de lui proposer un exercice de stimulation cognitive adapté à son niveau.

#### Critères d'acceptation

1. WHEN le médecin soumet une requête de création avec un `patientId` valide et `difficulty = EASY`, THE `SudokuService` SHALL générer une grille solution 4×4 valide où chaque ligne, chaque colonne et chaque sous-grille 2×2 contient exactement les chiffres 1, 2, 3 et 4 sans répétition.
2. WHEN une grille solution valide est générée, THE `SudokuService` SHALL retirer exactement 6 cellules de la solution pour produire le puzzle, en remplaçant les valeurs retirées par 0.
3. WHEN le puzzle est créé, THE `SudokuService` SHALL persister le `SudokuGame` avec les champs `puzzle` (JSON), `solution` (JSON), `gridSize = 4`, `difficulty = EASY` et `active = true`.
4. WHEN le `SudokuGame` est persisté, THE `SudokuService` SHALL créer un `MedicalEvent` associé de type `SUDOKU` avec le statut `ACTIVE`.
5. IF le `patientId` fourni est nul ou inférieur ou égal à zéro, THEN THE `SudokuService` SHALL retourner une erreur de validation avec le code HTTP 400.
6. THE `SudokuService` SHALL sérialiser et désérialiser les grilles puzzle et solution au format JSON via le bean `ObjectMapper` déclaré dans `RestClientConfig`.
7. FOR ALL grilles solution générées, THE `SudokuService` SHALL garantir que parser la grille JSON puis la reformater produit une grille équivalente (propriété de round-trip JSON).

---

### Exigence 2 : Endpoints REST du SudokuController

**User Story :** En tant que développeur frontend, je veux des endpoints REST documentés pour créer un jeu Sudoku, démarrer une session et soumettre les résultats, afin d'intégrer le Sudoku dans l'interface Angular.

#### Critères d'acceptation

1. THE `SudokuController` SHALL exposer un endpoint `POST /api/sudoku` acceptant un `SudokuCreateRequest` et retournant un `SudokuResponse` avec le code HTTP 201.
2. THE `SudokuController` SHALL exposer un endpoint `GET /api/sudoku/event/{eventId}` retournant le `SudokuResponse` correspondant au `MedicalEvent` identifié par `eventId`.
3. THE `SudokuController` SHALL exposer un endpoint `POST /api/sudoku/{gameId}/sessions/start` acceptant un `patientId` en paramètre de requête et retournant un `SudokuSessionStartResponse` avec le code HTTP 201.
4. THE `SudokuController` SHALL exposer un endpoint `POST /api/sudoku/{gameId}/sessions/{sessionId}/submit` acceptant un `SudokuSessionSubmitRequest` et retournant un `SudokuSessionResponse`.
5. THE `SudokuController` SHALL exposer un endpoint `GET /api/sudoku/{gameId}/sessions` acceptant un `patientId` en paramètre de requête et retournant la liste des `SudokuSessionResponse` du patient pour ce jeu.
6. IF un `gameId` ou `sessionId` inexistant est fourni, THEN THE `SudokuController` SHALL retourner une réponse avec le code HTTP 404 et un message d'erreur descriptif.
7. IF une session a déjà été soumise (champ `finishedAt` non nul), THEN THE `SudokuController` SHALL rejeter toute nouvelle soumission pour cette session avec le code HTTP 409.

---

### Exigence 3 : Gestion des sessions et calcul du score

**User Story :** En tant que patient, je veux que ma progression et mon score soient enregistrés à chaque session de jeu, afin de suivre mon évolution thérapeutique.

#### Critères d'acceptation

1. WHEN le patient démarre une session, THE `SudokuService` SHALL créer une `SudokuSession` avec `startedAt = now()`, `completed = false`, `abandoned = false`, `errorsCount = 0`, `hintsUsed = 0` et `completionPercent = 0`.
2. WHEN le patient soumet une session avec `completed = true` et `completionPercent = 100`, THE `SudokuService` SHALL calculer le score selon la formule : `score = max(0, 100 - (errorsCount × 5) - (hintsUsed × 10) - penalitéTemps)` où `penalitéTemps = max(0, durationSeconds - timeLimitSeconds) / 10`.
3. WHEN une session est soumise, THE `SudokuService` SHALL persister la `SudokuSession` avec `finishedAt = now()` et le score calculé.
4. WHEN une session est complétée avec `completionPercent = 100`, THE `SudokuService` SHALL mettre à jour le champ `bestScore` du `SudokuGame` si le score de la session est supérieur au `bestScore` actuel.
5. WHEN une session est soumise avec `abandoned = true`, THE `SudokuService` SHALL persister la session avec `completed = false` et `score = 0`.
6. IF le `patientId` de la session ne correspond pas au `patientId` du `SudokuGame`, THEN THE `SudokuService` SHALL rejeter la soumission avec le code HTTP 403.
7. FOR ALL sessions soumises avec `completed = true`, THE `SudokuService` SHALL garantir que `score >= 0`.

---

### Exigence 4 : Mise à jour des enums et de la base de données

**User Story :** En tant qu'administrateur système, je veux que les enums Java et les colonnes MySQL soient mis à jour pour inclure le type SUDOKU, afin d'assurer la cohérence des données.

#### Critères d'acceptation

1. THE `MedicalEventType` enum Java SHALL contenir la valeur `SUDOKU` (déjà présente — à vérifier lors de l'intégration).
2. THE `RecommendationType` enum Java SHALL contenir la valeur `SUDOKU` (déjà présente — à vérifier lors de l'intégration).
3. THE `MedicalEventType` enum TypeScript dans `recommendation.model.ts` SHALL contenir la valeur `SUDOKU = 'SUDOKU'`.
4. THE `RecommendationType` enum TypeScript dans `recommendation.model.ts` SHALL contenir la valeur `SUDOKU = 'SUDOKU'`.
5. THE script SQL de migration SHALL exécuter `ALTER TABLE medical_events MODIFY COLUMN type ENUM(...)` en ajoutant `'SUDOKU'` à la liste des valeurs autorisées.
6. THE script SQL de migration SHALL exécuter `ALTER TABLE recommendations MODIFY COLUMN type ENUM(...)` en ajoutant `'SUDOKU'` à la liste des valeurs autorisées.
7. IF la colonne MySQL `type` ne contient pas la valeur `'SUDOKU'` lors de l'insertion d'un `MedicalEvent` de type SUDOKU, THEN la base de données SHALL rejeter l'insertion avec une erreur de contrainte.

---

### Exigence 5 : Composant Angular frontoffice — SudokuPlayComponent

**User Story :** En tant que patient, je veux jouer au Sudoku depuis mon navigateur via une interface claire et adaptée, afin de réaliser mon exercice thérapeutique.

#### Critères d'acceptation

1. WHEN le patient navigue vers `/sudoku-play/:eventId`, THE `SudokuPlayComponent` SHALL charger le `SudokuGame` correspondant via `GET /api/sudoku/event/{eventId}` et afficher la grille 4×4.
2. WHEN la grille est affichée, THE `SudokuPlayComponent` SHALL présenter les cellules pré-remplies comme non modifiables et les cellules vides (valeur 0) comme des champs de saisie acceptant uniquement les chiffres 1 à 4.
3. WHEN le patient saisit une valeur incorrecte dans une cellule (valeur ne respectant pas les règles Sudoku), THE `SudokuPlayComponent` SHALL mettre en évidence la cellule en erreur visuellement sans bloquer la saisie.
4. WHEN le patient clique sur "Valider", THE `SudokuPlayComponent` SHALL soumettre la session via `POST /api/sudoku/{gameId}/sessions/{sessionId}/submit` avec les données de durée, d'erreurs et de complétion.
5. WHEN la session est soumise avec succès, THE `SudokuPlayComponent` SHALL afficher le score obtenu et un message de félicitations si `completed = true`.
6. WHEN le patient charge la page, THE `SudokuPlayComponent` SHALL démarrer automatiquement une session via `POST /api/sudoku/{gameId}/sessions/start` et lancer un chronomètre visible.
7. IF le patient n'est pas connecté ou n'a pas le rôle `PATIENT`, THEN THE `SudokuPlayComponent` SHALL afficher un message d'erreur et ne pas démarrer de session.
8. IF le chargement du `SudokuGame` échoue (erreur réseau ou 404), THEN THE `SudokuPlayComponent` SHALL afficher un message d'erreur explicite en français.
9. THE route `/sudoku-play/:eventId` SHALL être déclarée dans `FrontofficeRoutingModule` et le composant `SudokuPlayComponent` SHALL être déclaré dans `FrontofficeModule`.

---

### Exigence 6 : Bouton "Commencer le Sudoku" dans la page des recommandations

**User Story :** En tant que patient, je veux voir un bouton d'action sur mes recommandations de type SUDOKU, afin d'accéder directement au jeu depuis ma liste de recommandations.

#### Critères d'acceptation

1. WHEN une recommandation de type `SUDOKU` est affichée dans la page `RecommendationsPage`, THE `RecommendationsPage` SHALL afficher un bouton "Commencer le Sudoku" associé à cette recommandation.
2. WHEN le patient clique sur "Commencer le Sudoku", THE `RecommendationsPage` SHALL naviguer vers `/sudoku-play/{generatedMedicalEventId}` en utilisant le champ `generatedMedicalEventId` de la recommandation.
3. WHILE une recommandation de type `SUDOKU` a le statut `ACCEPTED`, THE `RecommendationsPage` SHALL afficher le bouton "Commencer le Sudoku" avec un style visuel distinct (couleur différente du bouton puzzle).
4. IF le champ `generatedMedicalEventId` d'une recommandation SUDOKU est nul, THEN THE `RecommendationsPage` SHALL désactiver le bouton "Commencer le Sudoku" et afficher une infobulle indiquant que le jeu n'est pas encore disponible.
5. THE `getTypeBadgeClass` de `RecommendationsPage` SHALL retourner une classe CSS distincte pour le type `SUDOKU`.

---

### Exigence 7 : Création de Sudoku dans le backoffice Angular

**User Story :** En tant que médecin, je veux créer un jeu Sudoku pour un patient depuis le backoffice, afin de lui prescrire cet exercice thérapeutique.

#### Critères d'acceptation

1. WHEN le médecin accède à la page de gestion des événements médicaux ou des souvenirs dans le backoffice, THE `SudokuBackofficeComponent` SHALL afficher un formulaire de création de Sudoku avec les champs : `patientId`, `timeLimitSeconds` (défaut : 300) et `difficulty` (valeur fixe : EASY).
2. WHEN le médecin soumet le formulaire avec un `patientId` valide, THE `SudokuBackofficeComponent` SHALL appeler `POST /api/sudoku` et afficher un message de succès indiquant l'identifiant du `MedicalEvent` créé.
3. IF le champ `patientId` est vide ou non numérique lors de la soumission, THEN THE `SudokuBackofficeComponent` SHALL afficher un message de validation en français sans appeler l'API.
4. WHEN la création réussit, THE `SudokuBackofficeComponent` SHALL réinitialiser le formulaire et afficher le message : "Sudoku créé avec succès (event #{medicalEventId})".
5. IF l'appel API échoue, THEN THE `SudokuBackofficeComponent` SHALL afficher le message d'erreur retourné par le serveur ou un message générique en français.

---

### Exigence 8 : Configuration ObjectMapper dans RestClientConfig

**User Story :** En tant que développeur backend, je veux un bean `ObjectMapper` configuré dans `RestClientConfig`, afin de garantir la sérialisation correcte des grilles Sudoku au format JSON.

#### Critères d'acceptation

1. THE `RestClientConfig` SHALL déclarer un bean `ObjectMapper` annoté `@Bean` avec la configuration `FAIL_ON_UNKNOWN_PROPERTIES = false`.
2. WHEN le `SudokuService` sérialise une grille `int[][]` en JSON, THE `ObjectMapper` SHALL produire une chaîne JSON valide de type tableau de tableaux (ex. : `[[1,2,3,4],[3,4,1,2],...]`).
3. WHEN le `SudokuService` désérialise une chaîne JSON de grille, THE `ObjectMapper` SHALL reconstruire un `int[][]` équivalent à la grille originale.
4. FOR ALL grilles `int[][]` valides, THE `ObjectMapper` SHALL garantir que `deserialiser(sérialiser(grille))` produit une grille identique à la grille originale (propriété de round-trip).
