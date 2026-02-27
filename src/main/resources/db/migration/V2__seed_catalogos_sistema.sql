-- ============================================================
-- V2__seed_catalogos_sistema.sql
-- Catálogos del sistema que no se crean desde la UI
-- ============================================================

-- Grados: Educación Básica (1° a 8°)
INSERT INTO grado (id, nombre, nivel) VALUES
    (gen_random_uuid(), '1° Básico', 1),
    (gen_random_uuid(), '2° Básico', 2),
    (gen_random_uuid(), '3° Básico', 3),
    (gen_random_uuid(), '4° Básico', 4),
    (gen_random_uuid(), '5° Básico', 5),
    (gen_random_uuid(), '6° Básico', 6),
    (gen_random_uuid(), '7° Básico', 7),
    (gen_random_uuid(), '8° Básico', 8);

-- Secciones: letras para identificar cursos paralelos (A–J)
INSERT INTO seccion_catalogo (letra, orden) VALUES
    ('A', 1),
    ('B', 2),
    ('C', 3),
    ('D', 4),
    ('E', 5),
    ('F', 6),
    ('G', 7),
    ('H', 8),
    ('I', 9),
    ('J', 10);
