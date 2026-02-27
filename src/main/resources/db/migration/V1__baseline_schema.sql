-- ============================================================
-- V1__baseline_schema.sql
-- SchoolMate Hub — Baseline consolidado
-- Reemplaza V1–V23. BD limpia desde cero.
-- ============================================================

-- 1. grado
CREATE TABLE grado (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre            varchar(50) NOT NULL,
    nivel             integer NOT NULL,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT grado_pkey PRIMARY KEY (id),
    CONSTRAINT uq_grado_nivel UNIQUE (nivel),
    CONSTRAINT uq_grado_nombre UNIQUE (nombre)
);
CREATE INDEX idx_grado_nivel ON grado (nivel);

-- 2. seccion_catalogo
CREATE TABLE seccion_catalogo (
    letra             varchar(1) NOT NULL,
    orden             smallint NOT NULL,
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT seccion_catalogo_pkey PRIMARY KEY (letra),
    CONSTRAINT seccion_catalogo_orden_key UNIQUE (orden)
);

-- 3. ano_escolar
CREATE TABLE ano_escolar (
    id                          uuid DEFAULT gen_random_uuid() NOT NULL,
    ano                         integer NOT NULL,
    fecha_inicio                date NOT NULL,
    fecha_fin                   date NOT NULL,
    fecha_inicio_planificacion  date NOT NULL,
    created_at                  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ano_escolar_pkey PRIMARY KEY (id),
    CONSTRAINT uq_ano_escolar_ano UNIQUE (ano),
    CONSTRAINT chk_fechas_orden CHECK (fecha_inicio_planificacion < fecha_inicio AND fecha_inicio < fecha_fin)
);

-- 4. materia
CREATE TABLE materia (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre            varchar(100) NOT NULL,
    icono             varchar(50),
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT materia_pkey PRIMARY KEY (id),
    CONSTRAINT uq_materia_nombre UNIQUE (nombre)
);
CREATE INDEX idx_materia_activo ON materia (activo);

-- 5. alumno
CREATE TABLE alumno (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    rut               varchar(20) NOT NULL,
    nombre            varchar(100) NOT NULL,
    apellido          varchar(100) NOT NULL,
    fecha_nacimiento  date NOT NULL,
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT alumno_pkey PRIMARY KEY (id),
    CONSTRAINT alumno_rut_key UNIQUE (rut)
);
CREATE INDEX idx_alumno_activo ON alumno (activo);

-- 6. apoderado
CREATE TABLE apoderado (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre            varchar(100) NOT NULL,
    apellido          varchar(100) NOT NULL,
    rut               varchar(20),
    email             varchar(255),
    telefono          varchar(30),
    created_at        timestamp NOT NULL DEFAULT now(),
    updated_at        timestamp NOT NULL DEFAULT now(),
    CONSTRAINT apoderado_pkey PRIMARY KEY (id),
    CONSTRAINT apoderado_email_key UNIQUE (email),
    CONSTRAINT apoderado_rut_key UNIQUE (rut)
);

-- 7. profesor
CREATE TABLE profesor (
    id                          uuid DEFAULT gen_random_uuid() NOT NULL,
    rut                         varchar(20) NOT NULL,
    nombre                      varchar(100) NOT NULL,
    apellido                    varchar(100) NOT NULL,
    email                       varchar(255) NOT NULL,
    telefono                    varchar(30),
    fecha_contratacion          date NOT NULL,
    activo                      boolean NOT NULL DEFAULT true,
    horas_pedagogicas_contrato  integer,
    created_at                  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT profesor_pkey PRIMARY KEY (id),
    CONSTRAINT profesor_rut_key UNIQUE (rut),
    CONSTRAINT profesor_email_key UNIQUE (email)
);
CREATE INDEX idx_profesor_activo ON profesor (activo);

-- 8. apoderado_alumno
CREATE TABLE apoderado_alumno (
    apoderado_id      uuid NOT NULL,
    alumno_id         uuid NOT NULL,
    es_principal      boolean NOT NULL DEFAULT true,
    vinculo           varchar(20) NOT NULL DEFAULT 'OTRO',
    created_at        timestamp NOT NULL DEFAULT now(),
    CONSTRAINT apoderado_alumno_pkey PRIMARY KEY (apoderado_id, alumno_id),
    CONSTRAINT apoderado_alumno_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES apoderado(id),
    CONSTRAINT apoderado_alumno_alumno_id_fkey FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT chk_apoderado_alumno_vinculo CHECK (vinculo IN ('MADRE','PADRE','TUTOR_LEGAL','ABUELO','OTRO'))
);
CREATE INDEX idx_apoderado_alumno_alumno_id ON apoderado_alumno (alumno_id);

-- 9. profesor_materia
CREATE TABLE profesor_materia (
    profesor_id       uuid NOT NULL,
    materia_id        uuid NOT NULL,
    CONSTRAINT profesor_materia_pkey PRIMARY KEY (profesor_id, materia_id),
    CONSTRAINT profesor_materia_profesor_id_fkey FOREIGN KEY (profesor_id) REFERENCES profesor(id),
    CONSTRAINT profesor_materia_materia_id_fkey FOREIGN KEY (materia_id) REFERENCES materia(id)
);

-- 10. curso
CREATE TABLE curso (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    grado_id          uuid NOT NULL,
    ano_escolar_id    uuid NOT NULL,
    letra             varchar(5) NOT NULL,
    nombre            varchar(50) NOT NULL,
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT curso_pkey PRIMARY KEY (id),
    CONSTRAINT curso_grado_id_fkey FOREIGN KEY (grado_id) REFERENCES grado(id),
    CONSTRAINT curso_ano_escolar_id_fkey FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id),
    CONSTRAINT fk_curso_seccion_catalogo FOREIGN KEY (letra) REFERENCES seccion_catalogo(letra),
    CONSTRAINT ck_curso_letra_formato CHECK (char_length(letra) = 1 AND letra = upper(letra))
);
CREATE UNIQUE INDEX uq_curso_grado_ano_letra ON curso (grado_id, ano_escolar_id, letra);
CREATE INDEX idx_curso_ano_escolar ON curso (ano_escolar_id);
CREATE INDEX idx_curso_grado ON curso (grado_id);
CREATE INDEX idx_curso_activo ON curso (activo);

-- 11. usuario
CREATE TABLE usuario (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    email             varchar(255) NOT NULL,
    password_hash     varchar(255) NOT NULL,
    nombre            varchar(100) NOT NULL,
    apellido          varchar(100) NOT NULL,
    rol               varchar(20) NOT NULL,
    rut               varchar(20),
    profesor_id       uuid,
    apoderado_id      uuid,
    refresh_token     varchar(255),
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT usuario_pkey PRIMARY KEY (id),
    CONSTRAINT usuario_email_key UNIQUE (email),
    CONSTRAINT fk_usuario_profesor FOREIGN KEY (profesor_id) REFERENCES profesor(id),
    CONSTRAINT usuario_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES apoderado(id),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('ADMIN','PROFESOR','APODERADO'))
);
CREATE UNIQUE INDEX ux_usuario_rut_not_null ON usuario (rut) WHERE (rut IS NOT NULL);
CREATE INDEX idx_usuario_rol ON usuario (rol);
CREATE INDEX idx_usuario_profesor ON usuario (profesor_id) WHERE (profesor_id IS NOT NULL);
CREATE INDEX idx_usuario_apoderado ON usuario (apoderado_id);

-- 12. malla_curricular
CREATE TABLE malla_curricular (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    materia_id        uuid NOT NULL,
    grado_id          uuid NOT NULL,
    ano_escolar_id    uuid NOT NULL,
    horas_pedagogicas integer NOT NULL DEFAULT 2,
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT malla_curricular_pkey PRIMARY KEY (id),
    CONSTRAINT uq_malla_materia_grado_ano UNIQUE (materia_id, grado_id, ano_escolar_id),
    CONSTRAINT malla_curricular_materia_id_fkey FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT malla_curricular_grado_id_fkey FOREIGN KEY (grado_id) REFERENCES grado(id),
    CONSTRAINT malla_curricular_ano_escolar_id_fkey FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id),
    CONSTRAINT chk_malla_horas_positivas CHECK (horas_pedagogicas > 0)
);
CREATE INDEX idx_malla_ano_escolar ON malla_curricular (ano_escolar_id);
CREATE INDEX idx_malla_activo ON malla_curricular (activo);

-- 13. matricula
CREATE TABLE matricula (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    alumno_id         uuid NOT NULL,
    curso_id          uuid NOT NULL,
    ano_escolar_id    uuid NOT NULL,
    estado            varchar(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_matricula   date NOT NULL,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT matricula_pkey PRIMARY KEY (id),
    CONSTRAINT matricula_alumno_id_fkey FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT matricula_curso_id_fkey FOREIGN KEY (curso_id) REFERENCES curso(id),
    CONSTRAINT matricula_ano_escolar_id_fkey FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id),
    CONSTRAINT chk_matricula_estado CHECK (estado IN ('ACTIVA','RETIRADO','TRASLADADO'))
);
CREATE UNIQUE INDEX uq_matricula_alumno_ano_activa ON matricula (alumno_id, ano_escolar_id) WHERE (estado = 'ACTIVA');
CREATE INDEX idx_matricula_curso ON matricula (curso_id);
CREATE INDEX idx_matricula_ano_escolar ON matricula (ano_escolar_id);

-- 14. dia_no_lectivo
CREATE TABLE dia_no_lectivo (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    ano_escolar_id    uuid NOT NULL,
    fecha             date NOT NULL,
    tipo              varchar(30) NOT NULL,
    descripcion       varchar(200),
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dia_no_lectivo_pkey PRIMARY KEY (id),
    CONSTRAINT uq_dia_no_lectivo_ano_fecha UNIQUE (ano_escolar_id, fecha),
    CONSTRAINT fk_dia_no_lectivo_ano_escolar FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id),
    CONSTRAINT chk_dia_no_lectivo_tipo CHECK (tipo IN ('FERIADO_LEGAL','VACACIONES','SUSPENSION','INTERFERIADO','ADMINISTRATIVO'))
);
CREATE INDEX idx_dia_no_lectivo_ano_escolar ON dia_no_lectivo (ano_escolar_id);
CREATE INDEX idx_dia_no_lectivo_fecha ON dia_no_lectivo (fecha);

-- 15. bloque_horario
CREATE TABLE bloque_horario (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    curso_id          uuid NOT NULL,
    dia_semana        integer NOT NULL,
    numero_bloque     integer NOT NULL,
    hora_inicio       time NOT NULL,
    hora_fin          time NOT NULL,
    tipo              varchar(20) NOT NULL,
    materia_id        uuid,
    profesor_id       uuid,
    activo            boolean NOT NULL DEFAULT true,
    created_at        timestamp NOT NULL DEFAULT now(),
    updated_at        timestamp NOT NULL DEFAULT now(),
    CONSTRAINT bloque_horario_pkey PRIMARY KEY (id),
    CONSTRAINT bloque_horario_curso_id_fkey FOREIGN KEY (curso_id) REFERENCES curso(id),
    CONSTRAINT bloque_horario_materia_id_fkey FOREIGN KEY (materia_id) REFERENCES materia(id),
    CONSTRAINT bloque_horario_profesor_id_fkey FOREIGN KEY (profesor_id) REFERENCES profesor(id),
    CONSTRAINT ck_bloque_dia_semana CHECK (dia_semana >= 1 AND dia_semana <= 5),
    CONSTRAINT ck_bloque_hora_fin CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_bloque_tipo CHECK (tipo IN ('CLASE','RECREO','ALMUERZO'))
);
CREATE UNIQUE INDEX uq_bloque_curso_dia_numero ON bloque_horario (curso_id, dia_semana, numero_bloque) WHERE (activo = true);
CREATE INDEX idx_bloque_horario_curso ON bloque_horario (curso_id);
CREATE INDEX idx_bloque_horario_profesor ON bloque_horario (profesor_id) WHERE (profesor_id IS NOT NULL);

-- 16. sesion_usuario
CREATE TABLE sesion_usuario (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    usuario_id        uuid NOT NULL,
    ip_address        varchar(45),
    latitud           numeric(10,7),
    longitud          numeric(10,7),
    precision_metros  numeric(8,2),
    user_agent        varchar(500),
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sesion_usuario_pkey PRIMARY KEY (id),
    CONSTRAINT fk_sesion_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
CREATE INDEX idx_sesion_usuario_usuario ON sesion_usuario (usuario_id);
CREATE INDEX idx_sesion_usuario_created ON sesion_usuario (created_at DESC);
CREATE INDEX idx_sesion_usuario_usuario_created ON sesion_usuario (usuario_id, created_at DESC);

-- 17. evento_auditoria (ano_escolar_id es snapshot, sin FK intencional)
CREATE TABLE evento_auditoria (
    id                uuid DEFAULT gen_random_uuid() NOT NULL,
    usuario_id        uuid NOT NULL,
    usuario_email     varchar(255) NOT NULL,
    usuario_rol       varchar(20) NOT NULL,
    metodo_http       varchar(10) NOT NULL,
    endpoint          varchar(500) NOT NULL,
    request_body      jsonb,
    response_status   integer NOT NULL,
    ip_address        varchar(45),
    ano_escolar_id    uuid,
    created_at        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT evento_auditoria_pkey PRIMARY KEY (id),
    CONSTRAINT fk_evento_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
CREATE INDEX idx_evento_auditoria_usuario ON evento_auditoria (usuario_id);
CREATE INDEX idx_evento_auditoria_created ON evento_auditoria (created_at DESC);
CREATE INDEX idx_evento_auditoria_usuario_created ON evento_auditoria (usuario_id, created_at DESC);
CREATE INDEX idx_evento_auditoria_metodo ON evento_auditoria (metodo_http);
CREATE INDEX idx_evento_auditoria_endpoint ON evento_auditoria (endpoint);
CREATE INDEX idx_evento_auditoria_request_body ON evento_auditoria USING gin (request_body);

-- 18. asistencia_clase
CREATE TABLE asistencia_clase (
    id                          uuid DEFAULT gen_random_uuid() NOT NULL,
    bloque_horario_id           uuid NOT NULL,
    fecha                       date NOT NULL,
    registrado_por_usuario_id   uuid,
    created_at                  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT asistencia_clase_pkey PRIMARY KEY (id),
    CONSTRAINT uk_asistencia_clase_bloque_fecha UNIQUE (bloque_horario_id, fecha),
    CONSTRAINT fk_asistencia_clase_bloque_horario FOREIGN KEY (bloque_horario_id) REFERENCES bloque_horario(id),
    CONSTRAINT fk_asistencia_clase_registrado_por FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id)
);
CREATE INDEX idx_asistencia_clase_bloque ON asistencia_clase (bloque_horario_id);
CREATE INDEX idx_asistencia_clase_fecha ON asistencia_clase (fecha);
CREATE INDEX idx_asistencia_clase_registrado_por ON asistencia_clase (registrado_por_usuario_id);

-- 19. registro_asistencia
CREATE TABLE registro_asistencia (
    id                    uuid DEFAULT gen_random_uuid() NOT NULL,
    asistencia_clase_id   uuid NOT NULL,
    alumno_id             uuid NOT NULL,
    estado                varchar(20) NOT NULL,
    observacion           varchar(500),
    created_at            timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT registro_asistencia_pkey PRIMARY KEY (id),
    CONSTRAINT uk_registro_asistencia_clase_alumno UNIQUE (asistencia_clase_id, alumno_id),
    CONSTRAINT fk_registro_asistencia_clase FOREIGN KEY (asistencia_clase_id) REFERENCES asistencia_clase(id) ON DELETE CASCADE,
    CONSTRAINT fk_registro_asistencia_alumno FOREIGN KEY (alumno_id) REFERENCES alumno(id),
    CONSTRAINT chk_registro_asistencia_estado CHECK (estado IN ('PRESENTE','AUSENTE','TARDANZA','JUSTIFICADO'))
);
CREATE INDEX idx_registro_asistencia_clase ON registro_asistencia (asistencia_clase_id);
CREATE INDEX idx_registro_asistencia_alumno ON registro_asistencia (alumno_id);
CREATE INDEX idx_registro_asistencia_alumno_clase ON registro_asistencia (alumno_id, asistencia_clase_id);
