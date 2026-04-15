# Plan d'implémentation : Puzzle Therapy

## Vue d'ensemble

Implémentation des 5 éléments manquants de la fonctionnalité Puzzle Therapy dans l'application Angular (TypeScript). Le backend et la majorité du frontend sont déjà en place ; seuls les éléments listés ci-dessous restent à créer ou modifier.

## Tâches

- [x] 1. Créer le composant `PuzzleManagementPage` (tableau de bord clinique médecin)
  - [x] 1.1 Créer `backoffice/puzzle-management/puzzle-management.ts`
    - Déclarer le composant Angular avec `selector: 'app-puzzle-management'`, `templateUrl` et `styleUrls`
    - Injecter `RecommendationService` et `Router`
    - Implémenter `patientIdInput: number | null`, `puzzles: Puzzle[]`, `selectedPuzzle: Puzzle | null`, `sessions: PuzzleSession[]`, `loading`, `errorMessage`
    - Implémenter `loadPuzzles(patientId: number)` : appel `getPuzzlesByPatient(patientId)`, gestion erreur avec message et possibilité de réessayer
    - Implémenter `loadSessions(puzzle: Puzzle)` : appel `getPuzzleSessions(puzzle.id, puzzle.patientId)`, stockage dans `sessions`
    - _Exigences : 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 1.2 Créer `backoffice/puzzle-management/puzzle-management.html`
    - Champ de saisie `patientId` avec bouton "Charger"
    - Tableau des puzzles : colonnes titre, difficulté, statut, meilleur score, sessions complétées ; ligne cliquable pour sélectionner
    - Message vide si aucun puzzle (`exigence 1.6`)
    - Tableau des sessions du puzzle sélectionné : colonnes date, durée, mouvements, erreurs, indices, complétion (%), score
    - Bloc d'erreur avec bouton "Réessayer" (`exigence 1.5`)
    - _Exigences : 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 1.3 Créer `backoffice/puzzle-management/puzzle-management.css`
    - Styles de base : tableau responsive, ligne sélectionnée mise en évidence, état de chargement
    - _Exigences : 1.1_

  - [ ]* 1.4 Écrire le test de propriété — Rendu complet des données puzzle
    - **Propriété 1 : Pour tout tableau de puzzles chargé, chaque puzzle rendu expose dans le DOM son titre, sa difficulté, son statut, son `bestScore` et son `completedSessions`**
    - **Valide : Exigence 1.2**

  - [ ]* 1.5 Écrire le test de propriété — Rendu complet des données de session
    - **Propriété 2 : Pour tout tableau de sessions chargé, chaque session rendue expose dans le DOM sa date, sa durée, ses mouvements, ses erreurs, ses indices, son pourcentage de complétion et son score**
    - **Valide : Exigence 1.4**

- [x] 2. Enregistrer la route `puzzle-management` dans `BackofficeRoutingModule`
  - Modifier `backoffice-routing-module.ts` : ajouter l'import de `PuzzleManagementPage` et insérer `{ path: 'puzzle-management', component: PuzzleManagementPage }` avant la route wildcard `**`
  - _Exigences : 2.1, 2.2_

- [x] 3. Déclarer `PuzzleManagementPage` dans `BackofficeModule`
  - Modifier `backoffice-module.ts` : ajouter l'import de `PuzzleManagementPage` et l'ajouter dans le tableau `declarations`
  - _Exigences : 3.1, 3.2_

- [x] 4. Ajouter le lien de navigation dans la Sidebar
  - Modifier `backoffice/sidebar/sidebar.html` : ajouter un `<li class="menu-item">` avec `<a class="menu-link" routerLink="/admin/puzzle-management">` et un libellé "Gestion Puzzles" (icône `fi fi-rr-puzzle-piece` ou équivalent disponible)
  - _Exigences : 4.1, 4.2_

- [x] 5. Ajouter le bouton "Commencer l'exercice" dans `recommendations.html`
  - Modifier `frontoffice/recommendations/recommendations.html` : à l'intérieur de la boucle `@for`, après le bloc `patient-action-panel`, ajouter un bloc conditionnel `@if (rec.type === 'PUZZLE' && rec.generatedMedicalEventId)` contenant un `<a>` ou `<button>` avec `routerLink="/puzzle-play/{{ rec.generatedMedicalEventId }}"` et le texte "Commencer l'exercice"
  - Ne pas afficher le bouton si `rec.type !== 'PUZZLE'` ou `rec.generatedMedicalEventId` est nul (`exigences 5.3, 5.4`)
  - _Exigences : 5.1, 5.2, 5.3, 5.4_

  - [ ]* 5.1 Écrire le test de propriété — Visibilité conditionnelle du bouton
    - **Propriété 3 : Pour tout tableau de recommandations affiché, le bouton "Commencer l'exercice" apparaît si et seulement si `rec.type === 'PUZZLE'` ET `rec.generatedMedicalEventId != null`**
    - **Valide : Exigences 5.1, 5.3, 5.4**

- [x] 6. Point de contrôle — Vérifier que l'application compile sans erreur
  - S'assurer que tous les tests passent, demander à l'utilisateur si des questions se posent.

## Notes

- Les tâches marquées `*` sont optionnelles et peuvent être ignorées pour un MVP rapide
- Chaque tâche référence les exigences correspondantes pour la traçabilité
- Les tests de propriété utilisent Jasmine/Karma (Angular) avec génération de données aléatoires
- Aucune modification du backend n'est requise : toutes les API sont déjà disponibles via `RecommendationService`
