# Guide Postman Debutant - Tester toutes les APIs

Ce guide est fait pour une personne debutante.
Tu peux suivre les etapes une par une, sans connaissance avancee.

---

## 0) Ce que tu dois avoir

- Java installe
- Maven (ou utiliser `mvnw.cmd` fourni dans le projet)
- Postman installe

---

## 1) Demarrer les services (obligatoire)

Va dans le dossier `backend` du projet, puis lance les services dans cet ordre:

1. eureka-server (port 8761)
2. config-server (port 8888)
3. cours-ms (port 8060)
4. inscription-ms (port 8081)
5. feedback-ms (port 8090)
6. reclamation-ms (port 8091)
7. gateway (port 8085)

Conseil debutant:
- Ouvre un terminal par service.
- Dans chaque terminal, lance le service puis laisse le terminal ouvert.

Exemple de commande (dans le dossier de chaque service):

```powershell
..\mvnw.cmd spring-boot:run
```

Verification:
- Ouvre cette page dans ton navigateur: `http://localhost:8761`
- Tu dois voir les microservices enregistres.

---

## 2) Ouvrir Postman et preparer l'environnement

1. Ouvre Postman.
2. Clique sur **Environments**.
3. Clique **Create new**.
4. Nom de l'environnement: `E-Learn Local`.
5. Ajoute cette variable:

- Key: `baseUrl`
- Value: `http://localhost:8085`

6. Clique **Save**.
7. En haut a droite de Postman, selectionne l'environnement `E-Learn Local`.

---

## 3) Regle simple pour chaque requete

Pour chaque requete:

- Choisir la bonne methode: `GET`, `POST`, `PUT`, `DELETE`
- URL correcte (exemple: `{{baseUrl}}/feedbacks`)
- Si `POST` ou `PUT`:
  - Onglet **Body**
  - Choisir **raw**
  - Choisir **JSON**
  - Coller le JSON
- Header auto ou manuel:
  - `Content-Type: application/json`

---

## 4) Tester API Cours

### 4.1 Creer un cours

- Methode: `POST`
- URL: `{{baseUrl}}/cours`
- Body:

```json
{
  "title": "Java Spring Boot",
  "description": "Cours complet",
  "cover": "https://img.com/cover.png",
  "video": "https://video.com/v1",
  "category": "Backend",
  "language": "FR",
  "level": "BEGINNER",
  "price": "99",
  "duration": "10h",
  "instructor": "Amena",
  "status": "PUBLISHED"
}
```

Clique **Send**.

Important:
- Dans la reponse, copie la valeur `id` du cours.
- Garde cette valeur, tu en auras besoin.

### 4.2 Voir tous les cours

- Methode: `GET`
- URL: `{{baseUrl}}/cours`

### 4.3 Voir un cours par id

- Methode: `GET`
- URL: `{{baseUrl}}/cours/1`

Remplace `1` par ton vrai id.

### 4.4 Modifier un cours

- Methode: `PUT`
- URL: `{{baseUrl}}/cours/1`
- Body: meme format JSON que la creation

### 4.5 Supprimer un cours

- Methode: `DELETE`
- URL: `{{baseUrl}}/cours/1`

---

## 5) Tester API Inscriptions

Tu dois avoir un `coursId` valide.

### 5.1 Creer une inscription

- Methode: `POST`
- URL: `{{baseUrl}}/inscriptions`
- Body:

```json
{
  "studentName": "Ali",
  "studentEmail": "ali@mail.com",
  "coursId": 1
}
```

Remplace `1` par ton vrai `coursId`.

### 5.2 Voir toutes les inscriptions

- Methode: `GET`
- URL: `{{baseUrl}}/inscriptions`

### 5.3 Voir inscription par id

- Methode: `GET`
- URL: `{{baseUrl}}/inscriptions/1`

### 5.4 Voir details inscription

- Methode: `GET`
- URL: `{{baseUrl}}/inscriptions/1/details`

### 5.5 Modifier inscription

- Methode: `PUT`
- URL: `{{baseUrl}}/inscriptions/1`

### 5.6 Supprimer inscription

- Methode: `DELETE`
- URL: `{{baseUrl}}/inscriptions/1`

---

## 6) Tester API Feedbacks

Attention au nom de route correct: `feedbacks` (pas `feddbacks`).

### 6.1 Creer feedback

- Methode: `POST`
- URL: `{{baseUrl}}/feedbacks`
- Body:

```json
{
  "userName": "Sara",
  "userEmail": "sara@mail.com",
  "title": "Tres bien",
  "imageUrl": "https://img.com/fb.png",
  "rating": 5,
  "comment": "Excellent cours"
}
```

### 6.2 Voir tous les feedbacks

- Methode: `GET`
- URL: `{{baseUrl}}/feedbacks`

### 6.3 Voir feedback par id

- Methode: `GET`
- URL: `{{baseUrl}}/feedbacks/1`

### 6.4 Modifier feedback

- Methode: `PUT`
- URL: `{{baseUrl}}/feedbacks/1`

### 6.5 Supprimer feedback

- Methode: `DELETE`
- URL: `{{baseUrl}}/feedbacks/1`

---

## 7) Tester API Reclamations

### 7.1 Creer reclamation

- Methode: `POST`
- URL: `{{baseUrl}}/reclamations`
- Body:

```json
{
  "userName": "Karim",
  "userEmail": "karim@mail.com",
  "subject": "Probleme video",
  "imageUrl": "https://img.com/rec.png",
  "description": "La video ne demarre pas",
  "status": "OPEN"
}
```

### 7.2 Voir toutes les reclamations

- Methode: `GET`
- URL: `{{baseUrl}}/reclamations`

### 7.3 Voir reclamation par id

- Methode: `GET`
- URL: `{{baseUrl}}/reclamations/1`

### 7.4 Modifier reclamation

- Methode: `PUT`
- URL: `{{baseUrl}}/reclamations/1`

### 7.5 Supprimer reclamation

- Methode: `DELETE`
- URL: `{{baseUrl}}/reclamations/1`

---

## 8) Si tu as une erreur

Checklist simple:

1. Tous les services sont bien lances.
2. URL correcte (ex: `feedbacks`, pas `feddbacks`).
3. Bonne methode HTTP.
4. Body en JSON pour `POST` et `PUT`.
5. `Content-Type: application/json`.
6. L'id existe vraiment.
7. Le gateway tourne sur `8085`.

---

## 9) Version sans gateway (optionnel)

Tu peux aussi tester directement chaque microservice:

- Cours: `http://localhost:8060/cours`
- Inscriptions: `http://localhost:8081/inscriptions`
- Feedbacks: `http://localhost:8090/feedbacks`
- Reclamations: `http://localhost:8091/reclamations`

Mais pour debuter, garde la methode via gateway:

- `http://localhost:8085`

---

## 10) Tester les APIs du Gateway

Le gateway est sur le port `8085`.

### 10.1 Verifier que gateway est vivant

- Methode: `GET`
- URL: `http://localhost:8085/actuator/health`
- Resultat attendu: status `UP`

### 10.2 Verifier infos gateway

- Methode: `GET`
- URL: `http://localhost:8085/actuator/info`

### 10.3 Tester le routage via gateway

Exemples:

- `GET http://localhost:8085/cours`
- `GET http://localhost:8085/inscriptions`
- `GET http://localhost:8085/feedbacks`
- `GET http://localhost:8085/reclamations`

Si ces URLs fonctionnent, le gateway route correctement vers les microservices.

---

## 11) Tester les APIs Eureka

Eureka est sur le port `8761`.

### 11.1 Verifier UI Eureka

- Methode: `GET`
- URL: `http://localhost:8761/`
- Resultat attendu: page web Eureka

### 11.2 Verifier sante Eureka

- Methode: `GET`
- URL: `http://localhost:8761/actuator/health`

### 11.3 Lister les services enregistres

- Methode: `GET`
- URL: `http://localhost:8761/eureka/apps`
- Header conseille: `Accept: application/json`

Tu dois voir les noms des apps (exemple: `gateway`, `cours-ms`, `feedback-ms`, etc.).

---

## 12) Tester les APIs Config Server

Config Server est sur le port `8888`.

### 12.1 Verifier sante Config Server

- Methode: `GET`
- URL: `http://localhost:8888/actuator/health`

### 12.2 Verifier infos Config Server

- Methode: `GET`
- URL: `http://localhost:8888/actuator/info`

### 12.3 Lire la configuration commune

- Methode: `GET`
- URL: `http://localhost:8888/application/default`

Resultat attendu:
- un JSON avec `propertySources`
- les proprietes globales (datasource, h2, jpa, etc.)

### 12.4 Tester une app specifique (optionnel)

Tu peux essayer aussi:

- `GET http://localhost:8888/gateway/default`
- `GET http://localhost:8888/cours-ms/default`

Si tu as une reponse vide ou erreur, ce n'est pas grave: cela depend des fichiers de config disponibles.
