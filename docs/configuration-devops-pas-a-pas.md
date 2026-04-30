# Configuration DevOps pas a pas - Khaoula

Objectif principal:
- Faire passer les pipelines Jenkins.
- Voir les tests unitaires dans Jenkins.
- Voir la qualite et la couverture dans SonarQube.
- Lancer MySQL, les deux microservices, Prometheus et Grafana avec Docker.

Important securite:
- Si un token GitHub ou SonarQube a ete visible dans une capture, il faut le supprimer et en creer un nouveau.

## 1. Ouvrir WSL et aller dans le projet

```bash
cd /mnt/c/Users/khaou/OneDrive/Bureau/mind-care/mind-care/mind-care
git checkout khaoula-integration-globale
git status
```

## 2. Sauvegarder les corrections dans GitHub

Jenkins lit les fichiers depuis GitHub. Si les corrections restent seulement sur ton PC, Jenkins ne les verra pas.

```bash
git add .
git commit -m "Ajout CI CD Sonar monitoring Khaoula"
git push origin khaoula-integration-globale
```

Si Git demande ton identite:

```bash
git config --global user.name "Khaoula Helali"
git config --global user.email "ton-email@example.com"
```

## 3. Preparer Docker Desktop avec WSL

Dans Docker Desktop:
- Settings
- Resources
- WSL Integration
- Activer ton Ubuntu/WSL
- Apply & Restart

Dans WSL, verifier:

```bash
docker version
docker compose version
```

Si ces commandes fonctionnent, Docker est bien connecte a WSL.

## 4. Preparer SonarQube sous WSL

SonarQube peut refuser de demarrer si `vm.max_map_count` est trop bas.

Dans WSL:

```bash
sudo sysctl -w vm.max_map_count=262144
```

## 5. Nettoyer les anciens conteneurs Jenkins/Sonar

Si tu as deja lance Jenkins ou Sonar a la main, les noms peuvent bloquer.

```bash
docker rm -f jenkins sonarqube sonar-db
```

Cette commande supprime seulement les conteneurs, pas ton code.

## 6. Lancer Jenkins + SonarQube

Depuis la racine du projet:

```bash
docker compose -f devops/docker-compose.devops.yml up -d --build
```

Verifier:

```bash
docker ps
```

Ouvrir:
- Jenkins: `http://localhost:8080`
- SonarQube: `http://localhost:9000`

Mot de passe initial Jenkins:

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

## 7. Installer les plugins Jenkins

Dans Jenkins:

`Manage Jenkins > Plugins > Available plugins`

Installer:
- Git
- Pipeline
- JUnit
- SonarQube Scanner
- Docker Pipeline

Redemarrer Jenkins si demande.

## 8. Configurer GitHub dans Jenkins

Dans Jenkins:

`Manage Jenkins > Credentials > System > Global credentials > Add Credentials`

Choisir:
- Kind: `Username with password`
- Username: ton username GitHub
- Password: ton nouveau token GitHub
- ID: `github-credentials`

## 9. Configurer SonarQube

Dans SonarQube:

1. Ouvrir `http://localhost:9000`
2. Login par defaut: `admin` / `admin`
3. Changer le mot de passe
4. Aller dans `My Account > Security`
5. Creer un token

Dans Jenkins:

`Manage Jenkins > Credentials > System > Global credentials > Add Credentials`

Choisir:
- Kind: `Secret text`
- Secret: ton token SonarQube
- ID: `sonarqube-token`

Puis:

`Manage Jenkins > System > SonarQube servers`

Ajouter:
- Name: `SonarQube`
- Server URL: `http://sonarqube:9000`
- Server authentication token: `sonarqube-token`

Le nom doit etre exactement `SonarQube`.

## 10. Creer le pipeline principal

Dans Jenkins:

1. `New Item`
2. Nom: `MindCare-Khaoula`
3. Type: `Pipeline`
4. Dans `Pipeline`, choisir `Pipeline script from SCM`
5. SCM: `Git`
6. Repository URL:

```text
https://github.com/ghofraneidriss/Esprit-PI-4SAE6-2026-mind-care.git
```

7. Credentials: `github-credentials`
8. Branch:

```text
*/khaoula-integration-globale
```

9. Script Path:

```text
Jenkinsfile
```

10. Save

## 11. Configurer Docker Hub dans Jenkins

Ton username Docker Hub:

```text
121999121999
```

Dans Docker Hub, creer un Access Token:

```text
Account Settings > Personal access tokens > Generate new token
```

Dans Jenkins:

`Manage Jenkins > Credentials > System > Global credentials > Add Credentials`

Choisir:
- Kind: `Username with password`
- Username: `121999121999`
- Password: ton token Docker Hub
- ID: `dockerhub-credentials`

Les images poussees seront:

```text
121999121999/mindcare-ordonnance:latest
121999121999/mindcare-traitement:latest
```

## 12. Premier lancement du pipeline

Cliquer:

```text
Build with Parameters
```

Laisser:

```text
RUN_DOCKER_CD = false
```

Ce premier lancement fait:
- tests du microservice ordonnance/medicaments
- tests du microservice traitement/consultation
- generation JaCoCo
- analyse SonarQube

Ne coche pas Docker au debut. On valide d'abord CI + tests + Sonar.

## 13. Voir les tests dans Jenkins

Apres le build:
- Ouvrir le build
- `Console Output`
- `Test Result`
- `Artifacts`

Resultats attendus:
- Ordonnance/medicaments: 10 tests
- Traitement/consultation: 3 tests

## 14. Voir SonarQube

Ouvrir:

```text
http://localhost:9000/projects
```

Tu dois voir:
- `MindCare Ordonnance Medicaments`
- `MindCare Traitement Consultation`

Captures a prendre:
- Overview
- Issues
- Coverage
- Code smells
- Security hotspots

## 15. Lancer MySQL + backend + Prometheus + Grafana

Quand Jenkins + Sonar marche:

```bash
docker compose up -d --build
```

Verifier:

```bash
docker ps
```

Services:
- MySQL: `localhost:3306`
- Traitement/consultation: `http://localhost:8081`
- Ordonnance/medicaments: `http://localhost:8083`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Grafana:
- user: `admin`
- password: `admin`

## 16. Verifier MySQL

Entrer dans MySQL:

```bash
docker exec -it mindcare-mysql mysql -uroot
```

Commandes MySQL:

```sql
SHOW DATABASES;
USE alzheimer_db;
SHOW TABLES;
EXIT;
```

## 17. Verifier Prometheus

Ouvrir:

```text
http://localhost:9090/targets
```

Les targets doivent etre `UP`:
- `mindcare-traitement-consultation`
- `mindcare-ordonnance-medicaments`

Si une target est `DOWN`, verifier:

```bash
docker logs mindcare-traitement
docker logs mindcare-ordonnance
docker logs mindcare-prometheus
```

## 18. Verifier Grafana

Ouvrir:

```text
http://localhost:3000
```

Login:

```text
admin / admin
```

Verifier:
- Connections
- Data sources
- Prometheus doit etre configure automatiquement

Tu peux creer un dashboard avec des requetes:

```text
up
http_server_requests_seconds_count
jvm_memory_used_bytes
process_cpu_usage
```

## 19. Activer le CD Docker dans Jenkins

Quand CI + Sonar + Docker local marchent:

Relancer le pipeline Jenkins avec:

```text
RUN_DOCKER_CD = true
```

Cela construit les images Docker et lance le deploiement global:

```bash
docker compose -f docker-compose.yml up -d --build mysql ordonnance-service traitement-service prometheus grafana
```

Le pipeline pousse aussi les images vers Docker Hub:

```text
121999121999/mindcare-ordonnance:latest
121999121999/mindcare-traitement:latest
```

## 20. Pipelines individuels

Tu peux aussi creer deux jobs Jenkins separes:

Ordonnance:

```text
devops/jenkins/Jenkinsfile.ordonnance-et-medicaments
```

Traitement:

```text
devops/jenkins/Jenkinsfile.traitement-et-consultation
```

Premier lancement:

```text
RUN_DOCKER_BUILD = false
```

Quand Docker est pret:

```text
RUN_DOCKER_BUILD = true
```

## 21. Phrase pour la presentation

Ma partie individuelle couvre les microservices ordonnance/medicaments et traitement/consultation. J'ai mis en place deux pipelines CI avec tests unitaires, couverture JaCoCo et analyse SonarQube. J'ai ajoute un pipeline CD pour construire les images Docker et deployer les services avec Docker Compose. Pour l'excellence, j'ai ajoute le monitoring avec Spring Boot Actuator, Prometheus et Grafana, ainsi qu'une configuration Kubernetes kubeadm.
