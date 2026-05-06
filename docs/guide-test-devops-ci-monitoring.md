# Guide de test DevOps CI et monitoring

Perimetre:
- `backoffice/traitement_et_consultation`
- `backoffice/ordonnance_et_medicaments`
- Jenkins, SonarQube, Prometheus, Grafana
- Projet Docker Compose unique: `devops`

## 1. Verifier la branche et le repo

```bash
git status --short --branch
git remote -v
```

Resultat attendu:
- branche `khaoula-integration-globale`
- working tree propre ou seulement vos changements en cours
- le job Jenkins doit pointer vers le repo GitHub du projet et cette branche.

## 2. Demarrer toute la stack DevOps

```bash
docker compose -f docker-compose.yml -f devops/docker-compose.devops.yml up -d --build
```

Resultat attendu:

```text
jenkins
sonarqube
sonar-db
mindcare-mysql
mindcare-traitement
mindcare-ordonnance
mindcare-prometheus
mindcare-grafana
```

Verifier que tout est dans un seul projet:

```bash
docker compose ls
docker compose ps
```

Resultat attendu:
- un seul projet Compose: `devops`
- `mindcare-mysql`, `mindcare-traitement` et `mindcare-ordonnance` en `healthy`.

## 3. Tester les microservices

Depuis la machine:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health
```

Depuis le container Jenkins, comme dans la pipeline:

```bash
docker exec jenkins sh -c "curl -fsS http://traitement-service:8081/actuator/health"
docker exec jenkins sh -c "curl -fsS http://ordonnance-service:8083/actuator/health"
```

Resultat attendu:

```json
{"status":"UP"}
```

## 4. Tester Prometheus

Interface:

```text
http://localhost:9090/targets
```

Targets attendues:
- `mindcare-traitement-consultation` -> `UP`
- `mindcare-ordonnance-medicaments` -> `UP`

Verification CLI:

```bash
curl "http://localhost:9090/api/v1/targets"
```

Requetes PromQL utiles:

```promql
up{job=~"mindcare-.*"}
process_cpu_usage{job=~"mindcare-.*"}
sum by (job) (jvm_memory_used_bytes{job=~"mindcare-.*", area="heap"})
sum by (job) (rate(http_server_requests_seconds_count{job=~"mindcare-.*"}[5m]))
```

## 5. Tester Grafana

Interface:

```text
http://localhost:3000
```

Identifiants locaux:
- username: `admin`
- password: `admin`

Verifier:
- Datasource `Prometheus`
- Dossier `MindCare`
- Dashboard `MindCare microservices`

Le dashboard doit afficher:
- disponibilite des deux services
- CPU process
- JVM heap
- taux de requetes HTTP
- latence HTTP moyenne
- connexions HikariCP.

## 6. Tester Jenkins

Interface:

```text
http://localhost:8080
```

Pre-requis Jenkins:
- plugin Pipeline
- plugin Git
- plugin JUnit
- plugin SonarQube Scanner for Jenkins
- credentials Docker Hub avec id `dockerhub-credentials`
- SonarQube configure dans Jenkins avec le nom `SonarQube`

Verifier Docker depuis Jenkins:

```bash
docker exec jenkins sh -c "docker --version && docker compose version && docker ps"
```

Resultat attendu:
- Docker CLI disponible
- Docker Compose disponible
- Jenkins voit les containers du projet `devops`.

## 7. Lancer la pipeline CI

Dans Jenkins:
1. Creer ou ouvrir le job Pipeline.
2. Branch: `khaoula-integration-globale`.
3. Jenkinsfile: `Jenkinsfile`.
4. Lancer avec `RUN_DOCKER_CD=false`.

Stages attendus:
- Checkout
- Runtime Jenkins
- CI ordonnance et medicaments
- CI traitement et consultation
- SonarQube backend

Resultat attendu:
- tests Maven OK
- rapports JUnit archives
- rapports JaCoCo archives
- analyses SonarQube visibles.

## 8. Lancer la pipeline CI/CD

Relancer la meme pipeline avec:

```text
RUN_DOCKER_CD=true
```

Stages supplementaires attendus:
- Build Docker images
- Push Docker Hub images
- CD backend global
- Smoke tests backend

Resultat attendu:
- images poussees:
  - `121999121999/mindcare-traitement:latest`
  - `121999121999/mindcare-ordonnance:latest`
- containers redeployes dans le projet `devops`
- smoke tests OK:
  - `traitement-service:8081/actuator/health`
  - `ordonnance-service:8083/actuator/health`
  - `prometheus:9090/-/ready`
  - `grafana:3000/api/health`

## 9. Commandes de diagnostic

Logs microservices:

```bash
docker logs --tail 100 mindcare-traitement
docker logs --tail 100 mindcare-ordonnance
```

Etat Compose:

```bash
docker compose ps
docker compose logs --tail 80 prometheus
docker compose logs --tail 80 grafana
```

Rebuild propre sans supprimer les volumes:

```bash
docker compose -f docker-compose.yml -f devops/docker-compose.devops.yml up -d --build
```

Arret sans supprimer les donnees:

```bash
docker compose -f docker-compose.yml -f devops/docker-compose.devops.yml down
```

Important: ne pas utiliser `-v` si vous voulez garder les volumes Jenkins, SonarQube, MySQL et Grafana.
