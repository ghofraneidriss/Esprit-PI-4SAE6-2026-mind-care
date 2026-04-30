# DevOps Sprint 3 - Partie Khaoula

Perimetre individuel:
- Microservice `ordonnance_et_medicaments`
- Microservice `traitement_et_consultation`
- Suivi frontend ajoute dans un Jenkinsfile dedie pour respecter la consigne groupe

## Pipelines CI/CD

Pipelines ajoutes:
- `devops/jenkins/Jenkinsfile.ordonnance-et-medicaments`: CI du microservice ordonnance/medicaments.
- `devops/jenkins/Jenkinsfile.traitement-et-consultation`: CI du microservice traitement/consultation.
- `devops/jenkins/Jenkinsfile.backend-cd`: CD global backend avec `docker compose`.
- `front/Jenkinsfile`: pipeline frontend dedie.
- `Jenkinsfile`: pipeline racine pour la branche `khaoula-integration-globale`.

Chaque CI backend execute:
- checkout
- tests unitaires
- generation JaCoCo
- analyse SonarQube
- construction image Docker

Le CD backend regroupe les deux microservices avec MySQL, Prometheus et Grafana:

```bash
docker compose up -d --build mysql traitement-service ordonnance-service prometheus grafana
```

## Tests et couverture

Les tests backend utilisent H2 en memoire via:
- `backoffice/ordonnance_et_medicaments/src/test/resources/application.properties`
- `backoffice/traitement_et_consultation/src/test/resources/application.properties`

Cela permet aux pipelines de passer sans base MySQL locale.

Couverture:
- JaCoCo genere `target/site/jacoco/jacoco.xml`
- SonarQube lit ce fichier avec `sonar.coverage.jacoco.xmlReportPaths`

## SonarQube

Fichiers de reference:
- `devops/sonarqube/sonar-ordonnance.properties`
- `devops/sonarqube/sonar-traitement-consultation.properties`

Ameliorations appliquees avant/apres:
- suppression des identifiants mail en dur dans `application.properties`
- externalisation des variables sensibles par variables d'environnement
- ajout de tests unitaires sur la logique consultation
- activation de JaCoCo pour remonter la couverture

Captures a presenter:
1. Analyse SonarQube avant correction.
2. Analyse SonarQube apres correction.
3. Evolution des issues, duplications, security hotspots et coverage.

## Monitoring

Les deux microservices exposent:
- `/actuator/health`
- `/actuator/prometheus`
- `/actuator/metrics`

Stack locale:
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin` / `admin`)

Prometheus scrape:
- `traitement-service:8081/actuator/prometheus`
- `ordonnance-service:8083/actuator/prometheus`

## Kubernetes kubeadm

Manifests:
- `k8s/khaoula/00-namespace.yaml`
- `k8s/khaoula/01-configmap.yaml`
- `k8s/khaoula/02-secret.yaml`
- `k8s/khaoula/03-mysql.yaml`
- `k8s/khaoula/04-traitement-consultation.yaml`
- `k8s/khaoula/05-ordonnance-medicaments.yaml`
- `k8s/khaoula/06-prometheus.yaml`
- `k8s/khaoula/07-grafana.yaml`

Deploiement:

```bash
kubectl apply -f k8s/khaoula
kubectl get pods -n mindcare
kubectl get svc -n mindcare
```

Acces NodePort:
- Prometheus: `http://<node-ip>:30090`
- Grafana: `http://<node-ip>:30300`

Avant Kubernetes, construire les images sur le noeud kubeadm ou les pousser dans un registry:

```bash
docker build -f backoffice/traitement_et_consultation/Dockerfile -t mindcare-traitement:latest backoffice
docker build -f backoffice/ordonnance_et_medicaments/Dockerfile -t mindcare-ordonnance:latest backoffice
```

Avec Docker Hub:

```bash
docker build -f backoffice/traitement_et_consultation/Dockerfile -t 121999121999/mindcare-traitement:latest backoffice
docker build -f backoffice/ordonnance_et_medicaments/Dockerfile -t 121999121999/mindcare-ordonnance:latest backoffice
docker push 121999121999/mindcare-traitement:latest
docker push 121999121999/mindcare-ordonnance:latest
```

## Partie excellence

Element propose:
- Monitoring applicatif complet avec Spring Boot Actuator, Micrometer, Prometheus et Grafana.
- Probes Kubernetes readiness/liveness sur `/actuator/health`.
- Externalisation des secrets via variables d'environnement et Kubernetes Secret.
