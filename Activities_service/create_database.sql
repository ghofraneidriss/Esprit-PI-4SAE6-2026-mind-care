-- Créer la base de données pour le service Activities
CREATE DATABASE IF NOT EXISTS alzheimer_activities 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Utiliser la base de données
USE alzheimer_activities;

-- Créer la table quiz
CREATE TABLE IF NOT EXISTS quiz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    theme VARCHAR(50) NOT NULL,
    description TEXT,
    difficulty VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_theme (theme),
    INDEX idx_difficulty (difficulty)
);

-- Créer la table question
CREATE TABLE IF NOT EXISTS question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    correct_answer VARCHAR(1) NOT NULL,
    FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE,
    INDEX idx_quiz_id (quiz_id)
);

-- Créer la table patient_answer
CREATE TABLE IF NOT EXISTS patient_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer VARCHAR(1) NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_patient_id (patient_id),
    INDEX idx_quiz_id (quiz_id),
    INDEX idx_question_id (question_id),
    INDEX idx_patient_quiz (patient_id, quiz_id)
);

-- Insérer des données de test
INSERT INTO quiz (title, theme, description, difficulty) VALUES
('Test de Mémoire à Court Terme', 'MEMORY', 'Test de mémoire pour détecter les premiers signes d''Alzheimer', 'EASY'),
('Test d''Attention et Concentration', 'ATTENTION', 'Test d''attention pour évaluer les capacités cognitives', 'MEDIUM'),
('Test d''Orientation Spatiale', 'ORIENTATION', 'Test d''orientation pour évaluer la perception spatiale', 'EASY');

-- Insérer des questions de test
INSERT INTO question (quiz_id, question_text, option_a, option_b, option_c, correct_answer) VALUES
(1, 'Quelle est la date d''aujourd''hui ?', '17/02/2026', '16/02/2026', '15/02/2026', 'A'),
(1, 'Rappelez-vous de ces 3 mots: Chat, Arbre, Livre. Quel est le premier mot ?', 'Chat', 'Arbre', 'Livre', 'A'),
(2, 'Comptez de 100 à l''envers par dizaines. Quel est le troisième nombre ?', '80', '70', '60', 'B'),
(2, 'Épelez le mot MONDE à l''envers', 'EDNOM', 'EDMON', 'MODNE', 'A'),
(3, 'Où êtes-vous actuellement ?', 'Chez vous', 'À l''hôpital', 'Dans un magasin', 'B');
