================================================================================
  MindCare — Backend (multi-modules Maven)
================================================================================

RACINE (ce dossier)
  pom.xml              Parent Spring Boot + BOM Spring Cloud
  ports.env.example    Reference des ports HTTP
  README.txt           Ce fichier

MODULES (un dossier = un microservice — ne pas ajouter de src/ ici)
  eureka_server        1) Registre Eureka — demarrer en premier (port 8761)
  users_service        2) Utilisateurs / auth — DB : alzheimer_db — 8081
  forums_service       3) Forum — DB : forum_db — 8082
  incident_service     4) Incidents — DB : incident_db — 8083
  activities_service   5) Activites (quiz, photo, rapports) — DB : activities_db — 8084
  api_gateway          6) Point d'entree /api pour Angular — demarrer apres Eureka + services — 8080

Demarrage : MySQL (3306) puis Eureka puis users/forums/incident/activities puis Gateway puis Angular.

Ports (inchangés — ne pas chevaucher) :
  8761 Eureka | 8080 API Gateway | 8081 users | 8082 forums | 8083 incident | 8084 activities
  Front ng serve : proxy /api/users -> 8081 direct ; autre /api -> 8080 (gateway)
  Detail : ports.env.example

Chaque module : src/main/java + src/main/resources + pom.xml

Script utile (depuis la racine du repo Git) : start-all.sh  (WSL/Linux)

================================================================================
