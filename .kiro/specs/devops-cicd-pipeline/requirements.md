# Requirements Document

## Introduction

Ce document définit les exigences pour la mise en place d'une infrastructure DevOps pour le
projet **MindCare** (application Alzheimer). Le périmètre est restreint aux deux microservices
Spring Boot suivants : `recommendation_service` et `souvenir_service`, ainsi qu'au frontend
Angular (`front/`). Le travail couvre : les pipelines CI/CD individuels et global, l'analyse de
qualité de code avec SonarQube, le monitoring applicatif, et le déploiement sur un cluster
Kubernetes local (kubeadm sur WSL). Le projet est réalisé individuellement dans le cadre d'un
sprint DevOps universitaire.

---

## Glossaire

- **CI_Pipeline** : Pipeline d'intégration continue GitLab CI exécuté à chaque push sur un microservice ou le frontend.
- **CD_Pipeline** : Pipeline de déploiement continu global qui orchestre le déploiement des deux microservices et du frontend.
- **Recommendation_Service** : Microservice Spring Boot / Maven situé dans `server/recommendation_service/`.
- **Souvenir_Service** : Microservice Spring Boot / Maven situé dans `server/souvenir_service/`.
- **Frontend** : Application Angular 21 située dans le dossier `front/`.
- **Container_Image** : Image Docker produite par un pipeline CI et stockée dans une Registry.
- **Registry** : Dépôt d'images Docker (Docker Hub ou GitLab Container Registry).
- **SonarQube_Server** : Instance SonarQube auto-hébergée via Docker, utilisée pour l'analyse statique du code.
- **Quality_Gate** : Ensemble de règles SonarQube déterminant si un build est acceptable.
- **Monitoring_Stack** : Ensemble des outils de surveillance (Prometheus + Grafana).
- **Kubernetes_Cluster** : Cluster déployé avec kubeadm sur WSL.
- **Manifest** : Fichier YAML Kubernetes décrivant un déploiement, service ou ingress.
- **JaCoCo** : Plugin Maven de couverture de code pour Java.
- **Vitest** : Framework de test unitaire utilisé pour le frontend Angular.

---

## Requirements

### Requirement 1 : Pipeline CI pour Recommendation_Service

**User Story :** En tant que développeur, je veux qu'un pipeline CI soit déclenché automatiquement
pour `recommendation_service` lors d'un push, afin de valider la compilation, les tests et la
construction de l'image Docker de manière isolée.

#### Acceptance Criteria

1. WHEN un commit est poussé sur le dépôt affectant le dossier `server/recommendation_service/`, THE CI_Pipeline SHALL compiler le Recommendation_Service avec Maven (`mvn clean package`).
2. WHEN la compilation du Recommendation_Service réussit, THE CI_Pipeline SHALL exécuter les tests unitaires Maven (`mvn test`) du Recommendation_Service.
3. WHEN les tests unitaires du Recommendation_Service réussissent, THE CI_Pipeline SHALL construire une Container_Image Docker pour le Recommendation_Service.
4. WHEN une Container_Image du Recommendation_Service est construite avec succès, THE CI_Pipeline SHALL pousser la Container_Image vers la Registry avec un tag incluant le nom du service et l'identifiant du commit.
5. IF la compilation du Recommendation_Service échoue, THEN THE CI_Pipeline SHALL marquer le pipeline comme échoué et arrêter les étapes suivantes.
6. IF les tests unitaires du Recommendation_Service échouent, THEN THE CI_Pipeline SHALL marquer le pipeline comme échoué et arrêter la construction de l'image Docker.
7. THE CI_Pipeline du Recommendation_Service SHALL être défini dans un fichier `.gitlab-ci.yml` versionné dans le dépôt.

---

### Requirement 2 : Pipeline CI pour Souvenir_Service

**User Story :** En tant que développeur, je veux qu'un pipeline CI soit déclenché automatiquement
pour `souvenir_service` lors d'un push, afin de valider la compilation, les tests et la
construction de l'image Docker de manière isolée.

#### Acceptance Criteria

1. WHEN un commit est poussé sur le dépôt affectant le dossier `server/souvenir_service/`, THE CI_Pipeline SHALL compiler le Souvenir_Service avec Maven (`mvn clean package`).
2. WHEN la compilation du Souvenir_Service réussit, THE CI_Pipeline SHALL exécuter les tests unitaires Maven (`mvn test`) du Souvenir_Service.
3. WHEN les tests unitaires du Souvenir_Service réussissent, THE CI_Pipeline SHALL construire une Container_Image Docker pour le Souvenir_Service.
4. WHEN une Container_Image du Souvenir_Service est construite avec succès, THE CI_Pipeline SHALL pousser la Container_Image vers la Registry avec un tag incluant le nom du service et l'identifiant du commit.
5. IF la compilation du Souvenir_Service échoue, THEN THE CI_Pipeline SHALL marquer le pipeline comme échoué et arrêter les étapes suivantes.
6. IF les tests unitaires du Souvenir_Service échouent, THEN THE CI_Pipeline SHALL marquer le pipeline comme échoué et arrêter la construction de l'image Docker.
7. THE CI_Pipeline du Souvenir_Service SHALL être défini dans un fichier `.gitlab-ci.yml` versionné dans le dépôt.

---

### Requirement 3 : Pipeline CI dédié pour le Frontend Angular

**User Story :** En tant que développeur, je veux un pipeline CI dédié au frontend Angular qui
valide le build et les tests à chaque push, afin de détecter les régressions côté client.

#### Acceptance Criteria

1. WHEN un commit est poussé sur le dépôt affectant le dossier `front/`, THE CI_Pipeline SHALL installer les dépendances npm (`npm ci`).
2. WHEN les dépendances npm sont installées, THE CI_Pipeline SHALL construire le Frontend en mode production (`ng build --configuration production`).
3. WHEN le build du Frontend réussit, THE CI_Pipeline SHALL exécuter les tests unitaires Vitest (`npx vitest --run`).
4. WHEN les tests unitaires du Frontend réussissent, THE CI_Pipeline SHALL construire une Container_Image Docker pour le Frontend.
5. WHEN une Container_Image du Frontend est construite, THE CI_Pipeline SHALL pousser la Container_Image vers la Registry avec un tag incluant l'identifiant du commit.
6. IF le build du Frontend échoue, THEN THE CI_Pipeline SHALL marquer le pipeline comme échoué et arrêter les étapes suivantes.
7. IF les tests unitaires du Frontend échouent, THEN THE CI_Pipeline SHALL marquer le pipeline comme échoué et arrêter la construction de l'image Docker.

---

### Requirement 4 : Pipeline CD global pour le déploiement des deux microservices et du frontend

**User Story :** En tant que développeur, je veux un pipeline CD global qui déploie le
Recommendation_Service, le Souvenir_Service et le Frontend sur le Kubernetes_Cluster en une
seule exécution, afin de garantir la cohérence des versions déployées.

#### Acceptance Criteria

1. WHEN les CI_Pipelines du Recommendation_Service, du Souvenir_Service et du Frontend ont tous réussi, THE CD_Pipeline SHALL appliquer les Manifests Kubernetes des trois services sur le Kubernetes_Cluster.
2. THE CD_Pipeline SHALL déployer les services dans l'ordre suivant : Recommendation_Service, puis Souvenir_Service, puis Frontend.
3. WHEN un Manifest est appliqué, THE CD_Pipeline SHALL attendre que le déploiement Kubernetes correspondant atteigne l'état `Ready` avant de passer au service suivant.
4. IF l'application d'un Manifest échoue, THEN THE CD_Pipeline SHALL arrêter le déploiement et signaler le service en erreur.
5. THE CD_Pipeline SHALL être déclenché manuellement ou par un tag Git de type release.
6. THE CD_Pipeline SHALL être défini dans un fichier `.gitlab-ci.yml` versionné dans le dépôt.

---

### Requirement 5 : Analyse de qualité de code avec SonarQube

**User Story :** En tant que développeur, je veux que chaque pipeline CI envoie les résultats
d'analyse statique à SonarQube, afin de mesurer et d'améliorer la qualité du code du
Recommendation_Service, du Souvenir_Service et du Frontend.

#### Acceptance Criteria

1. WHEN les tests unitaires du Recommendation_Service réussissent, THE CI_Pipeline SHALL exécuter l'analyse SonarQube via le plugin Maven SonarScanner (`mvn sonar:sonar`) pour le Recommendation_Service.
2. WHEN les tests unitaires du Souvenir_Service réussissent, THE CI_Pipeline SHALL exécuter l'analyse SonarQube via le plugin Maven SonarScanner (`mvn sonar:sonar`) pour le Souvenir_Service.
3. WHEN les tests unitaires du Frontend réussissent, THE CI_Pipeline SHALL exécuter l'analyse SonarQube via SonarScanner CLI sur le dossier `front/`.
4. THE SonarQube_Server SHALL être déployé via Docker Compose et accessible sur le réseau local.
5. WHEN une analyse SonarQube est soumise, THE SonarQube_Server SHALL créer ou mettre à jour un projet SonarQube correspondant au service analysé.
6. THE CI_Pipeline SHALL transmettre les rapports de couverture JaCoCo au SonarQube_Server pour le Recommendation_Service et le Souvenir_Service.
7. THE CI_Pipeline SHALL transmettre les rapports de couverture Vitest au SonarQube_Server pour le Frontend.
8. IF le SonarQube_Server est inaccessible lors de l'analyse, THEN THE CI_Pipeline SHALL signaler un avertissement sans bloquer le pipeline.

---

### Requirement 6 : Monitoring applicatif (Recommendation_Service, Souvenir_Service et Frontend)

**User Story :** En tant que développeur, je veux un système de monitoring qui collecte et
visualise les métriques du Recommendation_Service, du Souvenir_Service et du Frontend, afin
d'observer l'état de santé de l'application en temps réel.

#### Acceptance Criteria

1. THE Monitoring_Stack SHALL collecter les métriques exposées par le Recommendation_Service via l'endpoint `/actuator/prometheus`.
2. THE Monitoring_Stack SHALL collecter les métriques exposées par le Souvenir_Service via l'endpoint `/actuator/prometheus`.
3. THE Monitoring_Stack SHALL collecter les métriques du Frontend (disponibilité, temps de réponse HTTP) via un exporter dédié.
4. WHEN un service expose des métriques, THE Monitoring_Stack SHALL les stocker et les rendre interrogeables pendant au moins 15 jours.
5. THE Monitoring_Stack SHALL fournir un tableau de bord Grafana affichant pour le Recommendation_Service et le Souvenir_Service : le taux de requêtes, le taux d'erreurs et la latence.
6. THE Monitoring_Stack SHALL être déployée via Docker Compose ou Kubernetes Manifests versionnés dans le dépôt.
7. IF le Recommendation_Service ne répond plus à son endpoint de santé (`/actuator/health`), THEN THE Monitoring_Stack SHALL déclencher une alerte visible dans le tableau de bord Grafana.
8. IF le Souvenir_Service ne répond plus à son endpoint de santé (`/actuator/health`), THEN THE Monitoring_Stack SHALL déclencher une alerte visible dans le tableau de bord Grafana.

---

### Requirement 7 : Infrastructure Kubernetes avec kubeadm sur WSL

**User Story :** En tant que développeur, je veux déployer le Recommendation_Service, le
Souvenir_Service et le Frontend sur un cluster Kubernetes local créé avec kubeadm sur WSL,
afin de valider une architecture distribuée proche d'un environnement de production.

#### Acceptance Criteria

1. THE Kubernetes_Cluster SHALL être initialisé avec kubeadm sur WSL et comporter au minimum un nœud master et un nœud worker.
2. THE Kubernetes_Cluster SHALL disposer d'un plugin réseau CNI (ex. Flannel ou Calico) opérationnel permettant la communication inter-pods.
3. THE CD_Pipeline SHALL déployer le Recommendation_Service, le Souvenir_Service et le Frontend via un Manifest Kubernetes dédié (Deployment + Service) pour chacun.
4. THE Kubernetes_Cluster SHALL exposer le Frontend via un Ingress Controller accessible depuis l'hôte WSL.
5. WHEN un pod du Recommendation_Service ou du Souvenir_Service redémarre, THE Kubernetes_Cluster SHALL maintenir la disponibilité du service grâce à un nombre de réplicas supérieur ou égal à 1.
6. THE Kubernetes_Cluster SHALL disposer d'un PersistentVolume pour la base de données MySQL afin de garantir la persistance des données entre les redémarrages de pods.
7. IF un nœud worker devient indisponible, THEN THE Kubernetes_Cluster SHALL replanifier les pods affectés sur les nœuds disponibles.

---

### Requirement 8 : Dockerfiles optimisés pour les deux microservices et le frontend

**User Story :** En tant que développeur, je veux que le Recommendation_Service, le
Souvenir_Service et le Frontend disposent chacun d'un Dockerfile optimisé, afin de produire
des images légères et reproductibles pour les pipelines CI/CD.

#### Acceptance Criteria

1. THE CI_Pipeline SHALL construire chaque Container_Image à partir d'un Dockerfile versionné dans le dossier du service correspondant.
2. THE Dockerfile du Recommendation_Service SHALL utiliser un build multi-étapes (build Maven + image JRE allégée).
3. THE Dockerfile du Souvenir_Service SHALL utiliser un build multi-étapes (build Maven + image JRE allégée).
4. THE Dockerfile du Frontend SHALL utiliser un build multi-étapes (build Angular + serveur Nginx).
5. WHEN une Container_Image est construite, THE CI_Pipeline SHALL vérifier que la taille de l'image ne dépasse pas 500 Mo.
6. THE Dockerfile du Recommendation_Service SHALL définir un utilisateur non-root pour l'exécution du processus applicatif.
7. THE Dockerfile du Souvenir_Service SHALL définir un utilisateur non-root pour l'exécution du processus applicatif.
8. THE Dockerfile du Frontend SHALL définir un utilisateur non-root pour l'exécution du processus applicatif.
