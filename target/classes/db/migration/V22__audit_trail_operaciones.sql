-- V22: Audit trail automático de operaciones
-- MARCADOR: DDL ejecutado directamente en Supabase SQL Editor
-- Este archivo existe para mantener consistencia del historial de migraciones Flyway.

-- Cambios aplicados:
-- 1. CREATE TABLE evento_auditoria (id, usuario_id, usuario_email, usuario_rol, metodo_http, endpoint, request_body JSONB, response_status, ip_address, ano_escolar_id, created_at)
-- 2. FK: fk_evento_auditoria_usuario -> usuario(id)
-- 3. Indices: idx_evento_auditoria_usuario, idx_evento_auditoria_created, idx_evento_auditoria_endpoint, idx_evento_auditoria_usuario_created, idx_evento_auditoria_metodo
-- 4. Indice GIN: idx_evento_auditoria_request_body (para búsqueda en JSONB)
