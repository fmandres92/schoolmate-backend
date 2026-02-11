-- =============================================
-- SEED DATA: Catálogo Base
-- IDs coinciden con frontend mock para compatibilidad
-- =============================================

-- Años Escolares (3 años, 2026 es el activo)
INSERT INTO ano_escolar (id, ano, fecha_inicio, fecha_fin, activo) VALUES
    ('1', 2025, '2025-03-01', '2025-12-15', FALSE),
    ('2', 2026, '2026-03-01', '2026-12-15', TRUE),
    ('3', 2027, '2027-03-01', '2027-12-15', FALSE);

-- Grados (8 grados: 1° Básico a 8° Básico)
INSERT INTO grado (id, nombre, nivel) VALUES
    ('1', '1° Básico', 1),
    ('2', '2° Básico', 2),
    ('3', '3° Básico', 3),
    ('4', '4° Básico', 4),
    ('5', '5° Básico', 5),
    ('6', '6° Básico', 6),
    ('7', '7° Básico', 7),
    ('8', '8° Básico', 8);

-- Materias (11 materias con iconos Lucide)
INSERT INTO materia (id, nombre, icono) VALUES
    ('1',  'Matemáticas',              'Calculator'),
    ('2',  'Lenguaje y Comunicación',  'BookOpen'),
    ('3',  'Ciencias Naturales',       'Microscope'),
    ('4',  'Historia y Geografía',     'Globe'),
    ('5',  'Inglés',                   'Languages'),
    ('6',  'Educación Física',         'Dumbbell'),
    ('7',  'Artes Visuales',           'Palette'),
    ('8',  'Música',                   'Music'),
    ('9',  'Tecnología',              'Monitor'),
    ('10', 'Orientación',             'Heart'),
    ('11', 'Religión',                'BookHeart');

-- Materia-Grado: Qué materias aplican a qué grados
-- Matemáticas, Lenguaje, Ciencias, Historia, Inglés, Ed. Física, Artes, Música, Tecnología, Orientación → todos los grados (1-8)
INSERT INTO materia_grado (materia_id, grado_id) VALUES
    ('1', '1'), ('1', '2'), ('1', '3'), ('1', '4'), ('1', '5'), ('1', '6'), ('1', '7'), ('1', '8'),
    ('2', '1'), ('2', '2'), ('2', '3'), ('2', '4'), ('2', '5'), ('2', '6'), ('2', '7'), ('2', '8'),
    ('3', '1'), ('3', '2'), ('3', '3'), ('3', '4'), ('3', '5'), ('3', '6'), ('3', '7'), ('3', '8'),
    ('4', '1'), ('4', '2'), ('4', '3'), ('4', '4'), ('4', '5'), ('4', '6'), ('4', '7'), ('4', '8'),
    ('5', '1'), ('5', '2'), ('5', '3'), ('5', '4'), ('5', '5'), ('5', '6'), ('5', '7'), ('5', '8'),
    ('6', '1'), ('6', '2'), ('6', '3'), ('6', '4'), ('6', '5'), ('6', '6'), ('6', '7'), ('6', '8'),
    ('7', '1'), ('7', '2'), ('7', '3'), ('7', '4'), ('7', '5'), ('7', '6'), ('7', '7'), ('7', '8'),
    ('8', '1'), ('8', '2'), ('8', '3'), ('8', '4'), ('8', '5'), ('8', '6'), ('8', '7'), ('8', '8'),
    ('9', '1'), ('9', '2'), ('9', '3'), ('9', '4'), ('9', '5'), ('9', '6'), ('9', '7'), ('9', '8'),
    ('10', '1'), ('10', '2'), ('10', '3'), ('10', '4'), ('10', '5'), ('10', '6'), ('10', '7'), ('10', '8');

-- Religión → solo grados 3-8
INSERT INTO materia_grado (materia_id, grado_id) VALUES
    ('11', '3'), ('11', '4'), ('11', '5'), ('11', '6'), ('11', '7'), ('11', '8');
