-- Normaliza estados legacy de asistencia para mantener contrato binario:
-- PRESENTE | AUSENTE
UPDATE registro_asistencia
SET estado = 'PRESENTE'
WHERE estado IN ('TARDANZA', 'JUSTIFICADO');

ALTER TABLE registro_asistencia
    DROP CONSTRAINT IF EXISTS chk_registro_asistencia_estado;

ALTER TABLE registro_asistencia
    ADD CONSTRAINT chk_registro_asistencia_estado
        CHECK (estado IN ('PRESENTE', 'AUSENTE'));
