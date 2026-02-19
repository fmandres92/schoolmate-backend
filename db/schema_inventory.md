# Database Structure Inventory

Generated at: `2026-02-19T14:54:36.313856-03:00`  
Source: live PostgreSQL catalog (`information_schema` + `pg_catalog`)  
Connection: `jdbc:postgresql://db.suoiyaaswcibsbrvpjxa.supabase.co:5432/postgres?sslmode=require`

## Summary

- Schemas: **5**
- Tables: **47**
- Foreign Keys: **42**

## Relationships (Foreign Keys)

- `auth.identities` -> `identities_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- `auth.mfa_amr_claims` -> `mfa_amr_claims_session_id_fkey`: `FOREIGN KEY (session_id) REFERENCES auth.sessions(id) ON DELETE CASCADE`
- `auth.mfa_challenges` -> `mfa_challenges_auth_factor_id_fkey`: `FOREIGN KEY (factor_id) REFERENCES auth.mfa_factors(id) ON DELETE CASCADE`
- `auth.mfa_factors` -> `mfa_factors_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- `auth.oauth_authorizations` -> `oauth_authorizations_client_id_fkey`: `FOREIGN KEY (client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE`
- `auth.oauth_authorizations` -> `oauth_authorizations_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- `auth.oauth_consents` -> `oauth_consents_client_id_fkey`: `FOREIGN KEY (client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE`
- `auth.oauth_consents` -> `oauth_consents_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- `auth.one_time_tokens` -> `one_time_tokens_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- `auth.refresh_tokens` -> `refresh_tokens_session_id_fkey`: `FOREIGN KEY (session_id) REFERENCES auth.sessions(id) ON DELETE CASCADE`
- `auth.saml_providers` -> `saml_providers_sso_provider_id_fkey`: `FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE`
- `auth.saml_relay_states` -> `saml_relay_states_flow_state_id_fkey`: `FOREIGN KEY (flow_state_id) REFERENCES auth.flow_state(id) ON DELETE CASCADE`
- `auth.saml_relay_states` -> `saml_relay_states_sso_provider_id_fkey`: `FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE`
- `auth.sessions` -> `sessions_oauth_client_id_fkey`: `FOREIGN KEY (oauth_client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE`
- `auth.sessions` -> `sessions_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- `auth.sso_domains` -> `sso_domains_sso_provider_id_fkey`: `FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE`
- `public.apoderado_alumno` -> `apoderado_alumno_alumno_id_fkey`: `FOREIGN KEY (alumno_id) REFERENCES alumno(id)`
- `public.apoderado_alumno` -> `apoderado_alumno_apoderado_id_fkey`: `FOREIGN KEY (apoderado_id) REFERENCES apoderado(id)`
- `public.asistencia_clase` -> `fk_asistencia_clase_bloque_horario`: `FOREIGN KEY (bloque_horario_id) REFERENCES bloque_horario(id)`
- `public.bloque_horario` -> `bloque_horario_curso_id_fkey`: `FOREIGN KEY (curso_id) REFERENCES curso(id)`
- `public.bloque_horario` -> `bloque_horario_materia_id_fkey`: `FOREIGN KEY (materia_id) REFERENCES materia(id)`
- `public.bloque_horario` -> `bloque_horario_profesor_id_fkey`: `FOREIGN KEY (profesor_id) REFERENCES profesor(id)`
- `public.curso` -> `curso_ano_escolar_id_fkey`: `FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)`
- `public.curso` -> `curso_grado_id_fkey`: `FOREIGN KEY (grado_id) REFERENCES grado(id)`
- `public.curso` -> `fk_curso_seccion_catalogo`: `FOREIGN KEY (letra) REFERENCES seccion_catalogo(letra)`
- `public.malla_curricular` -> `malla_curricular_ano_escolar_id_fkey`: `FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)`
- `public.malla_curricular` -> `malla_curricular_grado_id_fkey`: `FOREIGN KEY (grado_id) REFERENCES grado(id)`
- `public.malla_curricular` -> `malla_curricular_materia_id_fkey`: `FOREIGN KEY (materia_id) REFERENCES materia(id)`
- `public.matricula` -> `matricula_alumno_id_fkey`: `FOREIGN KEY (alumno_id) REFERENCES alumno(id)`
- `public.matricula` -> `matricula_ano_escolar_id_fkey`: `FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)`
- `public.matricula` -> `matricula_curso_id_fkey`: `FOREIGN KEY (curso_id) REFERENCES curso(id)`
- `public.profesor_materia` -> `profesor_materia_materia_id_fkey`: `FOREIGN KEY (materia_id) REFERENCES materia(id)`
- `public.profesor_materia` -> `profesor_materia_profesor_id_fkey`: `FOREIGN KEY (profesor_id) REFERENCES profesor(id)`
- `public.registro_asistencia` -> `fk_registro_asistencia_alumno`: `FOREIGN KEY (alumno_id) REFERENCES alumno(id)`
- `public.registro_asistencia` -> `fk_registro_asistencia_clase`: `FOREIGN KEY (asistencia_clase_id) REFERENCES asistencia_clase(id) ON DELETE CASCADE`
- `public.usuario` -> `fk_usuario_profesor`: `FOREIGN KEY (profesor_id) REFERENCES profesor(id)`
- `public.usuario` -> `usuario_apoderado_id_fkey`: `FOREIGN KEY (apoderado_id) REFERENCES apoderado(id)`
- `storage.objects` -> `objects_bucketId_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id)`
- `storage.s3_multipart_uploads` -> `s3_multipart_uploads_bucket_id_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id)`
- `storage.s3_multipart_uploads_parts` -> `s3_multipart_uploads_parts_bucket_id_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id)`
- `storage.s3_multipart_uploads_parts` -> `s3_multipart_uploads_parts_upload_id_fkey`: `FOREIGN KEY (upload_id) REFERENCES storage.s3_multipart_uploads(id) ON DELETE CASCADE`
- `storage.vector_indexes` -> `vector_indexes_bucket_id_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets_vectors(id)`

## Tables

### `auth.audit_log_entries`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `instance_id` | `uuid` | YES | `` |
| `id` | `uuid` | NO | `` |
| `payload` | `json` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `ip_address` | `character varying(64)` | NO | `''::character varying` |

Constraints:

- [PK] `audit_log_entries_pkey`: `PRIMARY KEY (id)`

Indexes:

- [PK-INDEX] `audit_log_entries_pkey`: `CREATE UNIQUE INDEX audit_log_entries_pkey ON auth.audit_log_entries USING btree (id)`
- [INDEX] `audit_logs_instance_id_idx`: `CREATE INDEX audit_logs_instance_id_idx ON auth.audit_log_entries USING btree (instance_id)`

### `auth.flow_state`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `user_id` | `uuid` | YES | `` |
| `auth_code` | `text` | YES | `` |
| `code_challenge_method` | `auth.code_challenge_method` | YES | `` |
| `code_challenge` | `text` | YES | `` |
| `provider_type` | `text` | NO | `` |
| `provider_access_token` | `text` | YES | `` |
| `provider_refresh_token` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `authentication_method` | `text` | NO | `` |
| `auth_code_issued_at` | `timestamp with time zone` | YES | `` |
| `invite_token` | `text` | YES | `` |
| `referrer` | `text` | YES | `` |
| `oauth_client_state_id` | `uuid` | YES | `` |
| `linking_target_id` | `uuid` | YES | `` |
| `email_optional` | `boolean` | NO | `false` |

Constraints:

- [PK] `flow_state_pkey`: `PRIMARY KEY (id)`

Indexes:

- [INDEX] `flow_state_created_at_idx`: `CREATE INDEX flow_state_created_at_idx ON auth.flow_state USING btree (created_at DESC)`
- [PK-INDEX] `flow_state_pkey`: `CREATE UNIQUE INDEX flow_state_pkey ON auth.flow_state USING btree (id)`
- [INDEX] `idx_auth_code`: `CREATE INDEX idx_auth_code ON auth.flow_state USING btree (auth_code)`
- [INDEX] `idx_user_id_auth_method`: `CREATE INDEX idx_user_id_auth_method ON auth.flow_state USING btree (user_id, authentication_method)`

### `auth.identities`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `provider_id` | `text` | NO | `` |
| `user_id` | `uuid` | NO | `` |
| `identity_data` | `jsonb` | NO | `` |
| `provider` | `text` | NO | `` |
| `last_sign_in_at` | `timestamp with time zone` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `email` | `text` | YES | `lower((identity_data ->> 'email'::text))` |
| `id` | `uuid` | NO | `gen_random_uuid()` |

Constraints:

- [PK] `identities_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `identities_provider_id_provider_unique`: `UNIQUE (provider_id, provider)`
- [FK] `identities_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`

Indexes:

- [INDEX] `identities_email_idx`: `CREATE INDEX identities_email_idx ON auth.identities USING btree (email text_pattern_ops)`
- [PK-INDEX] `identities_pkey`: `CREATE UNIQUE INDEX identities_pkey ON auth.identities USING btree (id)`
- [INDEX] `identities_provider_id_provider_unique`: `CREATE UNIQUE INDEX identities_provider_id_provider_unique ON auth.identities USING btree (provider_id, provider)`
- [INDEX] `identities_user_id_idx`: `CREATE INDEX identities_user_id_idx ON auth.identities USING btree (user_id)`

### `auth.instances`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `uuid` | `uuid` | YES | `` |
| `raw_base_config` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |

Constraints:

- [PK] `instances_pkey`: `PRIMARY KEY (id)`

Indexes:

- [PK-INDEX] `instances_pkey`: `CREATE UNIQUE INDEX instances_pkey ON auth.instances USING btree (id)`

### `auth.mfa_amr_claims`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `session_id` | `uuid` | NO | `` |
| `created_at` | `timestamp with time zone` | NO | `` |
| `updated_at` | `timestamp with time zone` | NO | `` |
| `authentication_method` | `text` | NO | `` |
| `id` | `uuid` | NO | `` |

Constraints:

- [PK] `amr_id_pk`: `PRIMARY KEY (id)`
- [UNIQUE] `mfa_amr_claims_session_id_authentication_method_pkey`: `UNIQUE (session_id, authentication_method)`
- [FK] `mfa_amr_claims_session_id_fkey`: `FOREIGN KEY (session_id) REFERENCES auth.sessions(id) ON DELETE CASCADE`

Indexes:

- [PK-INDEX] `amr_id_pk`: `CREATE UNIQUE INDEX amr_id_pk ON auth.mfa_amr_claims USING btree (id)`
- [INDEX] `mfa_amr_claims_session_id_authentication_method_pkey`: `CREATE UNIQUE INDEX mfa_amr_claims_session_id_authentication_method_pkey ON auth.mfa_amr_claims USING btree (session_id, authentication_method)`

### `auth.mfa_challenges`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `factor_id` | `uuid` | NO | `` |
| `created_at` | `timestamp with time zone` | NO | `` |
| `verified_at` | `timestamp with time zone` | YES | `` |
| `ip_address` | `inet` | NO | `` |
| `otp_code` | `text` | YES | `` |
| `web_authn_session_data` | `jsonb` | YES | `` |

Constraints:

- [PK] `mfa_challenges_pkey`: `PRIMARY KEY (id)`
- [FK] `mfa_challenges_auth_factor_id_fkey`: `FOREIGN KEY (factor_id) REFERENCES auth.mfa_factors(id) ON DELETE CASCADE`

Indexes:

- [INDEX] `mfa_challenge_created_at_idx`: `CREATE INDEX mfa_challenge_created_at_idx ON auth.mfa_challenges USING btree (created_at DESC)`
- [PK-INDEX] `mfa_challenges_pkey`: `CREATE UNIQUE INDEX mfa_challenges_pkey ON auth.mfa_challenges USING btree (id)`

### `auth.mfa_factors`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `user_id` | `uuid` | NO | `` |
| `friendly_name` | `text` | YES | `` |
| `factor_type` | `auth.factor_type` | NO | `` |
| `status` | `auth.factor_status` | NO | `` |
| `created_at` | `timestamp with time zone` | NO | `` |
| `updated_at` | `timestamp with time zone` | NO | `` |
| `secret` | `text` | YES | `` |
| `phone` | `text` | YES | `` |
| `last_challenged_at` | `timestamp with time zone` | YES | `` |
| `web_authn_credential` | `jsonb` | YES | `` |
| `web_authn_aaguid` | `uuid` | YES | `` |
| `last_webauthn_challenge_data` | `jsonb` | YES | `` |

Constraints:

- [PK] `mfa_factors_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `mfa_factors_last_challenged_at_key`: `UNIQUE (last_challenged_at)`
- [FK] `mfa_factors_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`

Indexes:

- [INDEX] `factor_id_created_at_idx`: `CREATE INDEX factor_id_created_at_idx ON auth.mfa_factors USING btree (user_id, created_at)`
- [INDEX] `mfa_factors_last_challenged_at_key`: `CREATE UNIQUE INDEX mfa_factors_last_challenged_at_key ON auth.mfa_factors USING btree (last_challenged_at)`
- [PK-INDEX] `mfa_factors_pkey`: `CREATE UNIQUE INDEX mfa_factors_pkey ON auth.mfa_factors USING btree (id)`
- [INDEX] `mfa_factors_user_friendly_name_unique`: `CREATE UNIQUE INDEX mfa_factors_user_friendly_name_unique ON auth.mfa_factors USING btree (friendly_name, user_id) WHERE (TRIM(BOTH FROM friendly_name) <> ''::text)`
- [INDEX] `mfa_factors_user_id_idx`: `CREATE INDEX mfa_factors_user_id_idx ON auth.mfa_factors USING btree (user_id)`
- [INDEX] `unique_phone_factor_per_user`: `CREATE UNIQUE INDEX unique_phone_factor_per_user ON auth.mfa_factors USING btree (user_id, phone)`

### `auth.oauth_authorizations`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `authorization_id` | `text` | NO | `` |
| `client_id` | `uuid` | NO | `` |
| `user_id` | `uuid` | YES | `` |
| `redirect_uri` | `text` | NO | `` |
| `scope` | `text` | NO | `` |
| `state` | `text` | YES | `` |
| `resource` | `text` | YES | `` |
| `code_challenge` | `text` | YES | `` |
| `code_challenge_method` | `auth.code_challenge_method` | YES | `` |
| `response_type` | `auth.oauth_response_type` | NO | `'code'::auth.oauth_response_type` |
| `status` | `auth.oauth_authorization_status` | NO | `'pending'::auth.oauth_authorization_status` |
| `authorization_code` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | NO | `now()` |
| `expires_at` | `timestamp with time zone` | NO | `(now() + '00:03:00'::interval)` |
| `approved_at` | `timestamp with time zone` | YES | `` |
| `nonce` | `text` | YES | `` |

Constraints:

- [PK] `oauth_authorizations_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `oauth_authorizations_authorization_code_key`: `UNIQUE (authorization_code)`
- [UNIQUE] `oauth_authorizations_authorization_id_key`: `UNIQUE (authorization_id)`
- [FK] `oauth_authorizations_client_id_fkey`: `FOREIGN KEY (client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE`
- [FK] `oauth_authorizations_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- [CHECK] `oauth_authorizations_authorization_code_length`: `CHECK (char_length(authorization_code) <= 255)`
- [CHECK] `oauth_authorizations_code_challenge_length`: `CHECK (char_length(code_challenge) <= 128)`
- [CHECK] `oauth_authorizations_expires_at_future`: `CHECK (expires_at > created_at)`
- [CHECK] `oauth_authorizations_nonce_length`: `CHECK (char_length(nonce) <= 255)`
- [CHECK] `oauth_authorizations_redirect_uri_length`: `CHECK (char_length(redirect_uri) <= 2048)`
- [CHECK] `oauth_authorizations_resource_length`: `CHECK (char_length(resource) <= 2048)`
- [CHECK] `oauth_authorizations_scope_length`: `CHECK (char_length(scope) <= 4096)`
- [CHECK] `oauth_authorizations_state_length`: `CHECK (char_length(state) <= 4096)`

Indexes:

- [INDEX] `oauth_auth_pending_exp_idx`: `CREATE INDEX oauth_auth_pending_exp_idx ON auth.oauth_authorizations USING btree (expires_at) WHERE (status = 'pending'::auth.oauth_authorization_status)`
- [INDEX] `oauth_authorizations_authorization_code_key`: `CREATE UNIQUE INDEX oauth_authorizations_authorization_code_key ON auth.oauth_authorizations USING btree (authorization_code)`
- [INDEX] `oauth_authorizations_authorization_id_key`: `CREATE UNIQUE INDEX oauth_authorizations_authorization_id_key ON auth.oauth_authorizations USING btree (authorization_id)`
- [PK-INDEX] `oauth_authorizations_pkey`: `CREATE UNIQUE INDEX oauth_authorizations_pkey ON auth.oauth_authorizations USING btree (id)`

### `auth.oauth_client_states`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `provider_type` | `text` | NO | `` |
| `code_verifier` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | NO | `` |

Constraints:

- [PK] `oauth_client_states_pkey`: `PRIMARY KEY (id)`

Indexes:

- [INDEX] `idx_oauth_client_states_created_at`: `CREATE INDEX idx_oauth_client_states_created_at ON auth.oauth_client_states USING btree (created_at)`
- [PK-INDEX] `oauth_client_states_pkey`: `CREATE UNIQUE INDEX oauth_client_states_pkey ON auth.oauth_client_states USING btree (id)`

### `auth.oauth_clients`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `client_secret_hash` | `text` | YES | `` |
| `registration_type` | `auth.oauth_registration_type` | NO | `` |
| `redirect_uris` | `text` | NO | `` |
| `grant_types` | `text` | NO | `` |
| `client_name` | `text` | YES | `` |
| `client_uri` | `text` | YES | `` |
| `logo_uri` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | NO | `now()` |
| `updated_at` | `timestamp with time zone` | NO | `now()` |
| `deleted_at` | `timestamp with time zone` | YES | `` |
| `client_type` | `auth.oauth_client_type` | NO | `'confidential'::auth.oauth_client_type` |
| `token_endpoint_auth_method` | `text` | NO | `` |

Constraints:

- [PK] `oauth_clients_pkey`: `PRIMARY KEY (id)`
- [CHECK] `oauth_clients_client_name_length`: `CHECK (char_length(client_name) <= 1024)`
- [CHECK] `oauth_clients_client_uri_length`: `CHECK (char_length(client_uri) <= 2048)`
- [CHECK] `oauth_clients_logo_uri_length`: `CHECK (char_length(logo_uri) <= 2048)`
- [CHECK] `oauth_clients_token_endpoint_auth_method_check`: `CHECK (token_endpoint_auth_method = ANY (ARRAY['client_secret_basic'::text, 'client_secret_post'::text, 'none'::text]))`

Indexes:

- [INDEX] `oauth_clients_deleted_at_idx`: `CREATE INDEX oauth_clients_deleted_at_idx ON auth.oauth_clients USING btree (deleted_at)`
- [PK-INDEX] `oauth_clients_pkey`: `CREATE UNIQUE INDEX oauth_clients_pkey ON auth.oauth_clients USING btree (id)`

### `auth.oauth_consents`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `user_id` | `uuid` | NO | `` |
| `client_id` | `uuid` | NO | `` |
| `scopes` | `text` | NO | `` |
| `granted_at` | `timestamp with time zone` | NO | `now()` |
| `revoked_at` | `timestamp with time zone` | YES | `` |

Constraints:

- [PK] `oauth_consents_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `oauth_consents_user_client_unique`: `UNIQUE (user_id, client_id)`
- [FK] `oauth_consents_client_id_fkey`: `FOREIGN KEY (client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE`
- [FK] `oauth_consents_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- [CHECK] `oauth_consents_revoked_after_granted`: `CHECK (revoked_at IS NULL OR revoked_at >= granted_at)`
- [CHECK] `oauth_consents_scopes_length`: `CHECK (char_length(scopes) <= 2048)`
- [CHECK] `oauth_consents_scopes_not_empty`: `CHECK (char_length(TRIM(BOTH FROM scopes)) > 0)`

Indexes:

- [INDEX] `oauth_consents_active_client_idx`: `CREATE INDEX oauth_consents_active_client_idx ON auth.oauth_consents USING btree (client_id) WHERE (revoked_at IS NULL)`
- [INDEX] `oauth_consents_active_user_client_idx`: `CREATE INDEX oauth_consents_active_user_client_idx ON auth.oauth_consents USING btree (user_id, client_id) WHERE (revoked_at IS NULL)`
- [PK-INDEX] `oauth_consents_pkey`: `CREATE UNIQUE INDEX oauth_consents_pkey ON auth.oauth_consents USING btree (id)`
- [INDEX] `oauth_consents_user_client_unique`: `CREATE UNIQUE INDEX oauth_consents_user_client_unique ON auth.oauth_consents USING btree (user_id, client_id)`
- [INDEX] `oauth_consents_user_order_idx`: `CREATE INDEX oauth_consents_user_order_idx ON auth.oauth_consents USING btree (user_id, granted_at DESC)`

### `auth.one_time_tokens`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `user_id` | `uuid` | NO | `` |
| `token_type` | `auth.one_time_token_type` | NO | `` |
| `token_hash` | `text` | NO | `` |
| `relates_to` | `text` | NO | `` |
| `created_at` | `timestamp without time zone` | NO | `now()` |
| `updated_at` | `timestamp without time zone` | NO | `now()` |

Constraints:

- [PK] `one_time_tokens_pkey`: `PRIMARY KEY (id)`
- [FK] `one_time_tokens_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- [CHECK] `one_time_tokens_token_hash_check`: `CHECK (char_length(token_hash) > 0)`

Indexes:

- [PK-INDEX] `one_time_tokens_pkey`: `CREATE UNIQUE INDEX one_time_tokens_pkey ON auth.one_time_tokens USING btree (id)`
- [INDEX] `one_time_tokens_relates_to_hash_idx`: `CREATE INDEX one_time_tokens_relates_to_hash_idx ON auth.one_time_tokens USING hash (relates_to)`
- [INDEX] `one_time_tokens_token_hash_hash_idx`: `CREATE INDEX one_time_tokens_token_hash_hash_idx ON auth.one_time_tokens USING hash (token_hash)`
- [INDEX] `one_time_tokens_user_id_token_type_key`: `CREATE UNIQUE INDEX one_time_tokens_user_id_token_type_key ON auth.one_time_tokens USING btree (user_id, token_type)`

### `auth.refresh_tokens`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `instance_id` | `uuid` | YES | `` |
| `id` | `bigint` | NO | `nextval('auth.refresh_tokens_id_seq'::regclass)` |
| `token` | `character varying(255)` | YES | `` |
| `user_id` | `character varying(255)` | YES | `` |
| `revoked` | `boolean` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `parent` | `character varying(255)` | YES | `` |
| `session_id` | `uuid` | YES | `` |

Constraints:

- [PK] `refresh_tokens_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `refresh_tokens_token_unique`: `UNIQUE (token)`
- [FK] `refresh_tokens_session_id_fkey`: `FOREIGN KEY (session_id) REFERENCES auth.sessions(id) ON DELETE CASCADE`

Indexes:

- [INDEX] `refresh_tokens_instance_id_idx`: `CREATE INDEX refresh_tokens_instance_id_idx ON auth.refresh_tokens USING btree (instance_id)`
- [INDEX] `refresh_tokens_instance_id_user_id_idx`: `CREATE INDEX refresh_tokens_instance_id_user_id_idx ON auth.refresh_tokens USING btree (instance_id, user_id)`
- [INDEX] `refresh_tokens_parent_idx`: `CREATE INDEX refresh_tokens_parent_idx ON auth.refresh_tokens USING btree (parent)`
- [PK-INDEX] `refresh_tokens_pkey`: `CREATE UNIQUE INDEX refresh_tokens_pkey ON auth.refresh_tokens USING btree (id)`
- [INDEX] `refresh_tokens_session_id_revoked_idx`: `CREATE INDEX refresh_tokens_session_id_revoked_idx ON auth.refresh_tokens USING btree (session_id, revoked)`
- [INDEX] `refresh_tokens_token_unique`: `CREATE UNIQUE INDEX refresh_tokens_token_unique ON auth.refresh_tokens USING btree (token)`
- [INDEX] `refresh_tokens_updated_at_idx`: `CREATE INDEX refresh_tokens_updated_at_idx ON auth.refresh_tokens USING btree (updated_at DESC)`

### `auth.saml_providers`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `sso_provider_id` | `uuid` | NO | `` |
| `entity_id` | `text` | NO | `` |
| `metadata_xml` | `text` | NO | `` |
| `metadata_url` | `text` | YES | `` |
| `attribute_mapping` | `jsonb` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `name_id_format` | `text` | YES | `` |

Constraints:

- [PK] `saml_providers_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `saml_providers_entity_id_key`: `UNIQUE (entity_id)`
- [FK] `saml_providers_sso_provider_id_fkey`: `FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE`
- [CHECK] `entity_id not empty`: `CHECK (char_length(entity_id) > 0)`
- [CHECK] `metadata_url not empty`: `CHECK (metadata_url = NULL::text OR char_length(metadata_url) > 0)`
- [CHECK] `metadata_xml not empty`: `CHECK (char_length(metadata_xml) > 0)`

Indexes:

- [INDEX] `saml_providers_entity_id_key`: `CREATE UNIQUE INDEX saml_providers_entity_id_key ON auth.saml_providers USING btree (entity_id)`
- [PK-INDEX] `saml_providers_pkey`: `CREATE UNIQUE INDEX saml_providers_pkey ON auth.saml_providers USING btree (id)`
- [INDEX] `saml_providers_sso_provider_id_idx`: `CREATE INDEX saml_providers_sso_provider_id_idx ON auth.saml_providers USING btree (sso_provider_id)`

### `auth.saml_relay_states`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `sso_provider_id` | `uuid` | NO | `` |
| `request_id` | `text` | NO | `` |
| `for_email` | `text` | YES | `` |
| `redirect_to` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `flow_state_id` | `uuid` | YES | `` |

Constraints:

- [PK] `saml_relay_states_pkey`: `PRIMARY KEY (id)`
- [FK] `saml_relay_states_flow_state_id_fkey`: `FOREIGN KEY (flow_state_id) REFERENCES auth.flow_state(id) ON DELETE CASCADE`
- [FK] `saml_relay_states_sso_provider_id_fkey`: `FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE`
- [CHECK] `request_id not empty`: `CHECK (char_length(request_id) > 0)`

Indexes:

- [INDEX] `saml_relay_states_created_at_idx`: `CREATE INDEX saml_relay_states_created_at_idx ON auth.saml_relay_states USING btree (created_at DESC)`
- [INDEX] `saml_relay_states_for_email_idx`: `CREATE INDEX saml_relay_states_for_email_idx ON auth.saml_relay_states USING btree (for_email)`
- [PK-INDEX] `saml_relay_states_pkey`: `CREATE UNIQUE INDEX saml_relay_states_pkey ON auth.saml_relay_states USING btree (id)`
- [INDEX] `saml_relay_states_sso_provider_id_idx`: `CREATE INDEX saml_relay_states_sso_provider_id_idx ON auth.saml_relay_states USING btree (sso_provider_id)`

### `auth.schema_migrations`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `version` | `character varying(255)` | NO | `` |

Constraints:

- [PK] `schema_migrations_pkey`: `PRIMARY KEY (version)`

Indexes:

- [PK-INDEX] `schema_migrations_pkey`: `CREATE UNIQUE INDEX schema_migrations_pkey ON auth.schema_migrations USING btree (version)`

### `auth.sessions`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `user_id` | `uuid` | NO | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `factor_id` | `uuid` | YES | `` |
| `aal` | `auth.aal_level` | YES | `` |
| `not_after` | `timestamp with time zone` | YES | `` |
| `refreshed_at` | `timestamp without time zone` | YES | `` |
| `user_agent` | `text` | YES | `` |
| `ip` | `inet` | YES | `` |
| `tag` | `text` | YES | `` |
| `oauth_client_id` | `uuid` | YES | `` |
| `refresh_token_hmac_key` | `text` | YES | `` |
| `refresh_token_counter` | `bigint` | YES | `` |
| `scopes` | `text` | YES | `` |

Constraints:

- [PK] `sessions_pkey`: `PRIMARY KEY (id)`
- [FK] `sessions_oauth_client_id_fkey`: `FOREIGN KEY (oauth_client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE`
- [FK] `sessions_user_id_fkey`: `FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE`
- [CHECK] `sessions_scopes_length`: `CHECK (char_length(scopes) <= 4096)`

Indexes:

- [INDEX] `sessions_not_after_idx`: `CREATE INDEX sessions_not_after_idx ON auth.sessions USING btree (not_after DESC)`
- [INDEX] `sessions_oauth_client_id_idx`: `CREATE INDEX sessions_oauth_client_id_idx ON auth.sessions USING btree (oauth_client_id)`
- [PK-INDEX] `sessions_pkey`: `CREATE UNIQUE INDEX sessions_pkey ON auth.sessions USING btree (id)`
- [INDEX] `sessions_user_id_idx`: `CREATE INDEX sessions_user_id_idx ON auth.sessions USING btree (user_id)`
- [INDEX] `user_id_created_at_idx`: `CREATE INDEX user_id_created_at_idx ON auth.sessions USING btree (user_id, created_at)`

### `auth.sso_domains`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `sso_provider_id` | `uuid` | NO | `` |
| `domain` | `text` | NO | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |

Constraints:

- [PK] `sso_domains_pkey`: `PRIMARY KEY (id)`
- [FK] `sso_domains_sso_provider_id_fkey`: `FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE`
- [CHECK] `domain not empty`: `CHECK (char_length(domain) > 0)`

Indexes:

- [INDEX] `sso_domains_domain_idx`: `CREATE UNIQUE INDEX sso_domains_domain_idx ON auth.sso_domains USING btree (lower(domain))`
- [PK-INDEX] `sso_domains_pkey`: `CREATE UNIQUE INDEX sso_domains_pkey ON auth.sso_domains USING btree (id)`
- [INDEX] `sso_domains_sso_provider_id_idx`: `CREATE INDEX sso_domains_sso_provider_id_idx ON auth.sso_domains USING btree (sso_provider_id)`

### `auth.sso_providers`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `` |
| `resource_id` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `disabled` | `boolean` | YES | `` |

Constraints:

- [PK] `sso_providers_pkey`: `PRIMARY KEY (id)`
- [CHECK] `resource_id not empty`: `CHECK (resource_id = NULL::text OR char_length(resource_id) > 0)`

Indexes:

- [PK-INDEX] `sso_providers_pkey`: `CREATE UNIQUE INDEX sso_providers_pkey ON auth.sso_providers USING btree (id)`
- [INDEX] `sso_providers_resource_id_idx`: `CREATE UNIQUE INDEX sso_providers_resource_id_idx ON auth.sso_providers USING btree (lower(resource_id))`
- [INDEX] `sso_providers_resource_id_pattern_idx`: `CREATE INDEX sso_providers_resource_id_pattern_idx ON auth.sso_providers USING btree (resource_id text_pattern_ops)`

### `auth.users`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `instance_id` | `uuid` | YES | `` |
| `id` | `uuid` | NO | `` |
| `aud` | `character varying(255)` | YES | `` |
| `role` | `character varying(255)` | YES | `` |
| `email` | `character varying(255)` | YES | `` |
| `encrypted_password` | `character varying(255)` | YES | `` |
| `email_confirmed_at` | `timestamp with time zone` | YES | `` |
| `invited_at` | `timestamp with time zone` | YES | `` |
| `confirmation_token` | `character varying(255)` | YES | `` |
| `confirmation_sent_at` | `timestamp with time zone` | YES | `` |
| `recovery_token` | `character varying(255)` | YES | `` |
| `recovery_sent_at` | `timestamp with time zone` | YES | `` |
| `email_change_token_new` | `character varying(255)` | YES | `` |
| `email_change` | `character varying(255)` | YES | `` |
| `email_change_sent_at` | `timestamp with time zone` | YES | `` |
| `last_sign_in_at` | `timestamp with time zone` | YES | `` |
| `raw_app_meta_data` | `jsonb` | YES | `` |
| `raw_user_meta_data` | `jsonb` | YES | `` |
| `is_super_admin` | `boolean` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `` |
| `updated_at` | `timestamp with time zone` | YES | `` |
| `phone` | `text` | YES | `NULL::character varying` |
| `phone_confirmed_at` | `timestamp with time zone` | YES | `` |
| `phone_change` | `text` | YES | `''::character varying` |
| `phone_change_token` | `character varying(255)` | YES | `''::character varying` |
| `phone_change_sent_at` | `timestamp with time zone` | YES | `` |
| `confirmed_at` | `timestamp with time zone` | YES | `LEAST(email_confirmed_at, phone_confirmed_at)` |
| `email_change_token_current` | `character varying(255)` | YES | `''::character varying` |
| `email_change_confirm_status` | `smallint` | YES | `0` |
| `banned_until` | `timestamp with time zone` | YES | `` |
| `reauthentication_token` | `character varying(255)` | YES | `''::character varying` |
| `reauthentication_sent_at` | `timestamp with time zone` | YES | `` |
| `is_sso_user` | `boolean` | NO | `false` |
| `deleted_at` | `timestamp with time zone` | YES | `` |
| `is_anonymous` | `boolean` | NO | `false` |

Constraints:

- [PK] `users_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `users_phone_key`: `UNIQUE (phone)`
- [CHECK] `users_email_change_confirm_status_check`: `CHECK (email_change_confirm_status >= 0 AND email_change_confirm_status <= 2)`

Indexes:

- [INDEX] `confirmation_token_idx`: `CREATE UNIQUE INDEX confirmation_token_idx ON auth.users USING btree (confirmation_token) WHERE ((confirmation_token)::text !~ '^[0-9 ]*$'::text)`
- [INDEX] `email_change_token_current_idx`: `CREATE UNIQUE INDEX email_change_token_current_idx ON auth.users USING btree (email_change_token_current) WHERE ((email_change_token_current)::text !~ '^[0-9 ]*$'::text)`
- [INDEX] `email_change_token_new_idx`: `CREATE UNIQUE INDEX email_change_token_new_idx ON auth.users USING btree (email_change_token_new) WHERE ((email_change_token_new)::text !~ '^[0-9 ]*$'::text)`
- [INDEX] `reauthentication_token_idx`: `CREATE UNIQUE INDEX reauthentication_token_idx ON auth.users USING btree (reauthentication_token) WHERE ((reauthentication_token)::text !~ '^[0-9 ]*$'::text)`
- [INDEX] `recovery_token_idx`: `CREATE UNIQUE INDEX recovery_token_idx ON auth.users USING btree (recovery_token) WHERE ((recovery_token)::text !~ '^[0-9 ]*$'::text)`
- [INDEX] `users_email_partial_key`: `CREATE UNIQUE INDEX users_email_partial_key ON auth.users USING btree (email) WHERE (is_sso_user = false)`
- [INDEX] `users_instance_id_email_idx`: `CREATE INDEX users_instance_id_email_idx ON auth.users USING btree (instance_id, lower((email)::text))`
- [INDEX] `users_instance_id_idx`: `CREATE INDEX users_instance_id_idx ON auth.users USING btree (instance_id)`
- [INDEX] `users_is_anonymous_idx`: `CREATE INDEX users_is_anonymous_idx ON auth.users USING btree (is_anonymous)`
- [INDEX] `users_phone_key`: `CREATE UNIQUE INDEX users_phone_key ON auth.users USING btree (phone)`
- [PK-INDEX] `users_pkey`: `CREATE UNIQUE INDEX users_pkey ON auth.users USING btree (id)`

### `public.alumno`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `rut` | `character varying(20)` | NO | `` |
| `nombre` | `character varying(100)` | NO | `` |
| `apellido` | `character varying(100)` | NO | `` |
| `fecha_nacimiento` | `date` | NO | `` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `alumno_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `alumno_rut_key`: `UNIQUE (rut)`

Indexes:

- [PK-INDEX] `alumno_pkey`: `CREATE UNIQUE INDEX alumno_pkey ON public.alumno USING btree (id)`
- [INDEX] `alumno_rut_key`: `CREATE UNIQUE INDEX alumno_rut_key ON public.alumno USING btree (rut)`
- [INDEX] `idx_alumno_activo`: `CREATE INDEX idx_alumno_activo ON public.alumno USING btree (activo)`

### `public.ano_escolar`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `ano` | `integer` | NO | `` |
| `fecha_inicio` | `date` | NO | `` |
| `fecha_fin` | `date` | NO | `` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `fecha_inicio_planificacion` | `date` | NO | `` |

Constraints:

- [PK] `ano_escolar_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `uq_ano_escolar_ano`: `UNIQUE (ano)`
- [CHECK] `chk_fechas_orden`: `CHECK (fecha_inicio_planificacion < fecha_inicio AND fecha_inicio < fecha_fin)`

Indexes:

- [PK-INDEX] `ano_escolar_pkey`: `CREATE UNIQUE INDEX ano_escolar_pkey ON public.ano_escolar USING btree (id)`
- [INDEX] `uq_ano_escolar_ano`: `CREATE UNIQUE INDEX uq_ano_escolar_ano ON public.ano_escolar USING btree (ano)`

### `public.apoderado`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `nombre` | `character varying(100)` | NO | `` |
| `apellido` | `character varying(100)` | NO | `` |
| `rut` | `character varying(20)` | YES | `` |
| `email` | `character varying(255)` | YES | `` |
| `telefono` | `character varying(30)` | YES | `` |
| `created_at` | `timestamp without time zone` | NO | `now()` |
| `updated_at` | `timestamp without time zone` | NO | `now()` |

Constraints:

- [PK] `apoderado_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `apoderado_email_key`: `UNIQUE (email)`
- [UNIQUE] `apoderado_rut_key`: `UNIQUE (rut)`

Indexes:

- [INDEX] `apoderado_email_key`: `CREATE UNIQUE INDEX apoderado_email_key ON public.apoderado USING btree (email)`
- [PK-INDEX] `apoderado_pkey`: `CREATE UNIQUE INDEX apoderado_pkey ON public.apoderado USING btree (id)`
- [INDEX] `apoderado_rut_key`: `CREATE UNIQUE INDEX apoderado_rut_key ON public.apoderado USING btree (rut)`

### `public.apoderado_alumno`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `apoderado_id` | `uuid` | NO | `` |
| `alumno_id` | `uuid` | NO | `` |
| `es_principal` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `now()` |
| `vinculo` | `character varying(20)` | NO | `'OTRO'::character varying` |

Constraints:

- [PK] `apoderado_alumno_pkey`: `PRIMARY KEY (apoderado_id, alumno_id)`
- [FK] `apoderado_alumno_alumno_id_fkey`: `FOREIGN KEY (alumno_id) REFERENCES alumno(id)`
- [FK] `apoderado_alumno_apoderado_id_fkey`: `FOREIGN KEY (apoderado_id) REFERENCES apoderado(id)`
- [CHECK] `chk_apoderado_alumno_vinculo`: `CHECK (vinculo::text = ANY (ARRAY['MADRE'::character varying, 'PADRE'::character varying, 'TUTOR_LEGAL'::character varying, 'ABUELO'::character varying, 'OTRO'::character varying]::text[]))`

Indexes:

- [PK-INDEX] `apoderado_alumno_pkey`: `CREATE UNIQUE INDEX apoderado_alumno_pkey ON public.apoderado_alumno USING btree (apoderado_id, alumno_id)`
- [INDEX] `idx_apoderado_alumno_alumno_id`: `CREATE INDEX idx_apoderado_alumno_alumno_id ON public.apoderado_alumno USING btree (alumno_id)`

### `public.asistencia_clase`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `bloque_horario_id` | `uuid` | NO | `` |
| `fecha` | `date` | NO | `` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `asistencia_clase_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `uk_asistencia_clase_bloque_fecha`: `UNIQUE (bloque_horario_id, fecha)`
- [FK] `fk_asistencia_clase_bloque_horario`: `FOREIGN KEY (bloque_horario_id) REFERENCES bloque_horario(id)`

Indexes:

- [PK-INDEX] `asistencia_clase_pkey`: `CREATE UNIQUE INDEX asistencia_clase_pkey ON public.asistencia_clase USING btree (id)`
- [INDEX] `idx_asistencia_clase_bloque`: `CREATE INDEX idx_asistencia_clase_bloque ON public.asistencia_clase USING btree (bloque_horario_id)`
- [INDEX] `idx_asistencia_clase_fecha`: `CREATE INDEX idx_asistencia_clase_fecha ON public.asistencia_clase USING btree (fecha)`
- [INDEX] `uk_asistencia_clase_bloque_fecha`: `CREATE UNIQUE INDEX uk_asistencia_clase_bloque_fecha ON public.asistencia_clase USING btree (bloque_horario_id, fecha)`

### `public.bloque_horario`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `curso_id` | `uuid` | NO | `` |
| `dia_semana` | `integer` | NO | `` |
| `numero_bloque` | `integer` | NO | `` |
| `hora_inicio` | `time without time zone` | NO | `` |
| `hora_fin` | `time without time zone` | NO | `` |
| `tipo` | `character varying(20)` | NO | `` |
| `materia_id` | `uuid` | YES | `` |
| `profesor_id` | `uuid` | YES | `` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `now()` |
| `updated_at` | `timestamp without time zone` | NO | `now()` |

Constraints:

- [PK] `bloque_horario_pkey`: `PRIMARY KEY (id)`
- [FK] `bloque_horario_curso_id_fkey`: `FOREIGN KEY (curso_id) REFERENCES curso(id)`
- [FK] `bloque_horario_materia_id_fkey`: `FOREIGN KEY (materia_id) REFERENCES materia(id)`
- [FK] `bloque_horario_profesor_id_fkey`: `FOREIGN KEY (profesor_id) REFERENCES profesor(id)`
- [CHECK] `ck_bloque_dia_semana`: `CHECK (dia_semana >= 1 AND dia_semana <= 5)`
- [CHECK] `ck_bloque_hora_fin`: `CHECK (hora_fin > hora_inicio)`
- [CHECK] `ck_bloque_tipo`: `CHECK (tipo::text = ANY (ARRAY['CLASE'::character varying, 'RECREO'::character varying, 'ALMUERZO'::character varying]::text[]))`
- [CHECK] `ck_bloque_tipo_campos`: `CHECK ((tipo::text = ANY (ARRAY['RECREO'::character varying::text, 'ALMUERZO'::character varying::text])) AND materia_id IS NULL AND profesor_id IS NULL OR tipo::text = 'CLASE'::text)`

Indexes:

- [PK-INDEX] `bloque_horario_pkey`: `CREATE UNIQUE INDEX bloque_horario_pkey ON public.bloque_horario USING btree (id)`
- [INDEX] `idx_bloque_horario_curso`: `CREATE INDEX idx_bloque_horario_curso ON public.bloque_horario USING btree (curso_id)`
- [INDEX] `idx_bloque_horario_curso_activo_tipo`: `CREATE INDEX idx_bloque_horario_curso_activo_tipo ON public.bloque_horario USING btree (curso_id, activo, tipo) WHERE ((activo = true) AND ((tipo)::text = 'CLASE'::text))`
- [INDEX] `idx_bloque_horario_curso_dia`: `CREATE INDEX idx_bloque_horario_curso_dia ON public.bloque_horario USING btree (curso_id, dia_semana)`
- [INDEX] `idx_bloque_horario_profesor_dia`: `CREATE INDEX idx_bloque_horario_profesor_dia ON public.bloque_horario USING btree (profesor_id, dia_semana) WHERE ((activo = true) AND (profesor_id IS NOT NULL))`
- [INDEX] `uq_bloque_curso_dia_hora_activo`: `CREATE UNIQUE INDEX uq_bloque_curso_dia_hora_activo ON public.bloque_horario USING btree (curso_id, dia_semana, hora_inicio) WHERE (activo = true)`
- [INDEX] `uq_bloque_curso_dia_numero_activo`: `CREATE UNIQUE INDEX uq_bloque_curso_dia_numero_activo ON public.bloque_horario USING btree (curso_id, dia_semana, numero_bloque) WHERE (activo = true)`

### `public.curso`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `nombre` | `character varying(50)` | NO | `` |
| `letra` | `character varying(5)` | NO | `` |
| `grado_id` | `uuid` | NO | `` |
| `ano_escolar_id` | `uuid` | NO | `` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `curso_pkey`: `PRIMARY KEY (id)`
- [FK] `curso_ano_escolar_id_fkey`: `FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)`
- [FK] `curso_grado_id_fkey`: `FOREIGN KEY (grado_id) REFERENCES grado(id)`
- [FK] `fk_curso_seccion_catalogo`: `FOREIGN KEY (letra) REFERENCES seccion_catalogo(letra)`
- [CHECK] `ck_curso_letra_formato`: `CHECK (char_length(letra::text) = 1 AND letra::text = upper(letra::text))`

Indexes:

- [PK-INDEX] `curso_pkey`: `CREATE UNIQUE INDEX curso_pkey ON public.curso USING btree (id)`
- [INDEX] `idx_curso_activo`: `CREATE INDEX idx_curso_activo ON public.curso USING btree (activo)`
- [INDEX] `idx_curso_ano_escolar`: `CREATE INDEX idx_curso_ano_escolar ON public.curso USING btree (ano_escolar_id)`
- [INDEX] `idx_curso_grado`: `CREATE INDEX idx_curso_grado ON public.curso USING btree (grado_id)`
- [INDEX] `uq_curso_grado_ano_letra`: `CREATE UNIQUE INDEX uq_curso_grado_ano_letra ON public.curso USING btree (grado_id, ano_escolar_id, letra)`

### `public.grado`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `nombre` | `character varying(50)` | NO | `` |
| `nivel` | `integer` | NO | `` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `grado_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `uq_grado_nivel`: `UNIQUE (nivel)`
- [UNIQUE] `uq_grado_nombre`: `UNIQUE (nombre)`

Indexes:

- [PK-INDEX] `grado_pkey`: `CREATE UNIQUE INDEX grado_pkey ON public.grado USING btree (id)`
- [INDEX] `idx_grado_nivel`: `CREATE INDEX idx_grado_nivel ON public.grado USING btree (nivel)`
- [INDEX] `uq_grado_nivel`: `CREATE UNIQUE INDEX uq_grado_nivel ON public.grado USING btree (nivel)`
- [INDEX] `uq_grado_nombre`: `CREATE UNIQUE INDEX uq_grado_nombre ON public.grado USING btree (nombre)`

### `public.malla_curricular`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `materia_id` | `uuid` | NO | `` |
| `grado_id` | `uuid` | NO | `` |
| `ano_escolar_id` | `uuid` | NO | `` |
| `horas_pedagogicas` | `integer` | NO | `2` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `malla_curricular_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `uq_malla_materia_grado_ano`: `UNIQUE (materia_id, grado_id, ano_escolar_id)`
- [FK] `malla_curricular_ano_escolar_id_fkey`: `FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)`
- [FK] `malla_curricular_grado_id_fkey`: `FOREIGN KEY (grado_id) REFERENCES grado(id)`
- [FK] `malla_curricular_materia_id_fkey`: `FOREIGN KEY (materia_id) REFERENCES materia(id)`
- [CHECK] `chk_malla_horas_positivas`: `CHECK (horas_pedagogicas > 0)`

Indexes:

- [INDEX] `idx_malla_activo`: `CREATE INDEX idx_malla_activo ON public.malla_curricular USING btree (activo)`
- [INDEX] `idx_malla_ano_escolar`: `CREATE INDEX idx_malla_ano_escolar ON public.malla_curricular USING btree (ano_escolar_id)`
- [INDEX] `idx_malla_curricular_grado_ano_activo`: `CREATE INDEX idx_malla_curricular_grado_ano_activo ON public.malla_curricular USING btree (grado_id, ano_escolar_id, activo)`
- [INDEX] `idx_malla_grado`: `CREATE INDEX idx_malla_grado ON public.malla_curricular USING btree (grado_id)`
- [INDEX] `idx_malla_materia`: `CREATE INDEX idx_malla_materia ON public.malla_curricular USING btree (materia_id)`
- [PK-INDEX] `malla_curricular_pkey`: `CREATE UNIQUE INDEX malla_curricular_pkey ON public.malla_curricular USING btree (id)`
- [INDEX] `uq_malla_materia_grado_ano`: `CREATE UNIQUE INDEX uq_malla_materia_grado_ano ON public.malla_curricular USING btree (materia_id, grado_id, ano_escolar_id)`

### `public.materia`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `nombre` | `character varying(100)` | NO | `` |
| `icono` | `character varying(50)` | YES | `` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `activo` | `boolean` | NO | `true` |

Constraints:

- [PK] `materia_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `uq_materia_nombre`: `UNIQUE (nombre)`

Indexes:

- [INDEX] `idx_materia_activo`: `CREATE INDEX idx_materia_activo ON public.materia USING btree (activo)`
- [PK-INDEX] `materia_pkey`: `CREATE UNIQUE INDEX materia_pkey ON public.materia USING btree (id)`
- [INDEX] `uq_materia_nombre`: `CREATE UNIQUE INDEX uq_materia_nombre ON public.materia USING btree (nombre)`

### `public.matricula`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `alumno_id` | `uuid` | NO | `` |
| `curso_id` | `uuid` | NO | `` |
| `ano_escolar_id` | `uuid` | NO | `` |
| `fecha_matricula` | `date` | NO | `` |
| `estado` | `character varying(20)` | NO | `'ACTIVA'::character varying` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `matricula_pkey`: `PRIMARY KEY (id)`
- [FK] `matricula_alumno_id_fkey`: `FOREIGN KEY (alumno_id) REFERENCES alumno(id)`
- [FK] `matricula_ano_escolar_id_fkey`: `FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id)`
- [FK] `matricula_curso_id_fkey`: `FOREIGN KEY (curso_id) REFERENCES curso(id)`
- [CHECK] `chk_matricula_estado`: `CHECK (estado::text = ANY (ARRAY['ACTIVA'::character varying, 'RETIRADO'::character varying, 'TRASLADADO'::character varying]::text[]))`

Indexes:

- [INDEX] `idx_matricula_alumno`: `CREATE INDEX idx_matricula_alumno ON public.matricula USING btree (alumno_id)`
- [INDEX] `idx_matricula_ano_escolar`: `CREATE INDEX idx_matricula_ano_escolar ON public.matricula USING btree (ano_escolar_id)`
- [INDEX] `idx_matricula_ano_estado`: `CREATE INDEX idx_matricula_ano_estado ON public.matricula USING btree (ano_escolar_id, estado)`
- [INDEX] `idx_matricula_curso`: `CREATE INDEX idx_matricula_curso ON public.matricula USING btree (curso_id)`
- [INDEX] `idx_matricula_curso_estado`: `CREATE INDEX idx_matricula_curso_estado ON public.matricula USING btree (curso_id, estado)`
- [INDEX] `idx_matricula_estado`: `CREATE INDEX idx_matricula_estado ON public.matricula USING btree (estado)`
- [PK-INDEX] `matricula_pkey`: `CREATE UNIQUE INDEX matricula_pkey ON public.matricula USING btree (id)`
- [INDEX] `uq_matricula_alumno_ano_activa`: `CREATE UNIQUE INDEX uq_matricula_alumno_ano_activa ON public.matricula USING btree (alumno_id, ano_escolar_id) WHERE ((estado)::text = 'ACTIVA'::text)`

### `public.profesor`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `rut` | `character varying(20)` | NO | `` |
| `nombre` | `character varying(100)` | NO | `` |
| `apellido` | `character varying(100)` | NO | `` |
| `email` | `character varying(255)` | NO | `` |
| `telefono` | `character varying(30)` | YES | `` |
| `fecha_contratacion` | `date` | NO | `` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `horas_pedagogicas_contrato` | `integer` | YES | `` |

Constraints:

- [PK] `profesor_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `profesor_email_key`: `UNIQUE (email)`
- [UNIQUE] `profesor_rut_key`: `UNIQUE (rut)`
- [CHECK] `chk_profesor_horas_contrato`: `CHECK (horas_pedagogicas_contrato IS NULL OR horas_pedagogicas_contrato > 0)`

Indexes:

- [INDEX] `idx_profesor_activo`: `CREATE INDEX idx_profesor_activo ON public.profesor USING btree (activo)`
- [INDEX] `profesor_email_key`: `CREATE UNIQUE INDEX profesor_email_key ON public.profesor USING btree (email)`
- [PK-INDEX] `profesor_pkey`: `CREATE UNIQUE INDEX profesor_pkey ON public.profesor USING btree (id)`
- [INDEX] `profesor_rut_key`: `CREATE UNIQUE INDEX profesor_rut_key ON public.profesor USING btree (rut)`

### `public.profesor_materia`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `profesor_id` | `uuid` | NO | `` |
| `materia_id` | `uuid` | NO | `` |

Constraints:

- [PK] `profesor_materia_pkey`: `PRIMARY KEY (profesor_id, materia_id)`
- [FK] `profesor_materia_materia_id_fkey`: `FOREIGN KEY (materia_id) REFERENCES materia(id)`
- [FK] `profesor_materia_profesor_id_fkey`: `FOREIGN KEY (profesor_id) REFERENCES profesor(id)`

Indexes:

- [PK-INDEX] `profesor_materia_pkey`: `CREATE UNIQUE INDEX profesor_materia_pkey ON public.profesor_materia USING btree (profesor_id, materia_id)`

### `public.registro_asistencia`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `asistencia_clase_id` | `uuid` | NO | `` |
| `alumno_id` | `uuid` | NO | `` |
| `estado` | `character varying(20)` | NO | `` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `observacion` | `character varying(500)` | YES | `` |

Constraints:

- [PK] `registro_asistencia_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `uk_registro_asistencia_clase_alumno`: `UNIQUE (asistencia_clase_id, alumno_id)`
- [FK] `fk_registro_asistencia_alumno`: `FOREIGN KEY (alumno_id) REFERENCES alumno(id)`
- [FK] `fk_registro_asistencia_clase`: `FOREIGN KEY (asistencia_clase_id) REFERENCES asistencia_clase(id) ON DELETE CASCADE`
- [CHECK] `chk_registro_asistencia_estado`: `CHECK (estado::text = ANY (ARRAY['PRESENTE'::character varying, 'AUSENTE'::character varying, 'TARDANZA'::character varying, 'JUSTIFICADO'::character varying]::text[]))`

Indexes:

- [INDEX] `idx_registro_asistencia_alumno`: `CREATE INDEX idx_registro_asistencia_alumno ON public.registro_asistencia USING btree (alumno_id)`
- [INDEX] `idx_registro_asistencia_alumno_clase`: `CREATE INDEX idx_registro_asistencia_alumno_clase ON public.registro_asistencia USING btree (alumno_id, asistencia_clase_id)`
- [INDEX] `idx_registro_asistencia_clase`: `CREATE INDEX idx_registro_asistencia_clase ON public.registro_asistencia USING btree (asistencia_clase_id)`
- [PK-INDEX] `registro_asistencia_pkey`: `CREATE UNIQUE INDEX registro_asistencia_pkey ON public.registro_asistencia USING btree (id)`
- [INDEX] `uk_registro_asistencia_clase_alumno`: `CREATE UNIQUE INDEX uk_registro_asistencia_clase_alumno ON public.registro_asistencia USING btree (asistencia_clase_id, alumno_id)`

### `public.seccion_catalogo`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `letra` | `character varying(1)` | NO | `` |
| `orden` | `smallint` | NO | `` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `seccion_catalogo_pkey`: `PRIMARY KEY (letra)`
- [UNIQUE] `seccion_catalogo_orden_key`: `UNIQUE (orden)`

Indexes:

- [INDEX] `seccion_catalogo_orden_key`: `CREATE UNIQUE INDEX seccion_catalogo_orden_key ON public.seccion_catalogo USING btree (orden)`
- [PK-INDEX] `seccion_catalogo_pkey`: `CREATE UNIQUE INDEX seccion_catalogo_pkey ON public.seccion_catalogo USING btree (letra)`

### `public.usuario`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `email` | `character varying(255)` | NO | `` |
| `password_hash` | `character varying(255)` | NO | `` |
| `nombre` | `character varying(100)` | NO | `` |
| `apellido` | `character varying(100)` | NO | `` |
| `rol` | `character varying(20)` | NO | `` |
| `profesor_id` | `uuid` | YES | `` |
| `activo` | `boolean` | NO | `true` |
| `created_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp without time zone` | NO | `CURRENT_TIMESTAMP` |
| `rut` | `character varying(20)` | YES | `` |
| `apoderado_id` | `uuid` | YES | `` |

Constraints:

- [PK] `usuario_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `usuario_email_key`: `UNIQUE (email)`
- [FK] `fk_usuario_profesor`: `FOREIGN KEY (profesor_id) REFERENCES profesor(id)`
- [FK] `usuario_apoderado_id_fkey`: `FOREIGN KEY (apoderado_id) REFERENCES apoderado(id)`
- [CHECK] `chk_usuario_rol`: `CHECK (rol::text = ANY (ARRAY['ADMIN'::character varying, 'PROFESOR'::character varying, 'APODERADO'::character varying]::text[]))`

Indexes:

- [INDEX] `idx_usuario_apoderado`: `CREATE INDEX idx_usuario_apoderado ON public.usuario USING btree (apoderado_id)`
- [INDEX] `idx_usuario_profesor`: `CREATE INDEX idx_usuario_profesor ON public.usuario USING btree (profesor_id) WHERE (profesor_id IS NOT NULL)`
- [INDEX] `idx_usuario_rol`: `CREATE INDEX idx_usuario_rol ON public.usuario USING btree (rol)`
- [INDEX] `usuario_email_key`: `CREATE UNIQUE INDEX usuario_email_key ON public.usuario USING btree (email)`
- [PK-INDEX] `usuario_pkey`: `CREATE UNIQUE INDEX usuario_pkey ON public.usuario USING btree (id)`
- [INDEX] `ux_usuario_rut_not_null`: `CREATE UNIQUE INDEX ux_usuario_rut_not_null ON public.usuario USING btree (rut) WHERE (rut IS NOT NULL)`

### `realtime.schema_migrations`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `version` | `bigint` | NO | `` |
| `inserted_at` | `timestamp(0) without time zone` | YES | `` |

Constraints:

- [PK] `schema_migrations_pkey`: `PRIMARY KEY (version)`

Indexes:

- [PK-INDEX] `schema_migrations_pkey`: `CREATE UNIQUE INDEX schema_migrations_pkey ON realtime.schema_migrations USING btree (version)`

### `realtime.subscription`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `bigint` | NO | `` |
| `subscription_id` | `uuid` | NO | `` |
| `entity` | `regclass` | NO | `` |
| `filters` | `realtime.user_defined_filter[]` | NO | `'{}'::realtime.user_defined_filter[]` |
| `claims` | `jsonb` | NO | `` |
| `claims_role` | `regrole` | NO | `realtime.to_regrole((claims ->> 'role'::text))` |
| `created_at` | `timestamp without time zone` | NO | `timezone('utc'::text, now())` |
| `action_filter` | `text` | YES | `'*'::text` |

Constraints:

- [PK] `pk_subscription`: `PRIMARY KEY (id)`
- [CHECK] `subscription_action_filter_check`: `CHECK (action_filter = ANY (ARRAY['*'::text, 'INSERT'::text, 'UPDATE'::text, 'DELETE'::text]))`

Indexes:

- [INDEX] `ix_realtime_subscription_entity`: `CREATE INDEX ix_realtime_subscription_entity ON realtime.subscription USING btree (entity)`
- [PK-INDEX] `pk_subscription`: `CREATE UNIQUE INDEX pk_subscription ON realtime.subscription USING btree (id)`
- [INDEX] `subscription_subscription_id_entity_filters_action_filter_key`: `CREATE UNIQUE INDEX subscription_subscription_id_entity_filters_action_filter_key ON realtime.subscription USING btree (subscription_id, entity, filters, action_filter)`

### `storage.buckets`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `text` | NO | `` |
| `name` | `text` | NO | `` |
| `owner` | `uuid` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `now()` |
| `updated_at` | `timestamp with time zone` | YES | `now()` |
| `public` | `boolean` | YES | `false` |
| `avif_autodetection` | `boolean` | YES | `false` |
| `file_size_limit` | `bigint` | YES | `` |
| `allowed_mime_types` | `text[]` | YES | `` |
| `owner_id` | `text` | YES | `` |
| `type` | `storage.buckettype` | NO | `'STANDARD'::storage.buckettype` |

Constraints:

- [PK] `buckets_pkey`: `PRIMARY KEY (id)`

Indexes:

- [INDEX] `bname`: `CREATE UNIQUE INDEX bname ON storage.buckets USING btree (name)`
- [PK-INDEX] `buckets_pkey`: `CREATE UNIQUE INDEX buckets_pkey ON storage.buckets USING btree (id)`

### `storage.buckets_analytics`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `name` | `text` | NO | `` |
| `type` | `storage.buckettype` | NO | `'ANALYTICS'::storage.buckettype` |
| `format` | `text` | NO | `'ICEBERG'::text` |
| `created_at` | `timestamp with time zone` | NO | `now()` |
| `updated_at` | `timestamp with time zone` | NO | `now()` |
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `deleted_at` | `timestamp with time zone` | YES | `` |

Constraints:

- [PK] `buckets_analytics_pkey`: `PRIMARY KEY (id)`

Indexes:

- [PK-INDEX] `buckets_analytics_pkey`: `CREATE UNIQUE INDEX buckets_analytics_pkey ON storage.buckets_analytics USING btree (id)`
- [INDEX] `buckets_analytics_unique_name_idx`: `CREATE UNIQUE INDEX buckets_analytics_unique_name_idx ON storage.buckets_analytics USING btree (name) WHERE (deleted_at IS NULL)`

### `storage.buckets_vectors`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `text` | NO | `` |
| `type` | `storage.buckettype` | NO | `'VECTOR'::storage.buckettype` |
| `created_at` | `timestamp with time zone` | NO | `now()` |
| `updated_at` | `timestamp with time zone` | NO | `now()` |

Constraints:

- [PK] `buckets_vectors_pkey`: `PRIMARY KEY (id)`

Indexes:

- [PK-INDEX] `buckets_vectors_pkey`: `CREATE UNIQUE INDEX buckets_vectors_pkey ON storage.buckets_vectors USING btree (id)`

### `storage.migrations`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `integer` | NO | `` |
| `name` | `character varying(100)` | NO | `` |
| `hash` | `character varying(40)` | NO | `` |
| `executed_at` | `timestamp without time zone` | YES | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `migrations_pkey`: `PRIMARY KEY (id)`
- [UNIQUE] `migrations_name_key`: `UNIQUE (name)`

Indexes:

- [INDEX] `migrations_name_key`: `CREATE UNIQUE INDEX migrations_name_key ON storage.migrations USING btree (name)`
- [PK-INDEX] `migrations_pkey`: `CREATE UNIQUE INDEX migrations_pkey ON storage.migrations USING btree (id)`

### `storage.objects`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `bucket_id` | `text` | YES | `` |
| `name` | `text` | YES | `` |
| `owner` | `uuid` | YES | `` |
| `created_at` | `timestamp with time zone` | YES | `now()` |
| `updated_at` | `timestamp with time zone` | YES | `now()` |
| `last_accessed_at` | `timestamp with time zone` | YES | `now()` |
| `metadata` | `jsonb` | YES | `` |
| `path_tokens` | `text[]` | YES | `string_to_array(name, '/'::text)` |
| `version` | `text` | YES | `` |
| `owner_id` | `text` | YES | `` |
| `user_metadata` | `jsonb` | YES | `` |

Constraints:

- [PK] `objects_pkey`: `PRIMARY KEY (id)`
- [FK] `objects_bucketId_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id)`

Indexes:

- [INDEX] `bucketid_objname`: `CREATE UNIQUE INDEX bucketid_objname ON storage.objects USING btree (bucket_id, name)`
- [INDEX] `idx_objects_bucket_id_name`: `CREATE INDEX idx_objects_bucket_id_name ON storage.objects USING btree (bucket_id, name COLLATE "C")`
- [INDEX] `idx_objects_bucket_id_name_lower`: `CREATE INDEX idx_objects_bucket_id_name_lower ON storage.objects USING btree (bucket_id, lower(name) COLLATE "C")`
- [INDEX] `name_prefix_search`: `CREATE INDEX name_prefix_search ON storage.objects USING btree (name text_pattern_ops)`
- [PK-INDEX] `objects_pkey`: `CREATE UNIQUE INDEX objects_pkey ON storage.objects USING btree (id)`

### `storage.s3_multipart_uploads`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `text` | NO | `` |
| `in_progress_size` | `bigint` | NO | `0` |
| `upload_signature` | `text` | NO | `` |
| `bucket_id` | `text` | NO | `` |
| `key` | `text` | NO | `` |
| `version` | `text` | NO | `` |
| `owner_id` | `text` | YES | `` |
| `created_at` | `timestamp with time zone` | NO | `now()` |
| `user_metadata` | `jsonb` | YES | `` |

Constraints:

- [PK] `s3_multipart_uploads_pkey`: `PRIMARY KEY (id)`
- [FK] `s3_multipart_uploads_bucket_id_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id)`

Indexes:

- [INDEX] `idx_multipart_uploads_list`: `CREATE INDEX idx_multipart_uploads_list ON storage.s3_multipart_uploads USING btree (bucket_id, key, created_at)`
- [PK-INDEX] `s3_multipart_uploads_pkey`: `CREATE UNIQUE INDEX s3_multipart_uploads_pkey ON storage.s3_multipart_uploads USING btree (id)`

### `storage.s3_multipart_uploads_parts`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `upload_id` | `text` | NO | `` |
| `size` | `bigint` | NO | `0` |
| `part_number` | `integer` | NO | `` |
| `bucket_id` | `text` | NO | `` |
| `key` | `text` | NO | `` |
| `etag` | `text` | NO | `` |
| `owner_id` | `text` | YES | `` |
| `version` | `text` | NO | `` |
| `created_at` | `timestamp with time zone` | NO | `now()` |

Constraints:

- [PK] `s3_multipart_uploads_parts_pkey`: `PRIMARY KEY (id)`
- [FK] `s3_multipart_uploads_parts_bucket_id_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id)`
- [FK] `s3_multipart_uploads_parts_upload_id_fkey`: `FOREIGN KEY (upload_id) REFERENCES storage.s3_multipart_uploads(id) ON DELETE CASCADE`

Indexes:

- [PK-INDEX] `s3_multipart_uploads_parts_pkey`: `CREATE UNIQUE INDEX s3_multipart_uploads_parts_pkey ON storage.s3_multipart_uploads_parts USING btree (id)`

### `storage.vector_indexes`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `text` | NO | `gen_random_uuid()` |
| `name` | `text` | NO | `` |
| `bucket_id` | `text` | NO | `` |
| `data_type` | `text` | NO | `` |
| `dimension` | `integer` | NO | `` |
| `distance_metric` | `text` | NO | `` |
| `metadata_configuration` | `jsonb` | YES | `` |
| `created_at` | `timestamp with time zone` | NO | `now()` |
| `updated_at` | `timestamp with time zone` | NO | `now()` |

Constraints:

- [PK] `vector_indexes_pkey`: `PRIMARY KEY (id)`
- [FK] `vector_indexes_bucket_id_fkey`: `FOREIGN KEY (bucket_id) REFERENCES storage.buckets_vectors(id)`

Indexes:

- [INDEX] `vector_indexes_name_bucket_id_idx`: `CREATE UNIQUE INDEX vector_indexes_name_bucket_id_idx ON storage.vector_indexes USING btree (name, bucket_id)`
- [PK-INDEX] `vector_indexes_pkey`: `CREATE UNIQUE INDEX vector_indexes_pkey ON storage.vector_indexes USING btree (id)`

### `vault.secrets`

Columns:

| Name | Type | Nullable | Default |
|---|---|---|---|
| `id` | `uuid` | NO | `gen_random_uuid()` |
| `name` | `text` | YES | `` |
| `description` | `text` | NO | `''::text` |
| `secret` | `text` | NO | `` |
| `key_id` | `uuid` | YES | `` |
| `nonce` | `bytea` | YES | `vault._crypto_aead_det_noncegen()` |
| `created_at` | `timestamp with time zone` | NO | `CURRENT_TIMESTAMP` |
| `updated_at` | `timestamp with time zone` | NO | `CURRENT_TIMESTAMP` |

Constraints:

- [PK] `secrets_pkey`: `PRIMARY KEY (id)`

Indexes:

- [INDEX] `secrets_name_idx`: `CREATE UNIQUE INDEX secrets_name_idx ON vault.secrets USING btree (name) WHERE (name IS NOT NULL)`
- [PK-INDEX] `secrets_pkey`: `CREATE UNIQUE INDEX secrets_pkey ON vault.secrets USING btree (id)`

