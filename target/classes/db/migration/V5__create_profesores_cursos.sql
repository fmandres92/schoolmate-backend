-- Tabla: profesor
CREATE TABLE profesor (
    id VARCHAR(36) PRIMARY KEY,
    rut VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telefono VARCHAR(30),
    fecha_contratacion DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_profesor_email ON profesor(email);
CREATE INDEX idx_profesor_activo ON profesor(activo);

-- Tabla intermedia: profesor_materia (muchos a muchos)
CREATE TABLE profesor_materia (
    profesor_id VARCHAR(36) NOT NULL,
    materia_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (profesor_id, materia_id),
    FOREIGN KEY (profesor_id) REFERENCES profesor(id),
    FOREIGN KEY (materia_id) REFERENCES materia(id)
);

-- Tabla: curso
CREATE TABLE curso (
    id VARCHAR(36) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    letra VARCHAR(5) NOT NULL,
    grado_id VARCHAR(36) NOT NULL,
    ano_escolar_id VARCHAR(36) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (grado_id) REFERENCES grado(id),
    FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)
);

CREATE INDEX idx_curso_grado ON curso(grado_id);
CREATE INDEX idx_curso_ano_escolar ON curso(ano_escolar_id);
CREATE INDEX idx_curso_activo ON curso(activo);
