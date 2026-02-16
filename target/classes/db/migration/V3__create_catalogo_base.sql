-- =============================================
-- FASE 2: Catálogo Base
-- Tablas: ano_escolar, grado, materia, materia_grado
-- =============================================

-- Tabla: ano_escolar
CREATE TABLE ano_escolar (
    id VARCHAR(36) PRIMARY KEY,
    ano INTEGER NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ano_escolar_activo ON ano_escolar(activo);
CREATE INDEX idx_ano_escolar_ano ON ano_escolar(ano);

-- Tabla: grado
CREATE TABLE grado (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    nivel INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_grado_nivel ON grado(nivel);

-- Tabla: materia
CREATE TABLE materia (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    icono VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla intermedia: materia_grado (qué materias aplican a qué grados)
CREATE TABLE materia_grado (
    materia_id VARCHAR(36) NOT NULL,
    grado_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (materia_id, grado_id),
    FOREIGN KEY (materia_id) REFERENCES materia(id),
    FOREIGN KEY (grado_id) REFERENCES grado(id)
);
