-- V21: Registro de sesiones de login y trazabilidad de asistencia
-- MARCADOR: DDL ejecutado directamente en Supabase SQL Editor
-- Este archivo existe para mantener consistencia del historial de migraciones Flyway.

-- Cambios aplicados:
-- 1. CREATE TABLE sesion_usuario (id, usuario_id, ip_address, latitud, longitud, precision_metros, user_agent, created_at)
-- 2. Indices: idx_sesion_usuario_usuario, idx_sesion_usuario_created, idx_sesion_usuario_usuario_created
-- 3. ALTER TABLE asistencia_clase ADD COLUMN registrado_por_usuario_id UUID (nullable, FK a usuario)
-- 4. Indice: idx_asistencia_clase_registrado_por
