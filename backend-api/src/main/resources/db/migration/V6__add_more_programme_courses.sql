-- Add 10 more courses per level (21-30 for each level)

-- Level A - Debutant (courses 21-30)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('A', 'Debutant', 21, 'Les pieges d''ouverture classiques'),
('A', 'Debutant', 22, 'L''attaque double'),
('A', 'Debutant', 23, 'La defense des pieces'),
('A', 'Debutant', 24, 'Les echanges de pieces'),
('A', 'Debutant', 25, 'Le centre fort'),
('A', 'Debutant', 26, 'La securite du Roi'),
('A', 'Debutant', 27, 'Les finales de base'),
('A', 'Debutant', 28, 'La coordination des pieces'),
('A', 'Debutant', 29, 'Les erreurs courantes'),
('A', 'Debutant', 30, 'Exercices de consolidation Niveau A');

-- Level B - Intermediaire (courses 21-30)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('B', 'Intermediaire', 21, 'Tactique : Le rayon X'),
('B', 'Intermediaire', 22, 'Tactique : Le moulin'),
('B', 'Intermediaire', 23, 'Les ouvertures : Defenses indiennes'),
('B', 'Intermediaire', 24, 'Les ouvertures : Systemes de Londres'),
('B', 'Intermediaire', 25, 'Strategie : Le pion passe'),
('B', 'Intermediaire', 26, 'Strategie : La majorite sur l''aile'),
('B', 'Intermediaire', 27, 'Finales de Tours (2)'),
('B', 'Intermediaire', 28, 'Finales Fou contre Cavalier'),
('B', 'Intermediaire', 29, 'Analyse de parties classiques'),
('B', 'Intermediaire', 30, 'Exercices de consolidation Niveau B');

-- Level C - Avance (courses 21-30)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('C', 'Avance', 21, 'Tactique : Sacrifices sur h7 et g7'),
('C', 'Avance', 22, 'Tactique : L''attaque grecque'),
('C', 'Avance', 23, 'Repertoire d''ouvertures : Lignes secondaires'),
('C', 'Avance', 24, 'Strategie : La restriction'),
('C', 'Avance', 25, 'Strategie : Les faiblesses chroniques'),
('C', 'Avance', 26, 'Finales theoriques avancees'),
('C', 'Avance', 27, 'Analyse de parties de Karpov'),
('C', 'Avance', 28, 'Analyse de parties de Fischer'),
('C', 'Avance', 29, 'Gestion du temps en partie'),
('C', 'Avance', 30, 'Exercices de consolidation Niveau C');

-- Level D - Expert (courses 21-30)
INSERT INTO programme_courses (level_code, level_name, course_order, title) VALUES
('D', 'Expert', 21, 'Calcul a longue portee'),
('D', 'Expert', 22, 'Intuition et jugement positionnel'),
('D', 'Expert', 23, 'Preparation d''ouverture specifique'),
('D', 'Expert', 24, 'Les transformations de pions'),
('D', 'Expert', 25, 'Finales de pieces lourdes'),
('D', 'Expert', 26, 'Analyse de parties de Carlsen'),
('D', 'Expert', 27, 'L''art de la defense'),
('D', 'Expert', 28, 'Psychologie en competition'),
('D', 'Expert', 29, 'Strategies pour gagner des points Elo'),
('D', 'Expert', 30, 'Examen final Niveau D');
