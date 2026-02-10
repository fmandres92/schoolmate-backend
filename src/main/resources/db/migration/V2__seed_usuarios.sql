-- Passwords hasheados con BCrypt (strength 10):
-- admin123  → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- prof123   → $2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQkIhHMOY.Cf1YhKbYqPL8v7aH6yZS  
-- apod123   → $2a$10$EkRAUQk7UrBSJqWvJGiQBuQ1vFnHEVUGZlkA8UKhXK3.mQd9bqJTG

INSERT INTO usuario (id, email, password_hash, nombre, apellido, rol, profesor_id, alumno_id, activo)
VALUES
    ('admin-1', 'admin@edugestio.cl', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Carlos', 'Mendoza', 'ADMIN', NULL, NULL, TRUE),
    ('prof-1', 'profesor@edugestio.cl', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQkIhHMOY.Cf1YhKbYqPL8v7aH6yZS', 'Carlos', 'Rodríguez', 'PROFESOR', 'p2', NULL, TRUE),
    ('apod-1', 'apoderado@edugestio.cl', '$2a$10$EkRAUQk7UrBSJqWvJGiQBuQ1vFnHEVUGZlkA8UKhXK3.mQd9bqJTG', 'Carlos', 'Soto', 'APODERADO', NULL, 'al1', TRUE);
