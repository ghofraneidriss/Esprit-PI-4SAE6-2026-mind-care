# Plan d'Implémentation : Sudoku Thérapeutique

## Vue d'ensemble

Implémentation complète de la fonctionnalité Sudoku thérapeutique dans la plateforme MindCare. Les entités JPA, DTOs, repositories et enums sont déjà présents. Les tâches couvrent : le bean `ObjectMapper`, le `SudokuService`, le `SudokuController`, la migration SQL, les composants Angular frontoffice et backoffice, et le câblage des routes.

## Tâches

- [ ] 1. Vérifier et compléter le bean ObjectMapper dans RestClientConfig.java
  - Ouvrir `recommendation_service/src/main/java/tn/esprit/recommendation_service/config/RestClientConfig.java`
  - Vérifier que le bean `@Bean public ObjectMapper objectMapper()` est déclaré avec `FAIL_ON_UNKNOWN_PROPERTIES = false`, `JavaTimeModule` et `WRITE_DATES_AS_TIMESTAMPS` désactivé
  - Si absent, l'ajouter ; si présent, confirmer la configuration
  - _Exigences : 8.1, 8.2, 8.3_

- [ ] 2. Implémenter SudokuService.java
  - [ ] 2.1 Implémenter la génération de grille 4×4 par backtracking
    - Créer `generateSolution(int size)` : remplit la grille par backtracking avec shuffle aléatoire des chiffres 1–4
    - Créer `isValid(int[][] grid, int row, int col, int num, int size)` : vérifie ligne, colonne et sous-grille 2×2
    - Créer `generatePuzzle(int[][] solution, DifficultyLevel difficulty)` : retire exactement 6 cellules (valeur 0) pour EASY
    - Créer `toJson(int[][] grid)` : sérialise via `ObjectMapper`
    - _Exigences : 1.1, 1.2, 1.6_

  - [ ]* 2.2 Écrire le test de propriété pour la validité de la grille solution
    - **Propriété 1 : Validité de la grille solution**
    - Pour toute grille générée par `generateSolution(4)`, vérifier que chaque ligne, colonne et sous-grille 2×2 contient exactement {1,2,3,4}
    - **Valide : Exigence 1.1**

  - [ ]* 2.3 Écrire le test de propriété pour le round-trip JSON
    - **Propriété 2 : Round-trip JSON de la grille**
    - Pour toute grille `int[][]` valide, `deserialiser(sérialiser(grille))` produit une grille identique
    - **Valide : Exigences 1.7, 8.4**

  - [ ]* 2.4 Écrire le test de propriété pour le nombre de zéros dans le puzzle
    - **Propriété 5 : Puzzle contient exactement 6 zéros**
    - Pour tout puzzle généré depuis une solution 4×4, compter les cellules à 0 et vérifier que le total est exactement 6
    - **Valide : Exigence 1.2**

  - [ ] 2.5 Implémenter createGame()
    - Valider `patientId` (non nul, > 0) — lever `BusinessException` avec HTTP 400 sinon
    - Appeler `generateSolution(4)` puis `generatePuzzle(solution, EASY)`
    - Créer et persister un `MedicalEvent` de type `SUDOKU`, statut `ACTIVE`
    - Créer et persister le `SudokuGame` avec `puzzle`, `solution`, `gridSize=4`, `active=true`
    - Retourner un `SudokuResponse` via `toResponse(game)`
    - _Exigences : 1.3, 1.4, 1.5_

  - [ ] 2.6 Implémenter getById(), getByEvent(), getByPatient()
    - `getById(Long gameId)` : cherche par id, lève `ResourceNotFoundException` si absent
    - `getByEvent(Long eventId)` : utilise `SudokuGameRepository.findByMedicalEvent_Id(eventId)`
    - `getByPatient(Long patientId)` : utilise `findByPatientIdOrderByCreatedAtDesc`
    - _Exigences : 2.2, 2.6_

  - [ ] 2.7 Implémenter startSession()
    - Créer une `SudokuSession` avec `startedAt=now()`, `completed=false`, `abandoned=false`, `errorsCount=0`, `hintsUsed=0`, `completionPercent=0`
    - Retourner `SudokuSessionStartResponse`
    - _Exigences : 3.1_

  - [ ] 2.8 Implémenter submitSession() avec calcul du score
    - Vérifier que la session appartient au jeu (`session.sudokuGame.id == gameId`)
    - Vérifier que `session.patientId == request.patientId` — lever `BusinessException` HTTP 403 sinon
    - Vérifier que `session.finishedAt == null` — lever `BusinessException` HTTP 409 sinon
    - Calculer le score : `max(0, 100 - errorsCount*5 - hintsUsed*10 - max(0, duration-timeLimit)/10)`
    - Si `abandoned=true` : score = 0
    - Persister la session avec `finishedAt=now()` et le score calculé
    - Mettre à jour `bestScore` du jeu si `completed=true` et score supérieur
    - _Exigences : 3.2, 3.3, 3.4, 3.5, 3.6_

  - [ ]* 2.9 Écrire le test de propriété pour le score toujours positif
    - **Propriété 3 : Score toujours positif ou nul**
    - Pour toute combinaison valide de `(durationSeconds, errorsCount, hintsUsed, timeLimitSeconds)` avec `completed=true`, vérifier que `calculateScore(...)` retourne `>= 0`
    - **Valide : Exigence 3.7**

  - [ ]* 2.10 Écrire le test de propriété pour la monotonie inverse du score
    - **Propriété 4 : Monotonie inverse du score par rapport aux erreurs**
    - Pour s1 et s2 avec `s1.errorsCount < s2.errorsCount` et autres paramètres identiques, vérifier que `calculateScore(s1) >= calculateScore(s2)`
    - **Valide : Exigence 3.2**

  - [ ] 2.11 Implémenter getSessionsByPatient()
    - Utilise `SudokuSessionRepository.findBySudokuGame_IdAndPatientIdOrderByStartedAtDesc`
    - _Exigences : 2.5_

- [ ] 3. Implémenter SudokuController.java
  - [ ] 3.1 Créer le contrôleur avec les 6 endpoints
    - `POST /api/sudoku` → `createGame()` → HTTP 201
    - `GET /api/sudoku/event/{eventId}` → `getByEvent()`
    - `GET /api/sudoku/patient/{patientId}` → `getByPatient()`
    - `POST /api/sudoku/{gameId}/sessions/start?patientId=` → `startSession()` → HTTP 201
    - `POST /api/sudoku/{gameId}/sessions/{sessionId}/submit` → `submitSession()`
    - `GET /api/sudoku/{gameId}/sessions?patientId=` → `getSessionsByPatient()`
    - Annoter avec `@RestController`, `@RequestMapping("/api/sudoku")`, `@CrossOrigin(origins = "*")`
    - _Exigences : 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 3.2 Gérer les erreurs HTTP 404, 409, 403
    - Vérifier que `ResourceNotFoundException` est mappée en 404 par le gestionnaire d'exceptions global
    - Vérifier que `BusinessException` est mappée en 400/403/409 selon le message
    - _Exigences : 2.6, 2.7_

- [ ] 4. Checkpoint — Vérifier le backend
  - S'assurer que tous les tests passent, demander à l'utilisateur si des questions se posent.

- [ ] 5. Créer le script de migration SQL
  - Créer le fichier `recommendation_service/src/main/resources/db/migration/V{N}__add_sudoku_enum.sql`
  - Ajouter `ALTER TABLE medical_events MODIFY COLUMN type ENUM(...)` avec la valeur `'SUDOKU'`
  - Ajouter `ALTER TABLE recommendations MODIFY COLUMN type ENUM(...)` avec la valeur `'SUDOKU'`
  - Inclure toutes les valeurs existantes pour ne pas casser les données en place
  - _Exigences : 4.5, 4.6, 4.7_

- [ ] 6. Vérifier les enums TypeScript dans recommendation.model.ts
  - Ouvrir `front/src/app/backoffice/recommendation/recommendation.model.ts`
  - Confirmer que `MedicalEventType.SUDOKU = 'SUDOKU'` est présent
  - Confirmer que `RecommendationType.SUDOKU = 'SUDOKU'` est présent
  - Si absent, ajouter la valeur dans chaque enum
  - _Exigences : 4.3, 4.4_

- [ ] 7. Vérifier les méthodes Sudoku dans recommendation.service.ts
  - Ouvrir `front/src/app/backoffice/recommendation/recommendation.service.ts`
  - Confirmer que `createSudoku`, `getSudokuByEvent`, `startSudokuSession`, `submitSudokuSession`, `getSudokuSessions` sont présentes
  - Si absentes, les ajouter en suivant le pattern des méthodes puzzle existantes
  - _Exigences : 2.1, 2.3, 2.4, 2.5_

- [ ] 8. Implémenter le composant SudokuPlayPage (frontoffice)
  - [ ] 8.1 Créer sudoku-play.html — template de la grille 4×4
    - Afficher la grille 4×4 avec `*ngFor` sur les lignes et colonnes
    - Cellules fixes (`fixedCells[r][c] == true`) : `<span>` non modifiable
    - Cellules vides : `<input type="number" min="1" max="4">` avec `(input)="onCellInput(r, c, $event)"`
    - Appliquer une classe CSS `error-cell` si `errorCells[r][c] == true`
    - Afficher le chronomètre (`formattedTime`), la barre de progression, le bouton "Valider"
    - Overlay de succès avec score si `showSuccessOverlay == true`
    - Message d'erreur si `errorMessage` non vide
    - _Exigences : 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_

  - [ ] 8.2 Créer sudoku-play.css — styles de la grille et de l'overlay
    - Grille CSS `display: grid; grid-template-columns: repeat(4, 1fr)`
    - Style distinct pour cellules fixes vs modifiables
    - Style rouge/orange pour `error-cell`
    - Overlay de succès centré avec fond semi-transparent
    - _Exigences : 5.2, 5.3, 5.5_

  - [ ] 8.3 Compléter sudoku-play.ts — logique du composant
    - Vérifier que `loadGame()`, `initGrid()`, `startSession()`, `onCellInput()`, `validateSolution()`, `doSubmit()` sont implémentées
    - Vérifier que `updateErrorCells()` appelle `isCellValid()` pour chaque cellule modifiée
    - Vérifier que `ngOnDestroy()` soumet la session comme abandonnée si non soumise
    - Vérifier le guard `canPlay` : afficher un message si non connecté ou non PATIENT
    - _Exigences : 5.1, 5.2, 5.3, 5.4, 5.6, 5.7, 5.8_

- [ ] 9. Enregistrer la route /sudoku-play/:eventId dans FrontofficeRoutingModule
  - Ouvrir `front/src/app/frontoffice/frontoffice-routing-module.ts`
  - Ajouter l'import `SudokuPlayPage` depuis `./sudoku-play/sudoku-play`
  - Ajouter la route `{ path: 'sudoku-play/:eventId', component: SudokuPlayPage }`
  - _Exigences : 5.9_

- [ ] 10. Déclarer SudokuPlayPage dans FrontofficeModule
  - Ouvrir `front/src/app/frontoffice/frontoffice-module.ts`
  - Ajouter l'import `SudokuPlayPage` depuis `./sudoku-play/sudoku-play`
  - Ajouter `SudokuPlayPage` dans le tableau `declarations`
  - _Exigences : 5.9_

- [ ] 11. Ajouter le bouton "Commencer le Sudoku" dans recommendations.html
  - Ouvrir `front/src/app/frontoffice/recommendations/recommendations.html`
  - Après le bloc `*ngIf="rec.type === 'PUZZLE'"` existant, ajouter un bloc `*ngIf="rec.type === 'SUDOKU' && rec.generatedMedicalEventId"`
  - Bouton `[routerLink]="['/sudoku-play', rec.generatedMedicalEventId]"` avec texte "Commencer le Sudoku"
  - Ajouter un bouton désactivé `[disabled]="true"` si `rec.type === 'SUDOKU' && !rec.generatedMedicalEventId`
  - Vérifier que `getTypeBadgeClass()` dans `recommendations.ts` retourne une classe CSS pour `'SUDOKU'`
  - _Exigences : 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 12. Créer le composant SudokuManagementPage (backoffice)
  - [ ] 12.1 Créer sudoku-management.ts
    - Propriétés : `patientId: number`, `timeLimitSeconds: number = 300`, `difficulty = 'EASY'`
    - Propriétés d'état : `isLoading`, `successMessage`, `errorMessage`
    - Méthode `createSudoku()` : valider `patientId`, appeler `recommendationService.createSudoku(...)`, afficher `"Sudoku créé avec succès (event #<medicalEventId>)"`, réinitialiser le formulaire
    - _Exigences : 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ] 12.2 Créer sudoku-management.html
    - Formulaire avec champs `patientId` (number, required), `timeLimitSeconds` (number, défaut 300)
    - Champ `difficulty` affiché en lecture seule avec valeur "EASY"
    - Bouton "Créer le Sudoku" avec spinner pendant le chargement
    - Affichage du message de succès (vert) ou d'erreur (rouge)
    - Validation HTML5 : `required`, `min="1"` sur `patientId`
    - _Exigences : 7.1, 7.3, 7.4, 7.5_

  - [ ] 12.3 Créer sudoku-management.css
    - Styles du formulaire cohérents avec `puzzle-management.css` existant
    - _Exigences : 7.1_

- [ ] 13. Enregistrer SudokuManagementPage dans le backoffice
  - Ouvrir `front/src/app/backoffice/backoffice-module.ts`
  - Ajouter l'import `SudokuManagementPage` depuis `./sudoku-management/sudoku-management`
  - Ajouter `SudokuManagementPage` dans le tableau `declarations`
  - Ouvrir `front/src/app/backoffice/backoffice-routing-module.ts`
  - Ajouter l'import et la route `{ path: 'sudoku-management', component: SudokuManagementPage }`
  - _Exigences : 7.1_

- [ ] 14. Checkpoint final — Vérifier l'intégration complète
  - S'assurer que tous les tests passent, demander à l'utilisateur si des questions se posent.

## Notes

- Les tâches marquées `*` sont optionnelles et peuvent être ignorées pour un MVP rapide
- Les entités JPA, DTOs, repositories et enums Java/TypeScript sont déjà présents — ne pas les recréer
- Le `SudokuService` et le `SudokuController` existent déjà partiellement — vérifier avant d'écraser
- La formule de score est : `max(0, 100 - errorsCount*5 - hintsUsed*10 - max(0, duration-timeLimit)/10)`
- Les tests de propriétés valident les invariants mathématiques du service (génération de grille, score, JSON)
