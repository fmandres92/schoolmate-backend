-- =============================================
-- FASE 4 CORREGIDA: Separar Alumno de Curso
-- Crear tabla matricula como vínculo temporal
-- Migrar datos existentes, limpiar tabla alumno
-- =============================================

-- 1. Crear tabla matricula
CREATE TABLE matricula (
    id VARCHAR(36) PRIMARY KEY,
    alumno_id VARCHAR(36) NOT NULL,
    curso_id VARCHAR(36) NOT NULL,
    ano_escolar_id VARCHAR(36) NOT NULL,
    fecha_matricula DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    FOREIGN KEY (curso_id) REFERENCES curso(id),
    FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)
);

CREATE INDEX idx_matricula_alumno ON matricula(alumno_id);
CREATE INDEX idx_matricula_curso ON matricula(curso_id);
CREATE INDEX idx_matricula_ano_escolar ON matricula(ano_escolar_id);
CREATE INDEX idx_matricula_estado ON matricula(estado);
CREATE UNIQUE INDEX uq_matricula_alumno_ano_activa 
    ON matricula(alumno_id, ano_escolar_id) 
    WHERE estado = 'ACTIVA';

-- 2. Migrar datos existentes de alumno a matricula
-- Cada alumno que tiene curso_id obtiene una matrícula ACTIVA
-- Se usa fecha_inscripcion como fecha_matricula
INSERT INTO matricula (id, alumno_id, curso_id, ano_escolar_id, fecha_matricula, estado)
SELECT 
    'mat-' || a.id,
    a.id,
    a.curso_id,
    c.ano_escolar_id,
    a.fecha_inscripcion,
    'ACTIVA'
FROM alumno a
JOIN curso c ON c.id = a.curso_id
WHERE a.curso_id IS NOT NULL;

-- 3. Quitar la FK y columnas de alumno
ALTER TABLE alumno DROP CONSTRAINT IF EXISTS alumno_curso_id_fkey;
ALTER TABLE alumno DROP COLUMN IF EXISTS curso_id;
ALTER TABLE alumno DROP COLUMN IF EXISTS fecha_inscripcion;
