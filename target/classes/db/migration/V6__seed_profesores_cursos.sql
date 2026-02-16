-- PROFESORES (15 profesores, IDs p1-p15)
-- IMPORTANTE: Los IDs de materia referencian la tabla materia existente (IDs '1'-'11')

INSERT INTO profesor (id, rut, nombre, apellido, email, telefono, fecha_contratacion, activo) VALUES
('p1',  '12.345.678-9', 'María',    'González',  'maria.gonzalez@colegio.cl',  '+56 9 1234 5678', '2020-03-01', TRUE),
('p2',  '13.456.789-0', 'Carlos',   'Rodríguez', 'carlos.rodriguez@colegio.cl','+56 9 2345 6789', '2019-03-01', TRUE),
('p3',  '14.567.890-1', 'Ana',      'Martínez',  'ana.martinez@colegio.cl',    '+56 9 3456 7890', '2021-03-01', TRUE),
('p4',  '15.678.901-2', 'Pedro',    'López',     'pedro.lopez@colegio.cl',     '+56 9 4567 8901', '2018-03-01', TRUE),
('p5',  '16.789.012-3', 'Sofía',    'Hernández', 'sofia.hernandez@colegio.cl', '+56 9 5678 9012', '2022-03-01', TRUE),
('p6',  '17.890.123-4', 'Jorge',    'García',    'jorge.garcia@colegio.cl',    '+56 9 6789 0123', '2020-03-01', TRUE),
('p7',  '18.901.234-5', 'Valentina','Díaz',      'valentina.diaz@colegio.cl',  '+56 9 7890 1234', '2021-03-01', TRUE),
('p8',  '19.012.345-6', 'Andrés',   'Muñoz',     'andres.munoz@colegio.cl',    '+56 9 8901 2345', '2019-03-01', TRUE),
('p9',  '20.123.456-7', 'Camila',   'Rojas',     'camila.rojas@colegio.cl',    '+56 9 9012 3456', '2023-03-01', TRUE),
('p10', '21.234.567-8', 'Roberto',  'Sánchez',   'roberto.sanchez@colegio.cl', '+56 9 0123 4567', '2020-03-01', TRUE),
('p11', '22.345.678-9', 'Isabel',   'Torres',    'isabel.torres@colegio.cl',   '+56 9 1234 5670', '2022-03-01', TRUE),
('p12', '23.456.789-0', 'Fernando', 'Vargas',    'fernando.vargas@colegio.cl', '+56 9 2345 6780', '2018-03-01', TRUE),
('p13', '24.567.890-1', 'Daniela',  'Morales',   'daniela.morales@colegio.cl', '+56 9 3456 7891', '2021-03-01', TRUE),
('p14', '25.678.901-2', 'Alejandro','Flores',    'alejandro.flores@colegio.cl','+56 9 4567 8902', '2023-03-01', TRUE),
('p15', '26.789.012-3', 'Patricia', 'Castillo',  'patricia.castillo@colegio.cl','+56 9 5678 9013', '2017-03-01', FALSE);

-- RELACIÓN PROFESOR-MATERIA
-- Cada profesor imparte 1-3 materias
-- IDs de materia: 1=Matemáticas, 2=Lenguaje, 3=Ciencias, 4=Historia, 5=Inglés,
-- 6=Ed.Física, 7=Artes, 8=Música, 9=Tecnología, 10=Orientación, 11=Religión

INSERT INTO profesor_materia (profesor_id, materia_id) VALUES
('p1', '1'),   -- María González: Matemáticas
('p1', '3'),   -- María González: Ciencias
('p2', '1'),   -- Carlos Rodríguez: Matemáticas
('p2', '9'),   -- Carlos Rodríguez: Tecnología
('p3', '2'),   -- Ana Martínez: Lenguaje
('p4', '4'),   -- Pedro López: Historia
('p4', '10'),  -- Pedro López: Orientación
('p5', '5'),   -- Sofía Hernández: Inglés
('p6', '6'),   -- Jorge García: Ed. Física
('p7', '7'),   -- Valentina Díaz: Artes
('p7', '8'),   -- Valentina Díaz: Música
('p8', '3'),   -- Andrés Muñoz: Ciencias
('p8', '9'),   -- Andrés Muñoz: Tecnología
('p9', '2'),   -- Camila Rojas: Lenguaje
('p9', '4'),   -- Camila Rojas: Historia
('p10', '1'),  -- Roberto Sánchez: Matemáticas
('p10', '3'),  -- Roberto Sánchez: Ciencias
('p11', '5'),  -- Isabel Torres: Inglés
('p11', '2'),  -- Isabel Torres: Lenguaje
('p12', '6'),  -- Fernando Vargas: Ed. Física
('p13', '7'),  -- Daniela Morales: Artes
('p13', '8'),  -- Daniela Morales: Música
('p14', '11'), -- Alejandro Flores: Religión
('p14', '10'), -- Alejandro Flores: Orientación
('p15', '4'),  -- Patricia Castillo: Historia (inactiva)
('p15', '10'); -- Patricia Castillo: Orientación (inactiva)

-- CURSOS (18 cursos para año 2026, IDs c1-c18)
-- 2-3 cursos por grado, todos del año escolar '2' (2026)
INSERT INTO curso (id, nombre, letra, grado_id, ano_escolar_id, activo) VALUES
('c1',  '1° Básico A', 'A', '1', '2', TRUE),
('c2',  '1° Básico B', 'B', '1', '2', TRUE),
('c3',  '2° Básico A', 'A', '2', '2', TRUE),
('c4',  '2° Básico B', 'B', '2', '2', TRUE),
('c5',  '3° Básico A', 'A', '3', '2', TRUE),
('c6',  '3° Básico B', 'B', '3', '2', TRUE),
('c7',  '4° Básico A', 'A', '4', '2', TRUE),
('c8',  '4° Básico B', 'B', '4', '2', TRUE),
('c9',  '5° Básico A', 'A', '5', '2', TRUE),
('c10', '5° Básico B', 'B', '5', '2', TRUE),
('c11', '6° Básico A', 'A', '6', '2', TRUE),
('c12', '6° Básico B', 'B', '6', '2', TRUE),
('c13', '7° Básico A', 'A', '7', '2', TRUE),
('c14', '7° Básico B', 'B', '7', '2', TRUE),
('c15', '7° Básico C', 'C', '7', '2', TRUE),
('c16', '8° Básico A', 'A', '8', '2', TRUE),
('c17', '8° Básico B', 'B', '8', '2', TRUE),
('c18', '8° Básico C', 'C', '8', '2', TRUE);
