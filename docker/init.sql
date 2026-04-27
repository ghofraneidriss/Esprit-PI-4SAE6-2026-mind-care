-- ================================================
-- MindCare - Initialisation des bases de données
-- Exécuté automatiquement au premier démarrage
-- du conteneur MySQL
-- ================================================

-- Base de données du service Forums
CREATE DATABASE IF NOT EXISTS forum_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Base de données du service Incidents
CREATE DATABASE IF NOT EXISTS incident_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Base de données du service Utilisateurs
CREATE DATABASE IF NOT EXISTS alzheimer_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Base de données du service Recommendation
CREATE DATABASE IF NOT EXISTS recommendation_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Base de données du service Souvenir
CREATE DATABASE IF NOT EXISTS souvenir_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Accorder tous les droits à root depuis n'importe quelle adresse
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;
