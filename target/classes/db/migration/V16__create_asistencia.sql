CREATE TABLE IF NOT EXISTS asistencia_clase (
    id VARCHAR(36) PRIMARY KEY,
    bloque_horario_id VARCHAR(36) NOT NULL,
    fecha DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_asistencia_clase_bloque_horario'
    ) THEN
        ALTER TABLE asistencia_clase
            ADD CONSTRAINT fk_asistencia_clase_bloque_horario
            FOREIGN KEY (bloque_horario_id) REFERENCES bloque_horario(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_asistencia_clase_bloque_fecha
    ON asistencia_clase (bloque_horario_id, fecha);

CREATE INDEX IF NOT EXISTS idx_asistencia_clase_bloque
    ON asistencia_clase (bloque_horario_id);

CREATE INDEX IF NOT EXISTS idx_asistencia_clase_fecha
    ON asistencia_clase (fecha);

CREATE TABLE IF NOT EXISTS registro_asistencia (
    id VARCHAR(36) PRIMARY KEY,
    asistencia_clase_id VARCHAR(36) NOT NULL,
    alumno_id VARCHAR(36) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_registro_asistencia_estado CHECK (estado IN ('PRESENTE', 'AUSENTE'))
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_registro_asistencia_clase'
    ) THEN
        ALTER TABLE registro_asistencia
            ADD CONSTRAINT fk_registro_asistencia_clase
            FOREIGN KEY (asistencia_clase_id) REFERENCES asistencia_clase(id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_registro_asistencia_alumno'
    ) THEN
        ALTER TABLE registro_asistencia
            ADD CONSTRAINT fk_registro_asistencia_alumno
            FOREIGN KEY (alumno_id) REFERENCES alumno(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_registro_asistencia_clase_alumno
    ON registro_asistencia (asistencia_clase_id, alumno_id);

CREATE INDEX IF NOT EXISTS idx_registro_asistencia_clase
    ON registro_asistencia (asistencia_clase_id);

CREATE INDEX IF NOT EXISTS idx_registro_asistencia_alumno
    ON registro_asistencia (alumno_id);
