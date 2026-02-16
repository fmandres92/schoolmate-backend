BEGIN;

CREATE TABLE IF NOT EXISTS seccion_catalogo (
    letra VARCHAR(1) PRIMARY KEY,
    orden SMALLINT NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO seccion_catalogo (letra, orden) VALUES
    ('A', 1),
    ('B', 2),
    ('C', 3),
    ('D', 4),
    ('E', 5),
    ('F', 6)
ON CONFLICT (letra) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS uq_curso_grado_ano_letra
    ON curso (grado_id, ano_escolar_id, letra);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_curso_seccion_catalogo'
    ) THEN
        ALTER TABLE curso
            ADD CONSTRAINT fk_curso_seccion_catalogo
            FOREIGN KEY (letra) REFERENCES seccion_catalogo(letra);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_curso_letra_formato'
    ) THEN
        ALTER TABLE curso
            ADD CONSTRAINT ck_curso_letra_formato
            CHECK (char_length(letra) = 1 AND letra = upper(letra));
    END IF;
END $$;

COMMIT;
