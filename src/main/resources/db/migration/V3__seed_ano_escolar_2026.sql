-- ============================================================
-- V3__seed_ano_escolar_2026.sql
-- Seed inicial de año escolar para disponer de data operativa
-- ============================================================

INSERT INTO ano_escolar (
    id,
    ano,
    fecha_inicio_planificacion,
    fecha_inicio,
    fecha_fin
) VALUES (
    gen_random_uuid(),
    2026,
    DATE '2026-01-01',
    DATE '2026-02-10',
    DATE '2026-12-31'
)
ON CONFLICT (ano) DO NOTHING;
