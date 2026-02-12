DROP TABLE IF EXISTS materia_grado;

CREATE TABLE IF NOT EXISTS malla_curricular (
    id VARCHAR(36) PRIMARY KEY,
    materia_id VARCHAR(36) NOT NULL,
    grado_id VARCHAR(36) NOT NULL,
    ano_escolar_id VARCHAR(36) NOT NULL,
    horas_semanales INTEGER NOT NULL DEFAULT 2,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (materia_id) REFERENCES materia(id),
    FOREIGN KEY (grado_id) REFERENCES grado(id),
    FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id),
    CONSTRAINT uq_malla_materia_grado_ano UNIQUE (materia_id, grado_id, ano_escolar_id)
);

CREATE INDEX IF NOT EXISTS idx_malla_curricular_ano_escolar ON malla_curricular(ano_escolar_id);
CREATE INDEX IF NOT EXISTS idx_malla_curricular_materia_ano ON malla_curricular(materia_id, ano_escolar_id);
CREATE INDEX IF NOT EXISTS idx_malla_curricular_grado_ano ON malla_curricular(grado_id, ano_escolar_id);
CREATE INDEX IF NOT EXISTS idx_malla_curricular_activo ON malla_curricular(activo);

INSERT INTO malla_curricular (id, materia_id, grado_id, ano_escolar_id, horas_semanales, activo)
VALUES
    ('mc-1-1-2', '1', '1', '2', 2, TRUE), ('mc-1-2-2', '1', '2', '2', 2, TRUE),
    ('mc-1-3-2', '1', '3', '2', 2, TRUE), ('mc-1-4-2', '1', '4', '2', 2, TRUE),
    ('mc-1-5-2', '1', '5', '2', 2, TRUE), ('mc-1-6-2', '1', '6', '2', 2, TRUE),
    ('mc-1-7-2', '1', '7', '2', 2, TRUE), ('mc-1-8-2', '1', '8', '2', 2, TRUE),

    ('mc-2-1-2', '2', '1', '2', 2, TRUE), ('mc-2-2-2', '2', '2', '2', 2, TRUE),
    ('mc-2-3-2', '2', '3', '2', 2, TRUE), ('mc-2-4-2', '2', '4', '2', 2, TRUE),
    ('mc-2-5-2', '2', '5', '2', 2, TRUE), ('mc-2-6-2', '2', '6', '2', 2, TRUE),
    ('mc-2-7-2', '2', '7', '2', 2, TRUE), ('mc-2-8-2', '2', '8', '2', 2, TRUE),

    ('mc-3-1-2', '3', '1', '2', 2, TRUE), ('mc-3-2-2', '3', '2', '2', 2, TRUE),
    ('mc-3-3-2', '3', '3', '2', 2, TRUE), ('mc-3-4-2', '3', '4', '2', 2, TRUE),
    ('mc-3-5-2', '3', '5', '2', 2, TRUE), ('mc-3-6-2', '3', '6', '2', 2, TRUE),
    ('mc-3-7-2', '3', '7', '2', 2, TRUE), ('mc-3-8-2', '3', '8', '2', 2, TRUE),

    ('mc-4-1-2', '4', '1', '2', 2, TRUE), ('mc-4-2-2', '4', '2', '2', 2, TRUE),
    ('mc-4-3-2', '4', '3', '2', 2, TRUE), ('mc-4-4-2', '4', '4', '2', 2, TRUE),
    ('mc-4-5-2', '4', '5', '2', 2, TRUE), ('mc-4-6-2', '4', '6', '2', 2, TRUE),
    ('mc-4-7-2', '4', '7', '2', 2, TRUE), ('mc-4-8-2', '4', '8', '2', 2, TRUE),

    ('mc-5-1-2', '5', '1', '2', 2, TRUE), ('mc-5-2-2', '5', '2', '2', 2, TRUE),
    ('mc-5-3-2', '5', '3', '2', 2, TRUE), ('mc-5-4-2', '5', '4', '2', 2, TRUE),
    ('mc-5-5-2', '5', '5', '2', 2, TRUE), ('mc-5-6-2', '5', '6', '2', 2, TRUE),
    ('mc-5-7-2', '5', '7', '2', 2, TRUE), ('mc-5-8-2', '5', '8', '2', 2, TRUE),

    ('mc-6-1-2', '6', '1', '2', 2, TRUE), ('mc-6-2-2', '6', '2', '2', 2, TRUE),
    ('mc-6-3-2', '6', '3', '2', 2, TRUE), ('mc-6-4-2', '6', '4', '2', 2, TRUE),
    ('mc-6-5-2', '6', '5', '2', 2, TRUE), ('mc-6-6-2', '6', '6', '2', 2, TRUE),
    ('mc-6-7-2', '6', '7', '2', 2, TRUE), ('mc-6-8-2', '6', '8', '2', 2, TRUE),

    ('mc-7-1-2', '7', '1', '2', 2, TRUE), ('mc-7-2-2', '7', '2', '2', 2, TRUE),
    ('mc-7-3-2', '7', '3', '2', 2, TRUE), ('mc-7-4-2', '7', '4', '2', 2, TRUE),
    ('mc-7-5-2', '7', '5', '2', 2, TRUE), ('mc-7-6-2', '7', '6', '2', 2, TRUE),
    ('mc-7-7-2', '7', '7', '2', 2, TRUE), ('mc-7-8-2', '7', '8', '2', 2, TRUE),

    ('mc-8-1-2', '8', '1', '2', 2, TRUE), ('mc-8-2-2', '8', '2', '2', 2, TRUE),
    ('mc-8-3-2', '8', '3', '2', 2, TRUE), ('mc-8-4-2', '8', '4', '2', 2, TRUE),
    ('mc-8-5-2', '8', '5', '2', 2, TRUE), ('mc-8-6-2', '8', '6', '2', 2, TRUE),
    ('mc-8-7-2', '8', '7', '2', 2, TRUE), ('mc-8-8-2', '8', '8', '2', 2, TRUE),

    ('mc-9-1-2', '9', '1', '2', 2, TRUE), ('mc-9-2-2', '9', '2', '2', 2, TRUE),
    ('mc-9-3-2', '9', '3', '2', 2, TRUE), ('mc-9-4-2', '9', '4', '2', 2, TRUE),
    ('mc-9-5-2', '9', '5', '2', 2, TRUE), ('mc-9-6-2', '9', '6', '2', 2, TRUE),
    ('mc-9-7-2', '9', '7', '2', 2, TRUE), ('mc-9-8-2', '9', '8', '2', 2, TRUE),

    ('mc-10-1-2', '10', '1', '2', 2, TRUE), ('mc-10-2-2', '10', '2', '2', 2, TRUE),
    ('mc-10-3-2', '10', '3', '2', 2, TRUE), ('mc-10-4-2', '10', '4', '2', 2, TRUE),
    ('mc-10-5-2', '10', '5', '2', 2, TRUE), ('mc-10-6-2', '10', '6', '2', 2, TRUE),
    ('mc-10-7-2', '10', '7', '2', 2, TRUE), ('mc-10-8-2', '10', '8', '2', 2, TRUE),

    ('mc-11-3-2', '11', '3', '2', 2, TRUE), ('mc-11-4-2', '11', '4', '2', 2, TRUE),
    ('mc-11-5-2', '11', '5', '2', 2, TRUE), ('mc-11-6-2', '11', '6', '2', 2, TRUE),
    ('mc-11-7-2', '11', '7', '2', 2, TRUE), ('mc-11-8-2', '11', '8', '2', 2, TRUE)
ON CONFLICT (materia_id, grado_id, ano_escolar_id) DO NOTHING;
