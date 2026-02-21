-- ====================================================================================
-- V23: Consolidación de esquema faltante (Sinceramiento de Drift)
-- Objetivo: Crear de forma idempotente las tablas que se crearon manualmente en Supabase.
-- En producción esto será ignorado silenciosamente (por los IF NOT EXISTS).
-- En una BD local limpia, construirá el esquema faltante.
-- ====================================================================================

-- 1. Tabla: alumno (faltante real)
CREATE TABLE IF NOT EXISTS public.alumno (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    rut character varying(20) NOT NULL,
    nombre character varying(100) NOT NULL,
    apellido character varying(100) NOT NULL,
    fecha_nacimiento date NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT alumno_pkey PRIMARY KEY (id),
    CONSTRAINT alumno_rut_key UNIQUE (rut)
);
CREATE INDEX IF NOT EXISTS idx_alumno_activo ON public.alumno USING btree (activo);


-- 2. Tabla: bloque_horario (faltante real)
CREATE TABLE IF NOT EXISTS public.bloque_horario (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    curso_id uuid NOT NULL,
    dia_semana integer NOT NULL,
    numero_bloque integer NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    tipo character varying(20) NOT NULL,
    materia_id uuid,
    profesor_id uuid,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT bloque_horario_pkey PRIMARY KEY (id),
    CONSTRAINT bloque_horario_curso_id_fkey FOREIGN KEY (curso_id) REFERENCES public.curso(id),
    CONSTRAINT bloque_horario_materia_id_fkey FOREIGN KEY (materia_id) REFERENCES public.materia(id),
    CONSTRAINT bloque_horario_profesor_id_fkey FOREIGN KEY (profesor_id) REFERENCES public.profesor(id),
    CONSTRAINT ck_bloque_dia_semana CHECK (dia_semana >= 1 AND dia_semana <= 5),
    CONSTRAINT ck_bloque_hora_fin CHECK (hora_fin > hora_inicio),
    CONSTRAINT ck_bloque_tipo CHECK (tipo::text = ANY (ARRAY['CLASE'::character varying, 'RECREO'::character varying, 'ALMUERZO'::character varying]::text[])),
    CONSTRAINT ck_bloque_tipo_campos CHECK ((tipo::text = ANY (ARRAY['RECREO'::character varying::text, 'ALMUERZO'::character varying::text])) AND materia_id IS NULL AND profesor_id IS NULL OR tipo::text = 'CLASE'::text)
);
CREATE INDEX IF NOT EXISTS idx_bloque_horario_curso ON public.bloque_horario USING btree (curso_id);
CREATE INDEX IF NOT EXISTS idx_bloque_horario_curso_activo_tipo ON public.bloque_horario USING btree (curso_id, activo, tipo) WHERE ((activo = true) AND ((tipo)::text = 'CLASE'::text));
CREATE INDEX IF NOT EXISTS idx_bloque_horario_curso_dia ON public.bloque_horario USING btree (curso_id, dia_semana);
CREATE INDEX IF NOT EXISTS idx_bloque_horario_profesor_dia ON public.bloque_horario USING btree (profesor_id, dia_semana) WHERE ((activo = true) AND (profesor_id IS NOT NULL));
CREATE UNIQUE INDEX IF NOT EXISTS uq_bloque_curso_dia_hora_activo ON public.bloque_horario USING btree (curso_id, dia_semana, hora_inicio) WHERE (activo = true);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bloque_curso_dia_numero_activo ON public.bloque_horario USING btree (curso_id, dia_semana, numero_bloque) WHERE (activo = true);


-- 3. Tabla: apoderado (consolidación de migración marcadora V17)
CREATE TABLE IF NOT EXISTS public.apoderado (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(100) NOT NULL,
    apellido character varying(100) NOT NULL,
    rut character varying(20),
    email character varying(255),
    telefono character varying(30),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT apoderado_pkey PRIMARY KEY (id),
    CONSTRAINT apoderado_email_key UNIQUE (email),
    CONSTRAINT apoderado_rut_key UNIQUE (rut)
);


-- 4. Tabla: apoderado_alumno (consolidación de migración marcadora V17 y V18)
CREATE TABLE IF NOT EXISTS public.apoderado_alumno (
    apoderado_id uuid NOT NULL,
    alumno_id uuid NOT NULL,
    es_principal boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    vinculo character varying(20) DEFAULT 'OTRO'::character varying NOT NULL,
    CONSTRAINT apoderado_alumno_pkey PRIMARY KEY (apoderado_id, alumno_id),
    CONSTRAINT apoderado_alumno_alumno_id_fkey FOREIGN KEY (alumno_id) REFERENCES public.alumno(id),
    CONSTRAINT apoderado_alumno_apoderado_id_fkey FOREIGN KEY (apoderado_id) REFERENCES public.apoderado(id),
    CONSTRAINT chk_apoderado_alumno_vinculo CHECK ((vinculo)::text = ANY (ARRAY[('MADRE'::character varying)::text, ('PADRE'::character varying)::text, ('TUTOR_LEGAL'::character varying)::text, ('ABUELO'::character varying)::text, ('OTRO'::character varying)::text]))
);
CREATE INDEX IF NOT EXISTS idx_apoderado_alumno_alumno_id ON public.apoderado_alumno USING btree (alumno_id);


-- 5. Tabla: sesion_usuario (consolidación de migración marcadora V21)
CREATE TABLE IF NOT EXISTS public.sesion_usuario (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    usuario_id uuid NOT NULL,
    ip_address character varying(45),
    latitud numeric(10,7),
    longitud numeric(10,7),
    precision_metros numeric(8,2),
    user_agent character varying(500),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT sesion_usuario_pkey PRIMARY KEY (id),
    CONSTRAINT fk_sesion_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuario(id)
);
CREATE INDEX IF NOT EXISTS idx_sesion_usuario_created ON public.sesion_usuario USING btree (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sesion_usuario_usuario ON public.sesion_usuario USING btree (usuario_id);
CREATE INDEX IF NOT EXISTS idx_sesion_usuario_usuario_created ON public.sesion_usuario USING btree (usuario_id, created_at DESC);


-- 6. Tabla: evento_auditoria (consolidación de migración marcadora V22)
CREATE TABLE IF NOT EXISTS public.evento_auditoria (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    usuario_id uuid NOT NULL,
    usuario_email character varying(255) NOT NULL,
    usuario_rol character varying(20) NOT NULL,
    metodo_http character varying(10) NOT NULL,
    endpoint character varying(500) NOT NULL,
    request_body jsonb,
    response_status integer NOT NULL,
    ip_address character varying(45),
    ano_escolar_id uuid,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT evento_auditoria_pkey PRIMARY KEY (id),
    CONSTRAINT fk_evento_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES public.usuario(id)
);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_created ON public.evento_auditoria USING btree (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_endpoint ON public.evento_auditoria USING btree (endpoint);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_metodo ON public.evento_auditoria USING btree (metodo_http);
-- En H2 el index GIN no es nativo igual que en Postgres, por lo que usamos IF NOT EXISTS
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_request_body ON public.evento_auditoria USING gin (request_body);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_usuario ON public.evento_auditoria USING btree (usuario_id);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_usuario_created ON public.evento_auditoria USING btree (usuario_id, created_at DESC);


-- 7. Arreglo de Drift: ano_escolar.fecha_inicio_planificacion y sus validaciones
ALTER TABLE public.ano_escolar ADD COLUMN IF NOT EXISTS fecha_inicio_planificacion date;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_fechas_orden') THEN
        ALTER TABLE public.ano_escolar ADD CONSTRAINT chk_fechas_orden CHECK (fecha_inicio_planificacion < fecha_inicio AND fecha_inicio < fecha_fin);
    END IF;
END;
$$;


-- 8. Arreglo de Drift: asistencia_clase.registrado_por_usuario_id (V21)
ALTER TABLE public.asistencia_clase ADD COLUMN IF NOT EXISTS registrado_por_usuario_id uuid;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_asistencia_clase_registrado_por') THEN
        ALTER TABLE public.asistencia_clase ADD CONSTRAINT fk_asistencia_clase_registrado_por FOREIGN KEY (registrado_por_usuario_id) REFERENCES public.usuario(id);
    END IF;
END;
$$;
CREATE INDEX IF NOT EXISTS idx_asistencia_clase_registrado_por ON public.asistencia_clase USING btree (registrado_por_usuario_id);
