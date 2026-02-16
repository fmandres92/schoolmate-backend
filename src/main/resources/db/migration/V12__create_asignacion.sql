BEGIN;

CREATE TABLE IF NOT EXISTS asignacion (
    id VARCHAR(36) PRIMARY KEY,
    curso_id VARCHAR(36) NOT NULL,
    profesor_id VARCHAR(36),
    materia_id VARCHAR(36),
    tipo VARCHAR(20) NOT NULL DEFAULT 'CLASE',
    dia_semana INTEGER NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (curso_id) REFERENCES curso(id),
    FOREIGN KEY (profesor_id) REFERENCES profesor(id),
    FOREIGN KEY (materia_id) REFERENCES materia(id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_asignacion_dia_semana'
    ) THEN
        ALTER TABLE asignacion
            ADD CONSTRAINT ck_asignacion_dia_semana
            CHECK (dia_semana BETWEEN 1 AND 5);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_asignacion_hora_fin_mayor'
    ) THEN
        ALTER TABLE asignacion
            ADD CONSTRAINT ck_asignacion_hora_fin_mayor
            CHECK (hora_fin > hora_inicio);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_asignacion_tipo_campos'
    ) THEN
        ALTER TABLE asignacion
            ADD CONSTRAINT ck_asignacion_tipo_campos
            CHECK (
                (tipo = 'CLASE' AND profesor_id IS NOT NULL AND materia_id IS NOT NULL)
                OR
                (tipo = 'ALMUERZO' AND profesor_id IS NULL AND materia_id IS NULL)
            );
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_asignacion_curso_dia_hora_activa
    ON asignacion (curso_id, dia_semana, hora_inicio)
    WHERE activo = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_asignacion_profesor_dia_hora_activa
    ON asignacion (profesor_id, dia_semana, hora_inicio)
    WHERE activo = TRUE AND profesor_id IS NOT NULL;

COMMIT;
