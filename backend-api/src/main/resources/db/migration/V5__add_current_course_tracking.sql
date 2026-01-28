-- Add current course tracking for students
ALTER TABLE users ADD COLUMN current_course_id INTEGER DEFAULT 1;

-- Add index for quick lookups
CREATE INDEX idx_user_current_course ON users(current_course_id);

-- Create programme_courses table to store the static programme
CREATE TABLE programme_courses (
    id SERIAL PRIMARY KEY,
    level_code VARCHAR(1) NOT NULL,
    level_name VARCHAR(50) NOT NULL,
    course_order INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create programme_lessons table for sub-lessons
CREATE TABLE programme_lessons (
    id SERIAL PRIMARY KEY,
    course_id INTEGER NOT NULL REFERENCES programme_courses(id),
    lesson_order INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL
);

-- Insert Level A courses (Debutant)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('A', 'Debutant', 1, 'L''echiquier et les pieces'),
('A', 'Debutant', 2, 'Le deplacement du Pion'),
('A', 'Debutant', 3, 'Le deplacement de la Tour'),
('A', 'Debutant', 4, 'Le deplacement du Fou'),
('A', 'Debutant', 5, 'Le deplacement de la Dame'),
('A', 'Debutant', 6, 'Le deplacement du Cavalier'),
('A', 'Debutant', 7, 'Le deplacement du Roi'),
('A', 'Debutant', 8, 'L''echec et l''echec et mat'),
('A', 'Debutant', 9, 'Le Roque'),
('A', 'Debutant', 10, 'Le Pat et les nulles'),
('A', 'Debutant', 11, 'Principes d''ouverture (1)'),
('A', 'Debutant', 12, 'Principes d''ouverture (2)'),
('A', 'Debutant', 13, 'Les mats elementaires'),
('A', 'Debutant', 14, 'Introduction aux tactiques'),
('A', 'Debutant', 15, 'La fourchette'),
('A', 'Debutant', 16, 'Le clouage'),
('A', 'Debutant', 17, 'L''enfilade'),
('A', 'Debutant', 18, 'Finale Roi + Dame vs Roi'),
('A', 'Debutant', 19, 'Finale Roi + Tour vs Roi'),
('A', 'Debutant', 20, 'Revision et evaluation Niveau A');

-- Insert Level B courses (Intermediaire)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('B', 'Intermediaire', 1, 'Tactique : L''attaque a la decouverte'),
('B', 'Intermediaire', 2, 'Tactique : Le sacrifice'),
('B', 'Intermediaire', 3, 'Tactique : La deviation'),
('B', 'Intermediaire', 4, 'Tactique : L''attraction'),
('B', 'Intermediaire', 5, 'Tactique : L''elimination du defenseur'),
('B', 'Intermediaire', 6, 'Tactique : L''interference'),
('B', 'Intermediaire', 7, 'Combinaisons tactiques'),
('B', 'Intermediaire', 8, 'Les ouvertures : 1.e4 e5'),
('B', 'Intermediaire', 9, 'Les ouvertures : 1.d4 d5'),
('B', 'Intermediaire', 10, 'Les ouvertures : Defense Sicilienne'),
('B', 'Intermediaire', 11, 'Les ouvertures : Defense Francaise'),
('B', 'Intermediaire', 12, 'Les ouvertures : Defense Caro-Kann'),
('B', 'Intermediaire', 13, 'Strategie : Les structures de pions'),
('B', 'Intermediaire', 14, 'Strategie : Les colonnes ouvertes'),
('B', 'Intermediaire', 15, 'Strategie : Les cases faibles'),
('B', 'Intermediaire', 16, 'Strategie : Le centre'),
('B', 'Intermediaire', 17, 'Finales de pions (1)'),
('B', 'Intermediaire', 18, 'Finales de pions (2)'),
('B', 'Intermediaire', 19, 'Finales de Tours (1)'),
('B', 'Intermediaire', 20, 'Revision et evaluation Niveau B');

-- Insert Level C courses (Avance)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('C', 'Avance', 1, 'Tactique avancee : Calcul profond'),
('C', 'Avance', 2, 'Tactique avancee : Combinaisons complexes'),
('C', 'Avance', 3, 'Attaque sur le Roi'),
('C', 'Avance', 4, 'La defense active'),
('C', 'Avance', 5, 'Repertoire d''ouvertures blancs (1)'),
('C', 'Avance', 6, 'Repertoire d''ouvertures blancs (2)'),
('C', 'Avance', 7, 'Repertoire d''ouvertures noirs (1)'),
('C', 'Avance', 8, 'Repertoire d''ouvertures noirs (2)'),
('C', 'Avance', 9, 'Strategie : La prophylaxie'),
('C', 'Avance', 10, 'Strategie : L''echange des pieces'),
('C', 'Avance', 11, 'Strategie : Le jeu positionnel'),
('C', 'Avance', 12, 'Strategie : Les positions fermees'),
('C', 'Avance', 13, 'Finales de Tours (2)'),
('C', 'Avance', 14, 'Finales de Fous'),
('C', 'Avance', 15, 'Finales de Cavaliers'),
('C', 'Avance', 16, 'Finales de Dames'),
('C', 'Avance', 17, 'Analyse de parties de Grands Maitres'),
('C', 'Avance', 18, 'Preparation psychologique'),
('C', 'Avance', 19, 'Preparation aux tournois'),
('C', 'Avance', 20, 'Revision et evaluation Niveau C');

-- Insert Level D courses (Expert)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('D', 'Expert', 1, 'Calcul expert et visualisation'),
('D', 'Expert', 2, 'Tactique : Etudes artistiques'),
('D', 'Expert', 3, 'L''initiative et le temps'),
('D', 'Expert', 4, 'Les desequilibres'),
('D', 'Expert', 5, 'Theorie d''ouvertures : Lignes critiques'),
('D', 'Expert', 6, 'Theorie d''ouvertures : Systemes anti-mainline'),
('D', 'Expert', 7, 'Le milieu de jeu complexe'),
('D', 'Expert', 8, 'Technique de conversion'),
('D', 'Expert', 9, 'Defense dans les positions difficiles'),
('D', 'Expert', 10, 'Finales complexes (1)'),
('D', 'Expert', 11, 'Finales complexes (2)'),
('D', 'Expert', 12, 'Analyse avec moteur'),
('D', 'Expert', 13, 'Preparation specifique aux adversaires'),
('D', 'Expert', 14, 'Gestion du temps en competition'),
('D', 'Expert', 15, 'Psychologie avancee'),
('D', 'Expert', 16, 'Style de jeu personnel'),
('D', 'Expert', 17, 'Etude des champions du monde'),
('D', 'Expert', 18, 'Entrainement intensif'),
('D', 'Expert', 19, 'Preparation aux normes de titre'),
('D', 'Expert', 20, 'Revision et evaluation Niveau D');

-- Create indexes
CREATE INDEX idx_programme_courses_level ON programme_courses(level_code);
CREATE INDEX idx_programme_lessons_course ON programme_lessons(course_id);
