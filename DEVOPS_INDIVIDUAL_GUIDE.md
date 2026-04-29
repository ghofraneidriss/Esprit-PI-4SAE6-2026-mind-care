# DevOps Individual Part

Ce livrable ajoute une partie individuelle orientee DevSecOps et monitoring autour des deux microservices backend situes sous `server/`.

## Nouvel outil DevOps choisi

`Trivy` a ete integre dans les pipelines Jenkins de :

- `server/recommendation_service/recommendation_service/Jenkinsfile`
- `server/souvenir_service/souvenir_service/Jenkinsfile`

Objectif :

- scanner les images Docker construites
- detecter les vulnerabilites, secrets et mauvaises configurations
- archiver un rapport Jenkins exploitable pendant le coaching

Le stage ajoute est `Trivy Security Scan`.

## Monitoring ajoute

Le fichier `docker-compose.infra.yml` est maintenant aligne avec de vrais fichiers dans `monitoring/` :

- `Prometheus`
- `Grafana`
- `Alertmanager`
- `cAdvisor`
- `blackbox-exporter`

Les microservices `recommendation_service` et `souvenir_service` exposent desormais :

- `/actuator/health`
- `/actuator/prometheus`

## Lancement

Infrastructure DevOps :

```bash
docker compose -f docker-compose.infra.yml up -d
```

Verification rapide :

```bash
docker ps
```

URLs utiles :

- Jenkins : `http://localhost:8080`
- SonarQube : `http://localhost:9000`
- Prometheus : `http://localhost:9090`
- Grafana : `http://localhost:3000`
- Alertmanager : `http://localhost:9093`

Connexion Grafana par defaut :

- utilisateur : `admin`
- mot de passe : `admin`

## Ce qu'il faut montrer pendant le coaching

1. Capture SonarQube avant refactoring
2. Pipelines Jenkins backend avec :
   - build
   - tests unitaires
   - JaCoCo
   - SonarQube
   - build Docker
   - Trivy scan
3. Rapport Trivy archive dans Jenkins
4. Dashboard Grafana `MindCare DevOps Overview`
5. Alertes visibles dans Prometheus/Alertmanager

## Interprétation de la couverture SonarQube

Le coverage affiche dans SonarQube est le coverage global du projet, pas seulement celui de la classe testee.

Donc :

- `souvenir_service` peut avoir un meilleur pourcentage avec peu de tests si le projet est plus petit
- `recommendation_service` peut rester bas si beaucoup de classes ne sont pas encore couvertes

## Webhook

Un webhook est un appel HTTP automatique entre outils.

Exemples dans ce sprint :

- GitHub vers Jenkins pour declencher un pipeline automatiquement apres un `push`
- SonarQube vers Jenkins pour remonter le resultat du Quality Gate
