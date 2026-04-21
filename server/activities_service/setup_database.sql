-- Créer la base de données alzheimer_activities
CREATE DATABASE IF NOT EXISTS alzheimer_activities 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Utiliser la base de données
USE alzheimer_activities;

-- Créer la table quiz_activity
CREATE TABLE IF NOT EXISTS quiz_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    theme VARCHAR(50) NOT NULL,
    description TEXT,
    difficulty VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_theme (theme),
    INDEX idx_difficulty (difficulty)
);

-- Créer la table photo_activity
CREATE TABLE IF NOT EXISTS photo_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_difficulty (difficulty)
);

-- Créer la table game_result
CREATE TABLE IF NOT EXISTS game_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_id BIGINT NOT NULL,
    score INT DEFAULT 0,
    max_score INT DEFAULT 100,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    time_spent_seconds INT DEFAULT 0,
    INDEX idx_patient_id (patient_id),
    INDEX idx_activity (activity_type, activity_id),
    INDEX idx_completed_at (completed_at)
);

-- Insérer des données de test
INSERT INTO quiz_activity (title, theme, description, difficulty) VALUES
('Test de Mémoire à Court Terme', 'MEMORY', 'Test de mémoire pour détecter les premiers signes d''Alzheimer', 'EASY'),
('Test d''Attention et Concentration', 'ATTENTION', 'Test d''attention pour évaluer les capacités cognitives', 'MEDIUM'),
('Test d''Orientation Spatiale', 'ORIENTATION', 'Test d''orientation pour évaluer la perception spatiale', 'EASY'),
('Test de Langage et Communication', 'LANGUAGE', 'Test de langage pour évaluer les capacités de communication', 'MEDIUM'),
('Test des Fonctions Exécutives', 'EXECUTIVE_FUNCTION', 'Test des fonctions exécutives pour la planification', 'HARD');

INSERT INTO photo_activity (title, description, image_url, difficulty) VALUES
('Reconnaissance d''objets familiers', 'Identifiez les objets du quotidien', '/images/everyday_objects.jpg', 'EASY'),
('Reconnaissance de visages', 'Identifiez les membres de la famille', '/images/family_faces.jpg', 'MEDIUM'),
('Reconnaissance d''animaux', 'Identifiez les animaux domestiques', '/images/domestic_animals.jpg', 'EASY'),
('Reconnaissance de couleurs', 'Identifiez les couleurs primaires', '/images/primary_colors.jpg', 'EASY'),
('Reconnaissance de formes', 'Identifiez les formes géométriques', '/images/geometric_shapes.jpg', 'MEDIUM');
