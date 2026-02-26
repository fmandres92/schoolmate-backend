-- Auto-generated schema snapshot from live PostgreSQL
-- Generated at: 2026-02-26T20:36:53.303612-03:00

-- Table: auth.audit_log_entries
CREATE TABLE "auth"."audit_log_entries" (
    "instance_id" uuid,
    "id" uuid NOT NULL,
    "payload" json,
    "created_at" timestamp with time zone,
    "ip_address" character varying(64) DEFAULT ''::character varying NOT NULL
);

ALTER TABLE ONLY "auth"."audit_log_entries" ADD CONSTRAINT "audit_log_entries_pkey" PRIMARY KEY (id);

CREATE INDEX audit_logs_instance_id_idx ON auth.audit_log_entries USING btree (instance_id);

-- Table: auth.custom_oauth_providers
CREATE TABLE "auth"."custom_oauth_providers" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "provider_type" text NOT NULL,
    "identifier" text NOT NULL,
    "name" text NOT NULL,
    "client_id" text NOT NULL,
    "client_secret" text NOT NULL,
    "acceptable_client_ids" text[] DEFAULT '{}'::text[] NOT NULL,
    "scopes" text[] DEFAULT '{}'::text[] NOT NULL,
    "pkce_enabled" boolean DEFAULT true NOT NULL,
    "attribute_mapping" jsonb DEFAULT '{}'::jsonb NOT NULL,
    "authorization_params" jsonb DEFAULT '{}'::jsonb NOT NULL,
    "enabled" boolean DEFAULT true NOT NULL,
    "email_optional" boolean DEFAULT false NOT NULL,
    "issuer" text,
    "discovery_url" text,
    "skip_nonce_check" boolean DEFAULT false NOT NULL,
    "cached_discovery" jsonb,
    "discovery_cached_at" timestamp with time zone,
    "authorization_url" text,
    "token_url" text,
    "userinfo_url" text,
    "jwks_uri" text,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_identifier_key" UNIQUE (identifier);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_authorization_url_https" CHECK (authorization_url IS NULL OR authorization_url ~~ 'https://%'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_authorization_url_length" CHECK (authorization_url IS NULL OR char_length(authorization_url) <= 2048);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_client_id_length" CHECK (char_length(client_id) >= 1 AND char_length(client_id) <= 512);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_discovery_url_length" CHECK (discovery_url IS NULL OR char_length(discovery_url) <= 2048);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_identifier_format" CHECK (identifier ~ '^[a-z0-9][a-z0-9:-]{0,48}[a-z0-9]$'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_issuer_length" CHECK (issuer IS NULL OR char_length(issuer) >= 1 AND char_length(issuer) <= 2048);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_jwks_uri_https" CHECK (jwks_uri IS NULL OR jwks_uri ~~ 'https://%'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_jwks_uri_length" CHECK (jwks_uri IS NULL OR char_length(jwks_uri) <= 2048);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_name_length" CHECK (char_length(name) >= 1 AND char_length(name) <= 100);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_oauth2_requires_endpoints" CHECK (provider_type <> 'oauth2'::text OR authorization_url IS NOT NULL AND token_url IS NOT NULL AND userinfo_url IS NOT NULL);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_oidc_discovery_url_https" CHECK (provider_type <> 'oidc'::text OR discovery_url IS NULL OR discovery_url ~~ 'https://%'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_oidc_issuer_https" CHECK (provider_type <> 'oidc'::text OR issuer IS NULL OR issuer ~~ 'https://%'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_oidc_requires_issuer" CHECK (provider_type <> 'oidc'::text OR issuer IS NOT NULL);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_provider_type_check" CHECK (provider_type = ANY (ARRAY['oauth2'::text, 'oidc'::text]));
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_token_url_https" CHECK (token_url IS NULL OR token_url ~~ 'https://%'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_token_url_length" CHECK (token_url IS NULL OR char_length(token_url) <= 2048);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_userinfo_url_https" CHECK (userinfo_url IS NULL OR userinfo_url ~~ 'https://%'::text);
ALTER TABLE ONLY "auth"."custom_oauth_providers" ADD CONSTRAINT "custom_oauth_providers_userinfo_url_length" CHECK (userinfo_url IS NULL OR char_length(userinfo_url) <= 2048);

CREATE INDEX custom_oauth_providers_created_at_idx ON auth.custom_oauth_providers USING btree (created_at);
CREATE INDEX custom_oauth_providers_enabled_idx ON auth.custom_oauth_providers USING btree (enabled);
CREATE INDEX custom_oauth_providers_identifier_idx ON auth.custom_oauth_providers USING btree (identifier);
CREATE UNIQUE INDEX custom_oauth_providers_identifier_key ON auth.custom_oauth_providers USING btree (identifier);
CREATE INDEX custom_oauth_providers_provider_type_idx ON auth.custom_oauth_providers USING btree (provider_type);

-- Table: auth.flow_state
CREATE TABLE "auth"."flow_state" (
    "id" uuid NOT NULL,
    "user_id" uuid,
    "auth_code" text,
    "code_challenge_method" auth.code_challenge_method,
    "code_challenge" text,
    "provider_type" text NOT NULL,
    "provider_access_token" text,
    "provider_refresh_token" text,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "authentication_method" text NOT NULL,
    "auth_code_issued_at" timestamp with time zone,
    "invite_token" text,
    "referrer" text,
    "oauth_client_state_id" uuid,
    "linking_target_id" uuid,
    "email_optional" boolean DEFAULT false NOT NULL
);

ALTER TABLE ONLY "auth"."flow_state" ADD CONSTRAINT "flow_state_pkey" PRIMARY KEY (id);

CREATE INDEX flow_state_created_at_idx ON auth.flow_state USING btree (created_at DESC);
CREATE INDEX idx_auth_code ON auth.flow_state USING btree (auth_code);
CREATE INDEX idx_user_id_auth_method ON auth.flow_state USING btree (user_id, authentication_method);

-- Table: auth.identities
CREATE TABLE "auth"."identities" (
    "provider_id" text NOT NULL,
    "user_id" uuid NOT NULL,
    "identity_data" jsonb NOT NULL,
    "provider" text NOT NULL,
    "last_sign_in_at" timestamp with time zone,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "email" text DEFAULT lower((identity_data ->> 'email'::text)),
    "id" uuid DEFAULT gen_random_uuid() NOT NULL
);

ALTER TABLE ONLY "auth"."identities" ADD CONSTRAINT "identities_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."identities" ADD CONSTRAINT "identities_provider_id_provider_unique" UNIQUE (provider_id, provider);
ALTER TABLE ONLY "auth"."identities" ADD CONSTRAINT "identities_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

CREATE INDEX identities_email_idx ON auth.identities USING btree (email text_pattern_ops);
CREATE UNIQUE INDEX identities_provider_id_provider_unique ON auth.identities USING btree (provider_id, provider);
CREATE INDEX identities_user_id_idx ON auth.identities USING btree (user_id);

-- Table: auth.instances
CREATE TABLE "auth"."instances" (
    "id" uuid NOT NULL,
    "uuid" uuid,
    "raw_base_config" text,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone
);

ALTER TABLE ONLY "auth"."instances" ADD CONSTRAINT "instances_pkey" PRIMARY KEY (id);


-- Table: auth.mfa_amr_claims
CREATE TABLE "auth"."mfa_amr_claims" (
    "session_id" uuid NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "authentication_method" text NOT NULL,
    "id" uuid NOT NULL
);

ALTER TABLE ONLY "auth"."mfa_amr_claims" ADD CONSTRAINT "amr_id_pk" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."mfa_amr_claims" ADD CONSTRAINT "mfa_amr_claims_session_id_authentication_method_pkey" UNIQUE (session_id, authentication_method);
ALTER TABLE ONLY "auth"."mfa_amr_claims" ADD CONSTRAINT "mfa_amr_claims_session_id_fkey" FOREIGN KEY (session_id) REFERENCES auth.sessions(id) ON DELETE CASCADE;

CREATE UNIQUE INDEX mfa_amr_claims_session_id_authentication_method_pkey ON auth.mfa_amr_claims USING btree (session_id, authentication_method);

-- Table: auth.mfa_challenges
CREATE TABLE "auth"."mfa_challenges" (
    "id" uuid NOT NULL,
    "factor_id" uuid NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "verified_at" timestamp with time zone,
    "ip_address" inet NOT NULL,
    "otp_code" text,
    "web_authn_session_data" jsonb
);

ALTER TABLE ONLY "auth"."mfa_challenges" ADD CONSTRAINT "mfa_challenges_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."mfa_challenges" ADD CONSTRAINT "mfa_challenges_auth_factor_id_fkey" FOREIGN KEY (factor_id) REFERENCES auth.mfa_factors(id) ON DELETE CASCADE;

CREATE INDEX mfa_challenge_created_at_idx ON auth.mfa_challenges USING btree (created_at DESC);

-- Table: auth.mfa_factors
CREATE TABLE "auth"."mfa_factors" (
    "id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "friendly_name" text,
    "factor_type" auth.factor_type NOT NULL,
    "status" auth.factor_status NOT NULL,
    "created_at" timestamp with time zone NOT NULL,
    "updated_at" timestamp with time zone NOT NULL,
    "secret" text,
    "phone" text,
    "last_challenged_at" timestamp with time zone,
    "web_authn_credential" jsonb,
    "web_authn_aaguid" uuid,
    "last_webauthn_challenge_data" jsonb
);

ALTER TABLE ONLY "auth"."mfa_factors" ADD CONSTRAINT "mfa_factors_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."mfa_factors" ADD CONSTRAINT "mfa_factors_last_challenged_at_key" UNIQUE (last_challenged_at);
ALTER TABLE ONLY "auth"."mfa_factors" ADD CONSTRAINT "mfa_factors_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;

CREATE INDEX factor_id_created_at_idx ON auth.mfa_factors USING btree (user_id, created_at);
CREATE UNIQUE INDEX mfa_factors_last_challenged_at_key ON auth.mfa_factors USING btree (last_challenged_at);
CREATE UNIQUE INDEX mfa_factors_user_friendly_name_unique ON auth.mfa_factors USING btree (friendly_name, user_id) WHERE (TRIM(BOTH FROM friendly_name) <> ''::text);
CREATE INDEX mfa_factors_user_id_idx ON auth.mfa_factors USING btree (user_id);
CREATE UNIQUE INDEX unique_phone_factor_per_user ON auth.mfa_factors USING btree (user_id, phone);

-- Table: auth.oauth_authorizations
CREATE TABLE "auth"."oauth_authorizations" (
    "id" uuid NOT NULL,
    "authorization_id" text NOT NULL,
    "client_id" uuid NOT NULL,
    "user_id" uuid,
    "redirect_uri" text NOT NULL,
    "scope" text NOT NULL,
    "state" text,
    "resource" text,
    "code_challenge" text,
    "code_challenge_method" auth.code_challenge_method,
    "response_type" auth.oauth_response_type DEFAULT 'code'::auth.oauth_response_type NOT NULL,
    "status" auth.oauth_authorization_status DEFAULT 'pending'::auth.oauth_authorization_status NOT NULL,
    "authorization_code" text,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "expires_at" timestamp with time zone DEFAULT (now() + '00:03:00'::interval) NOT NULL,
    "approved_at" timestamp with time zone,
    "nonce" text
);

ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_authorization_code_key" UNIQUE (authorization_code);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_authorization_id_key" UNIQUE (authorization_id);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_client_id_fkey" FOREIGN KEY (client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_authorization_code_length" CHECK (char_length(authorization_code) <= 255);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_code_challenge_length" CHECK (char_length(code_challenge) <= 128);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_expires_at_future" CHECK (expires_at > created_at);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_nonce_length" CHECK (char_length(nonce) <= 255);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_redirect_uri_length" CHECK (char_length(redirect_uri) <= 2048);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_resource_length" CHECK (char_length(resource) <= 2048);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_scope_length" CHECK (char_length(scope) <= 4096);
ALTER TABLE ONLY "auth"."oauth_authorizations" ADD CONSTRAINT "oauth_authorizations_state_length" CHECK (char_length(state) <= 4096);

CREATE INDEX oauth_auth_pending_exp_idx ON auth.oauth_authorizations USING btree (expires_at) WHERE (status = 'pending'::auth.oauth_authorization_status);
CREATE UNIQUE INDEX oauth_authorizations_authorization_code_key ON auth.oauth_authorizations USING btree (authorization_code);
CREATE UNIQUE INDEX oauth_authorizations_authorization_id_key ON auth.oauth_authorizations USING btree (authorization_id);

-- Table: auth.oauth_client_states
CREATE TABLE "auth"."oauth_client_states" (
    "id" uuid NOT NULL,
    "provider_type" text NOT NULL,
    "code_verifier" text,
    "created_at" timestamp with time zone NOT NULL
);

ALTER TABLE ONLY "auth"."oauth_client_states" ADD CONSTRAINT "oauth_client_states_pkey" PRIMARY KEY (id);

CREATE INDEX idx_oauth_client_states_created_at ON auth.oauth_client_states USING btree (created_at);

-- Table: auth.oauth_clients
CREATE TABLE "auth"."oauth_clients" (
    "id" uuid NOT NULL,
    "client_secret_hash" text,
    "registration_type" auth.oauth_registration_type NOT NULL,
    "redirect_uris" text NOT NULL,
    "grant_types" text NOT NULL,
    "client_name" text,
    "client_uri" text,
    "logo_uri" text,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
    "deleted_at" timestamp with time zone,
    "client_type" auth.oauth_client_type DEFAULT 'confidential'::auth.oauth_client_type NOT NULL,
    "token_endpoint_auth_method" text NOT NULL
);

ALTER TABLE ONLY "auth"."oauth_clients" ADD CONSTRAINT "oauth_clients_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."oauth_clients" ADD CONSTRAINT "oauth_clients_client_name_length" CHECK (char_length(client_name) <= 1024);
ALTER TABLE ONLY "auth"."oauth_clients" ADD CONSTRAINT "oauth_clients_client_uri_length" CHECK (char_length(client_uri) <= 2048);
ALTER TABLE ONLY "auth"."oauth_clients" ADD CONSTRAINT "oauth_clients_logo_uri_length" CHECK (char_length(logo_uri) <= 2048);
ALTER TABLE ONLY "auth"."oauth_clients" ADD CONSTRAINT "oauth_clients_token_endpoint_auth_method_check" CHECK (token_endpoint_auth_method = ANY (ARRAY['client_secret_basic'::text, 'client_secret_post'::text, 'none'::text]));

CREATE INDEX oauth_clients_deleted_at_idx ON auth.oauth_clients USING btree (deleted_at);

-- Table: auth.oauth_consents
CREATE TABLE "auth"."oauth_consents" (
    "id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "client_id" uuid NOT NULL,
    "scopes" text NOT NULL,
    "granted_at" timestamp with time zone DEFAULT now() NOT NULL,
    "revoked_at" timestamp with time zone
);

ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_user_client_unique" UNIQUE (user_id, client_id);
ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_client_id_fkey" FOREIGN KEY (client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_revoked_after_granted" CHECK (revoked_at IS NULL OR revoked_at >= granted_at);
ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_scopes_length" CHECK (char_length(scopes) <= 2048);
ALTER TABLE ONLY "auth"."oauth_consents" ADD CONSTRAINT "oauth_consents_scopes_not_empty" CHECK (char_length(TRIM(BOTH FROM scopes)) > 0);

CREATE INDEX oauth_consents_active_client_idx ON auth.oauth_consents USING btree (client_id) WHERE (revoked_at IS NULL);
CREATE INDEX oauth_consents_active_user_client_idx ON auth.oauth_consents USING btree (user_id, client_id) WHERE (revoked_at IS NULL);
CREATE UNIQUE INDEX oauth_consents_user_client_unique ON auth.oauth_consents USING btree (user_id, client_id);
CREATE INDEX oauth_consents_user_order_idx ON auth.oauth_consents USING btree (user_id, granted_at DESC);

-- Table: auth.one_time_tokens
CREATE TABLE "auth"."one_time_tokens" (
    "id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "token_type" auth.one_time_token_type NOT NULL,
    "token_hash" text NOT NULL,
    "relates_to" text NOT NULL,
    "created_at" timestamp without time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "auth"."one_time_tokens" ADD CONSTRAINT "one_time_tokens_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."one_time_tokens" ADD CONSTRAINT "one_time_tokens_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."one_time_tokens" ADD CONSTRAINT "one_time_tokens_token_hash_check" CHECK (char_length(token_hash) > 0);

CREATE INDEX one_time_tokens_relates_to_hash_idx ON auth.one_time_tokens USING hash (relates_to);
CREATE INDEX one_time_tokens_token_hash_hash_idx ON auth.one_time_tokens USING hash (token_hash);
CREATE UNIQUE INDEX one_time_tokens_user_id_token_type_key ON auth.one_time_tokens USING btree (user_id, token_type);

-- Table: auth.refresh_tokens
CREATE TABLE "auth"."refresh_tokens" (
    "instance_id" uuid,
    "id" bigint DEFAULT nextval('auth.refresh_tokens_id_seq'::regclass) NOT NULL,
    "token" character varying(255),
    "user_id" character varying(255),
    "revoked" boolean,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "parent" character varying(255),
    "session_id" uuid
);

ALTER TABLE ONLY "auth"."refresh_tokens" ADD CONSTRAINT "refresh_tokens_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."refresh_tokens" ADD CONSTRAINT "refresh_tokens_token_unique" UNIQUE (token);
ALTER TABLE ONLY "auth"."refresh_tokens" ADD CONSTRAINT "refresh_tokens_session_id_fkey" FOREIGN KEY (session_id) REFERENCES auth.sessions(id) ON DELETE CASCADE;

CREATE INDEX refresh_tokens_instance_id_idx ON auth.refresh_tokens USING btree (instance_id);
CREATE INDEX refresh_tokens_instance_id_user_id_idx ON auth.refresh_tokens USING btree (instance_id, user_id);
CREATE INDEX refresh_tokens_parent_idx ON auth.refresh_tokens USING btree (parent);
CREATE INDEX refresh_tokens_session_id_revoked_idx ON auth.refresh_tokens USING btree (session_id, revoked);
CREATE UNIQUE INDEX refresh_tokens_token_unique ON auth.refresh_tokens USING btree (token);
CREATE INDEX refresh_tokens_updated_at_idx ON auth.refresh_tokens USING btree (updated_at DESC);

-- Table: auth.saml_providers
CREATE TABLE "auth"."saml_providers" (
    "id" uuid NOT NULL,
    "sso_provider_id" uuid NOT NULL,
    "entity_id" text NOT NULL,
    "metadata_xml" text NOT NULL,
    "metadata_url" text,
    "attribute_mapping" jsonb,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "name_id_format" text
);

ALTER TABLE ONLY "auth"."saml_providers" ADD CONSTRAINT "saml_providers_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."saml_providers" ADD CONSTRAINT "saml_providers_entity_id_key" UNIQUE (entity_id);
ALTER TABLE ONLY "auth"."saml_providers" ADD CONSTRAINT "saml_providers_sso_provider_id_fkey" FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."saml_providers" ADD CONSTRAINT "entity_id not empty" CHECK (char_length(entity_id) > 0);
ALTER TABLE ONLY "auth"."saml_providers" ADD CONSTRAINT "metadata_url not empty" CHECK (metadata_url = NULL::text OR char_length(metadata_url) > 0);
ALTER TABLE ONLY "auth"."saml_providers" ADD CONSTRAINT "metadata_xml not empty" CHECK (char_length(metadata_xml) > 0);

CREATE UNIQUE INDEX saml_providers_entity_id_key ON auth.saml_providers USING btree (entity_id);
CREATE INDEX saml_providers_sso_provider_id_idx ON auth.saml_providers USING btree (sso_provider_id);

-- Table: auth.saml_relay_states
CREATE TABLE "auth"."saml_relay_states" (
    "id" uuid NOT NULL,
    "sso_provider_id" uuid NOT NULL,
    "request_id" text NOT NULL,
    "for_email" text,
    "redirect_to" text,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "flow_state_id" uuid
);

ALTER TABLE ONLY "auth"."saml_relay_states" ADD CONSTRAINT "saml_relay_states_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."saml_relay_states" ADD CONSTRAINT "saml_relay_states_flow_state_id_fkey" FOREIGN KEY (flow_state_id) REFERENCES auth.flow_state(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."saml_relay_states" ADD CONSTRAINT "saml_relay_states_sso_provider_id_fkey" FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."saml_relay_states" ADD CONSTRAINT "request_id not empty" CHECK (char_length(request_id) > 0);

CREATE INDEX saml_relay_states_created_at_idx ON auth.saml_relay_states USING btree (created_at DESC);
CREATE INDEX saml_relay_states_for_email_idx ON auth.saml_relay_states USING btree (for_email);
CREATE INDEX saml_relay_states_sso_provider_id_idx ON auth.saml_relay_states USING btree (sso_provider_id);

-- Table: auth.schema_migrations
CREATE TABLE "auth"."schema_migrations" (
    "version" character varying(255) NOT NULL
);

ALTER TABLE ONLY "auth"."schema_migrations" ADD CONSTRAINT "schema_migrations_pkey" PRIMARY KEY (version);


-- Table: auth.sessions
CREATE TABLE "auth"."sessions" (
    "id" uuid NOT NULL,
    "user_id" uuid NOT NULL,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "factor_id" uuid,
    "aal" auth.aal_level,
    "not_after" timestamp with time zone,
    "refreshed_at" timestamp without time zone,
    "user_agent" text,
    "ip" inet,
    "tag" text,
    "oauth_client_id" uuid,
    "refresh_token_hmac_key" text,
    "refresh_token_counter" bigint,
    "scopes" text
);

ALTER TABLE ONLY "auth"."sessions" ADD CONSTRAINT "sessions_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."sessions" ADD CONSTRAINT "sessions_oauth_client_id_fkey" FOREIGN KEY (oauth_client_id) REFERENCES auth.oauth_clients(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."sessions" ADD CONSTRAINT "sessions_user_id_fkey" FOREIGN KEY (user_id) REFERENCES auth.users(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."sessions" ADD CONSTRAINT "sessions_scopes_length" CHECK (char_length(scopes) <= 4096);

CREATE INDEX sessions_not_after_idx ON auth.sessions USING btree (not_after DESC);
CREATE INDEX sessions_oauth_client_id_idx ON auth.sessions USING btree (oauth_client_id);
CREATE INDEX sessions_user_id_idx ON auth.sessions USING btree (user_id);
CREATE INDEX user_id_created_at_idx ON auth.sessions USING btree (user_id, created_at);

-- Table: auth.sso_domains
CREATE TABLE "auth"."sso_domains" (
    "id" uuid NOT NULL,
    "sso_provider_id" uuid NOT NULL,
    "domain" text NOT NULL,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone
);

ALTER TABLE ONLY "auth"."sso_domains" ADD CONSTRAINT "sso_domains_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."sso_domains" ADD CONSTRAINT "sso_domains_sso_provider_id_fkey" FOREIGN KEY (sso_provider_id) REFERENCES auth.sso_providers(id) ON DELETE CASCADE;
ALTER TABLE ONLY "auth"."sso_domains" ADD CONSTRAINT "domain not empty" CHECK (char_length(domain) > 0);

CREATE UNIQUE INDEX sso_domains_domain_idx ON auth.sso_domains USING btree (lower(domain));
CREATE INDEX sso_domains_sso_provider_id_idx ON auth.sso_domains USING btree (sso_provider_id);

-- Table: auth.sso_providers
CREATE TABLE "auth"."sso_providers" (
    "id" uuid NOT NULL,
    "resource_id" text,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "disabled" boolean
);

ALTER TABLE ONLY "auth"."sso_providers" ADD CONSTRAINT "sso_providers_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."sso_providers" ADD CONSTRAINT "resource_id not empty" CHECK (resource_id = NULL::text OR char_length(resource_id) > 0);

CREATE UNIQUE INDEX sso_providers_resource_id_idx ON auth.sso_providers USING btree (lower(resource_id));
CREATE INDEX sso_providers_resource_id_pattern_idx ON auth.sso_providers USING btree (resource_id text_pattern_ops);

-- Table: auth.users
CREATE TABLE "auth"."users" (
    "instance_id" uuid,
    "id" uuid NOT NULL,
    "aud" character varying(255),
    "role" character varying(255),
    "email" character varying(255),
    "encrypted_password" character varying(255),
    "email_confirmed_at" timestamp with time zone,
    "invited_at" timestamp with time zone,
    "confirmation_token" character varying(255),
    "confirmation_sent_at" timestamp with time zone,
    "recovery_token" character varying(255),
    "recovery_sent_at" timestamp with time zone,
    "email_change_token_new" character varying(255),
    "email_change" character varying(255),
    "email_change_sent_at" timestamp with time zone,
    "last_sign_in_at" timestamp with time zone,
    "raw_app_meta_data" jsonb,
    "raw_user_meta_data" jsonb,
    "is_super_admin" boolean,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "phone" text DEFAULT NULL::character varying,
    "phone_confirmed_at" timestamp with time zone,
    "phone_change" text DEFAULT ''::character varying,
    "phone_change_token" character varying(255) DEFAULT ''::character varying,
    "phone_change_sent_at" timestamp with time zone,
    "confirmed_at" timestamp with time zone DEFAULT LEAST(email_confirmed_at, phone_confirmed_at),
    "email_change_token_current" character varying(255) DEFAULT ''::character varying,
    "email_change_confirm_status" smallint DEFAULT 0,
    "banned_until" timestamp with time zone,
    "reauthentication_token" character varying(255) DEFAULT ''::character varying,
    "reauthentication_sent_at" timestamp with time zone,
    "is_sso_user" boolean DEFAULT false NOT NULL,
    "deleted_at" timestamp with time zone,
    "is_anonymous" boolean DEFAULT false NOT NULL
);

ALTER TABLE ONLY "auth"."users" ADD CONSTRAINT "users_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "auth"."users" ADD CONSTRAINT "users_phone_key" UNIQUE (phone);
ALTER TABLE ONLY "auth"."users" ADD CONSTRAINT "users_email_change_confirm_status_check" CHECK (email_change_confirm_status >= 0 AND email_change_confirm_status <= 2);

CREATE UNIQUE INDEX confirmation_token_idx ON auth.users USING btree (confirmation_token) WHERE ((confirmation_token)::text !~ '^[0-9 ]*$'::text);
CREATE UNIQUE INDEX email_change_token_current_idx ON auth.users USING btree (email_change_token_current) WHERE ((email_change_token_current)::text !~ '^[0-9 ]*$'::text);
CREATE UNIQUE INDEX email_change_token_new_idx ON auth.users USING btree (email_change_token_new) WHERE ((email_change_token_new)::text !~ '^[0-9 ]*$'::text);
CREATE UNIQUE INDEX reauthentication_token_idx ON auth.users USING btree (reauthentication_token) WHERE ((reauthentication_token)::text !~ '^[0-9 ]*$'::text);
CREATE UNIQUE INDEX recovery_token_idx ON auth.users USING btree (recovery_token) WHERE ((recovery_token)::text !~ '^[0-9 ]*$'::text);
CREATE UNIQUE INDEX users_email_partial_key ON auth.users USING btree (email) WHERE (is_sso_user = false);
CREATE INDEX users_instance_id_email_idx ON auth.users USING btree (instance_id, lower((email)::text));
CREATE INDEX users_instance_id_idx ON auth.users USING btree (instance_id);
CREATE INDEX users_is_anonymous_idx ON auth.users USING btree (is_anonymous);
CREATE UNIQUE INDEX users_phone_key ON auth.users USING btree (phone);

-- Table: public.alumno
CREATE TABLE "public"."alumno" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "rut" character varying(20) NOT NULL,
    "nombre" character varying(100) NOT NULL,
    "apellido" character varying(100) NOT NULL,
    "fecha_nacimiento" date NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."alumno" ADD CONSTRAINT "alumno_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."alumno" ADD CONSTRAINT "alumno_rut_key" UNIQUE (rut);

CREATE UNIQUE INDEX alumno_rut_key ON public.alumno USING btree (rut);
CREATE INDEX idx_alumno_activo ON public.alumno USING btree (activo);

-- Table: public.ano_escolar
CREATE TABLE "public"."ano_escolar" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "ano" integer NOT NULL,
    "fecha_inicio" date NOT NULL,
    "fecha_fin" date NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "fecha_inicio_planificacion" date NOT NULL
);

ALTER TABLE ONLY "public"."ano_escolar" ADD CONSTRAINT "ano_escolar_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."ano_escolar" ADD CONSTRAINT "uq_ano_escolar_ano" UNIQUE (ano);
ALTER TABLE ONLY "public"."ano_escolar" ADD CONSTRAINT "chk_fechas_orden" CHECK (fecha_inicio_planificacion < fecha_inicio AND fecha_inicio < fecha_fin);

CREATE UNIQUE INDEX uq_ano_escolar_ano ON public.ano_escolar USING btree (ano);

-- Table: public.apoderado
CREATE TABLE "public"."apoderado" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "nombre" character varying(100) NOT NULL,
    "apellido" character varying(100) NOT NULL,
    "rut" character varying(20),
    "email" character varying(255),
    "telefono" character varying(30),
    "created_at" timestamp without time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "public"."apoderado" ADD CONSTRAINT "apoderado_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."apoderado" ADD CONSTRAINT "apoderado_email_key" UNIQUE (email);
ALTER TABLE ONLY "public"."apoderado" ADD CONSTRAINT "apoderado_rut_key" UNIQUE (rut);

CREATE UNIQUE INDEX apoderado_email_key ON public.apoderado USING btree (email);
CREATE UNIQUE INDEX apoderado_rut_key ON public.apoderado USING btree (rut);

-- Table: public.apoderado_alumno
CREATE TABLE "public"."apoderado_alumno" (
    "apoderado_id" uuid NOT NULL,
    "alumno_id" uuid NOT NULL,
    "es_principal" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT now() NOT NULL,
    "vinculo" character varying(20) DEFAULT 'OTRO'::character varying NOT NULL
);

ALTER TABLE ONLY "public"."apoderado_alumno" ADD CONSTRAINT "apoderado_alumno_pkey" PRIMARY KEY (apoderado_id, alumno_id);
ALTER TABLE ONLY "public"."apoderado_alumno" ADD CONSTRAINT "apoderado_alumno_alumno_id_fkey" FOREIGN KEY (alumno_id) REFERENCES alumno(id);
ALTER TABLE ONLY "public"."apoderado_alumno" ADD CONSTRAINT "apoderado_alumno_apoderado_id_fkey" FOREIGN KEY (apoderado_id) REFERENCES apoderado(id);
ALTER TABLE ONLY "public"."apoderado_alumno" ADD CONSTRAINT "chk_apoderado_alumno_vinculo" CHECK (vinculo::text = ANY (ARRAY['MADRE'::character varying, 'PADRE'::character varying, 'TUTOR_LEGAL'::character varying, 'ABUELO'::character varying, 'OTRO'::character varying]::text[]));

CREATE INDEX idx_apoderado_alumno_alumno_id ON public.apoderado_alumno USING btree (alumno_id);

-- Table: public.asistencia_clase
CREATE TABLE "public"."asistencia_clase" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "bloque_horario_id" uuid NOT NULL,
    "fecha" date NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "registrado_por_usuario_id" uuid
);

ALTER TABLE ONLY "public"."asistencia_clase" ADD CONSTRAINT "asistencia_clase_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."asistencia_clase" ADD CONSTRAINT "uk_asistencia_clase_bloque_fecha" UNIQUE (bloque_horario_id, fecha);
ALTER TABLE ONLY "public"."asistencia_clase" ADD CONSTRAINT "fk_asistencia_clase_bloque_horario" FOREIGN KEY (bloque_horario_id) REFERENCES bloque_horario(id);
ALTER TABLE ONLY "public"."asistencia_clase" ADD CONSTRAINT "fk_asistencia_clase_registrado_por" FOREIGN KEY (registrado_por_usuario_id) REFERENCES usuario(id);

CREATE INDEX idx_asistencia_clase_bloque ON public.asistencia_clase USING btree (bloque_horario_id);
CREATE INDEX idx_asistencia_clase_fecha ON public.asistencia_clase USING btree (fecha);
CREATE INDEX idx_asistencia_clase_registrado_por ON public.asistencia_clase USING btree (registrado_por_usuario_id);
CREATE UNIQUE INDEX uk_asistencia_clase_bloque_fecha ON public.asistencia_clase USING btree (bloque_horario_id, fecha);

-- Table: public.bloque_horario
CREATE TABLE "public"."bloque_horario" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "curso_id" uuid NOT NULL,
    "dia_semana" integer NOT NULL,
    "numero_bloque" integer NOT NULL,
    "hora_inicio" time without time zone NOT NULL,
    "hora_fin" time without time zone NOT NULL,
    "tipo" character varying(20) NOT NULL,
    "materia_id" uuid,
    "profesor_id" uuid,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "bloque_horario_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "bloque_horario_curso_id_fkey" FOREIGN KEY (curso_id) REFERENCES curso(id);
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "bloque_horario_materia_id_fkey" FOREIGN KEY (materia_id) REFERENCES materia(id);
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "bloque_horario_profesor_id_fkey" FOREIGN KEY (profesor_id) REFERENCES profesor(id);
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "ck_bloque_dia_semana" CHECK (dia_semana >= 1 AND dia_semana <= 5);
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "ck_bloque_hora_fin" CHECK (hora_fin > hora_inicio);
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "ck_bloque_tipo" CHECK (tipo::text = ANY (ARRAY['CLASE'::character varying, 'RECREO'::character varying, 'ALMUERZO'::character varying]::text[]));
ALTER TABLE ONLY "public"."bloque_horario" ADD CONSTRAINT "ck_bloque_tipo_campos" CHECK ((tipo::text = ANY (ARRAY['RECREO'::character varying::text, 'ALMUERZO'::character varying::text])) AND materia_id IS NULL AND profesor_id IS NULL OR tipo::text = 'CLASE'::text);

CREATE INDEX idx_bloque_horario_curso ON public.bloque_horario USING btree (curso_id);
CREATE INDEX idx_bloque_horario_curso_activo_tipo ON public.bloque_horario USING btree (curso_id, activo, tipo) WHERE ((activo = true) AND ((tipo)::text = 'CLASE'::text));
CREATE INDEX idx_bloque_horario_curso_dia ON public.bloque_horario USING btree (curso_id, dia_semana);
CREATE INDEX idx_bloque_horario_profesor_dia ON public.bloque_horario USING btree (profesor_id, dia_semana) WHERE ((activo = true) AND (profesor_id IS NOT NULL));
CREATE UNIQUE INDEX uq_bloque_curso_dia_hora_activo ON public.bloque_horario USING btree (curso_id, dia_semana, hora_inicio) WHERE (activo = true);
CREATE UNIQUE INDEX uq_bloque_curso_dia_numero_activo ON public.bloque_horario USING btree (curso_id, dia_semana, numero_bloque) WHERE (activo = true);

-- Table: public.curso
CREATE TABLE "public"."curso" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "nombre" character varying(50) NOT NULL,
    "letra" character varying(5) NOT NULL,
    "grado_id" uuid NOT NULL,
    "ano_escolar_id" uuid NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."curso" ADD CONSTRAINT "curso_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."curso" ADD CONSTRAINT "curso_ano_escolar_id_fkey" FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id);
ALTER TABLE ONLY "public"."curso" ADD CONSTRAINT "curso_grado_id_fkey" FOREIGN KEY (grado_id) REFERENCES grado(id);
ALTER TABLE ONLY "public"."curso" ADD CONSTRAINT "fk_curso_seccion_catalogo" FOREIGN KEY (letra) REFERENCES seccion_catalogo(letra);
ALTER TABLE ONLY "public"."curso" ADD CONSTRAINT "ck_curso_letra_formato" CHECK (char_length(letra::text) = 1 AND letra::text = upper(letra::text));

CREATE INDEX idx_curso_activo ON public.curso USING btree (activo);
CREATE INDEX idx_curso_ano_escolar ON public.curso USING btree (ano_escolar_id);
CREATE INDEX idx_curso_grado ON public.curso USING btree (grado_id);
CREATE UNIQUE INDEX uq_curso_grado_ano_letra ON public.curso USING btree (grado_id, ano_escolar_id, letra);

-- Table: public.dia_no_lectivo
CREATE TABLE "public"."dia_no_lectivo" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "ano_escolar_id" uuid NOT NULL,
    "fecha" date NOT NULL,
    "tipo" character varying(30) NOT NULL,
    "descripcion" character varying(200),
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."dia_no_lectivo" ADD CONSTRAINT "dia_no_lectivo_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."dia_no_lectivo" ADD CONSTRAINT "uq_dia_no_lectivo_ano_fecha" UNIQUE (ano_escolar_id, fecha);
ALTER TABLE ONLY "public"."dia_no_lectivo" ADD CONSTRAINT "fk_dia_no_lectivo_ano_escolar" FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id);
ALTER TABLE ONLY "public"."dia_no_lectivo" ADD CONSTRAINT "chk_dia_no_lectivo_tipo" CHECK (tipo::text = ANY (ARRAY['FERIADO_LEGAL'::text, 'VACACIONES'::text, 'SUSPENSION'::text, 'INTERFERIADO'::text, 'ADMINISTRATIVO'::text]));

CREATE INDEX idx_dia_no_lectivo_ano_escolar ON public.dia_no_lectivo USING btree (ano_escolar_id);
CREATE INDEX idx_dia_no_lectivo_fecha ON public.dia_no_lectivo USING btree (fecha);
CREATE UNIQUE INDEX uq_dia_no_lectivo_ano_fecha ON public.dia_no_lectivo USING btree (ano_escolar_id, fecha);

-- Table: public.evento_auditoria
CREATE TABLE "public"."evento_auditoria" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "usuario_id" uuid NOT NULL,
    "usuario_email" character varying(255) NOT NULL,
    "usuario_rol" character varying(20) NOT NULL,
    "metodo_http" character varying(10) NOT NULL,
    "endpoint" character varying(500) NOT NULL,
    "request_body" jsonb,
    "response_status" integer NOT NULL,
    "ip_address" character varying(45),
    "ano_escolar_id" uuid,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."evento_auditoria" ADD CONSTRAINT "evento_auditoria_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."evento_auditoria" ADD CONSTRAINT "fk_evento_auditoria_usuario" FOREIGN KEY (usuario_id) REFERENCES usuario(id);

CREATE INDEX idx_evento_auditoria_created ON public.evento_auditoria USING btree (created_at DESC);
CREATE INDEX idx_evento_auditoria_endpoint ON public.evento_auditoria USING btree (endpoint);
CREATE INDEX idx_evento_auditoria_metodo ON public.evento_auditoria USING btree (metodo_http);
CREATE INDEX idx_evento_auditoria_request_body ON public.evento_auditoria USING gin (request_body);
CREATE INDEX idx_evento_auditoria_usuario ON public.evento_auditoria USING btree (usuario_id);
CREATE INDEX idx_evento_auditoria_usuario_created ON public.evento_auditoria USING btree (usuario_id, created_at DESC);

-- Table: public.grado
CREATE TABLE "public"."grado" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "nombre" character varying(50) NOT NULL,
    "nivel" integer NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."grado" ADD CONSTRAINT "grado_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."grado" ADD CONSTRAINT "uq_grado_nivel" UNIQUE (nivel);
ALTER TABLE ONLY "public"."grado" ADD CONSTRAINT "uq_grado_nombre" UNIQUE (nombre);

CREATE INDEX idx_grado_nivel ON public.grado USING btree (nivel);
CREATE UNIQUE INDEX uq_grado_nivel ON public.grado USING btree (nivel);
CREATE UNIQUE INDEX uq_grado_nombre ON public.grado USING btree (nombre);

-- Table: public.malla_curricular
CREATE TABLE "public"."malla_curricular" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "materia_id" uuid NOT NULL,
    "grado_id" uuid NOT NULL,
    "ano_escolar_id" uuid NOT NULL,
    "horas_pedagogicas" integer DEFAULT 2 NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."malla_curricular" ADD CONSTRAINT "malla_curricular_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."malla_curricular" ADD CONSTRAINT "uq_malla_materia_grado_ano" UNIQUE (materia_id, grado_id, ano_escolar_id);
ALTER TABLE ONLY "public"."malla_curricular" ADD CONSTRAINT "malla_curricular_ano_escolar_id_fkey" FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id);
ALTER TABLE ONLY "public"."malla_curricular" ADD CONSTRAINT "malla_curricular_grado_id_fkey" FOREIGN KEY (grado_id) REFERENCES grado(id);
ALTER TABLE ONLY "public"."malla_curricular" ADD CONSTRAINT "malla_curricular_materia_id_fkey" FOREIGN KEY (materia_id) REFERENCES materia(id);
ALTER TABLE ONLY "public"."malla_curricular" ADD CONSTRAINT "chk_malla_horas_positivas" CHECK (horas_pedagogicas > 0);

CREATE INDEX idx_malla_activo ON public.malla_curricular USING btree (activo);
CREATE INDEX idx_malla_ano_escolar ON public.malla_curricular USING btree (ano_escolar_id);
CREATE INDEX idx_malla_curricular_grado_ano_activo ON public.malla_curricular USING btree (grado_id, ano_escolar_id, activo);
CREATE INDEX idx_malla_grado ON public.malla_curricular USING btree (grado_id);
CREATE INDEX idx_malla_materia ON public.malla_curricular USING btree (materia_id);
CREATE UNIQUE INDEX uq_malla_materia_grado_ano ON public.malla_curricular USING btree (materia_id, grado_id, ano_escolar_id);

-- Table: public.materia
CREATE TABLE "public"."materia" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "nombre" character varying(100) NOT NULL,
    "icono" character varying(50),
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "activo" boolean DEFAULT true NOT NULL
);

ALTER TABLE ONLY "public"."materia" ADD CONSTRAINT "materia_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."materia" ADD CONSTRAINT "uq_materia_nombre" UNIQUE (nombre);

CREATE INDEX idx_materia_activo ON public.materia USING btree (activo);
CREATE UNIQUE INDEX uq_materia_nombre ON public.materia USING btree (nombre);

-- Table: public.matricula
CREATE TABLE "public"."matricula" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "alumno_id" uuid NOT NULL,
    "curso_id" uuid NOT NULL,
    "ano_escolar_id" uuid NOT NULL,
    "fecha_matricula" date NOT NULL,
    "estado" character varying(20) DEFAULT 'ACTIVA'::character varying NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."matricula" ADD CONSTRAINT "matricula_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."matricula" ADD CONSTRAINT "matricula_alumno_id_fkey" FOREIGN KEY (alumno_id) REFERENCES alumno(id);
ALTER TABLE ONLY "public"."matricula" ADD CONSTRAINT "matricula_ano_escolar_id_fkey" FOREIGN KEY (ano_escolar_id) REFERENCES ano_escolar(id);
ALTER TABLE ONLY "public"."matricula" ADD CONSTRAINT "matricula_curso_id_fkey" FOREIGN KEY (curso_id) REFERENCES curso(id);
ALTER TABLE ONLY "public"."matricula" ADD CONSTRAINT "chk_matricula_estado" CHECK (estado::text = ANY (ARRAY['ACTIVA'::character varying, 'RETIRADO'::character varying, 'TRASLADADO'::character varying]::text[]));

CREATE INDEX idx_matricula_alumno ON public.matricula USING btree (alumno_id);
CREATE INDEX idx_matricula_ano_escolar ON public.matricula USING btree (ano_escolar_id);
CREATE INDEX idx_matricula_ano_estado ON public.matricula USING btree (ano_escolar_id, estado);
CREATE INDEX idx_matricula_curso ON public.matricula USING btree (curso_id);
CREATE INDEX idx_matricula_curso_estado ON public.matricula USING btree (curso_id, estado);
CREATE INDEX idx_matricula_estado ON public.matricula USING btree (estado);
CREATE UNIQUE INDEX uq_matricula_alumno_ano_activa ON public.matricula USING btree (alumno_id, ano_escolar_id) WHERE ((estado)::text = 'ACTIVA'::text);

-- Table: public.profesor
CREATE TABLE "public"."profesor" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "rut" character varying(20) NOT NULL,
    "nombre" character varying(100) NOT NULL,
    "apellido" character varying(100) NOT NULL,
    "email" character varying(255) NOT NULL,
    "telefono" character varying(30),
    "fecha_contratacion" date NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "horas_pedagogicas_contrato" integer
);

ALTER TABLE ONLY "public"."profesor" ADD CONSTRAINT "profesor_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."profesor" ADD CONSTRAINT "profesor_email_key" UNIQUE (email);
ALTER TABLE ONLY "public"."profesor" ADD CONSTRAINT "profesor_rut_key" UNIQUE (rut);
ALTER TABLE ONLY "public"."profesor" ADD CONSTRAINT "chk_profesor_horas_contrato" CHECK (horas_pedagogicas_contrato IS NULL OR horas_pedagogicas_contrato > 0);

CREATE INDEX idx_profesor_activo ON public.profesor USING btree (activo);
CREATE UNIQUE INDEX profesor_email_key ON public.profesor USING btree (email);
CREATE UNIQUE INDEX profesor_rut_key ON public.profesor USING btree (rut);

-- Table: public.profesor_materia
CREATE TABLE "public"."profesor_materia" (
    "profesor_id" uuid NOT NULL,
    "materia_id" uuid NOT NULL
);

ALTER TABLE ONLY "public"."profesor_materia" ADD CONSTRAINT "profesor_materia_pkey" PRIMARY KEY (profesor_id, materia_id);
ALTER TABLE ONLY "public"."profesor_materia" ADD CONSTRAINT "profesor_materia_materia_id_fkey" FOREIGN KEY (materia_id) REFERENCES materia(id);
ALTER TABLE ONLY "public"."profesor_materia" ADD CONSTRAINT "profesor_materia_profesor_id_fkey" FOREIGN KEY (profesor_id) REFERENCES profesor(id);


-- Table: public.registro_asistencia
CREATE TABLE "public"."registro_asistencia" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "asistencia_clase_id" uuid NOT NULL,
    "alumno_id" uuid NOT NULL,
    "estado" character varying(20) NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "observacion" character varying(500)
);

ALTER TABLE ONLY "public"."registro_asistencia" ADD CONSTRAINT "registro_asistencia_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."registro_asistencia" ADD CONSTRAINT "uk_registro_asistencia_clase_alumno" UNIQUE (asistencia_clase_id, alumno_id);
ALTER TABLE ONLY "public"."registro_asistencia" ADD CONSTRAINT "fk_registro_asistencia_alumno" FOREIGN KEY (alumno_id) REFERENCES alumno(id);
ALTER TABLE ONLY "public"."registro_asistencia" ADD CONSTRAINT "fk_registro_asistencia_clase" FOREIGN KEY (asistencia_clase_id) REFERENCES asistencia_clase(id) ON DELETE CASCADE;
ALTER TABLE ONLY "public"."registro_asistencia" ADD CONSTRAINT "chk_registro_asistencia_estado" CHECK (estado::text = ANY (ARRAY['PRESENTE'::character varying, 'AUSENTE'::character varying, 'TARDANZA'::character varying, 'JUSTIFICADO'::character varying]::text[]));

CREATE INDEX idx_registro_asistencia_alumno ON public.registro_asistencia USING btree (alumno_id);
CREATE INDEX idx_registro_asistencia_alumno_clase ON public.registro_asistencia USING btree (alumno_id, asistencia_clase_id);
CREATE INDEX idx_registro_asistencia_clase ON public.registro_asistencia USING btree (asistencia_clase_id);
CREATE UNIQUE INDEX uk_registro_asistencia_clase_alumno ON public.registro_asistencia USING btree (asistencia_clase_id, alumno_id);

-- Table: public.seccion_catalogo
CREATE TABLE "public"."seccion_catalogo" (
    "letra" character varying(1) NOT NULL,
    "orden" smallint NOT NULL,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."seccion_catalogo" ADD CONSTRAINT "seccion_catalogo_pkey" PRIMARY KEY (letra);
ALTER TABLE ONLY "public"."seccion_catalogo" ADD CONSTRAINT "seccion_catalogo_orden_key" UNIQUE (orden);

CREATE UNIQUE INDEX seccion_catalogo_orden_key ON public.seccion_catalogo USING btree (orden);

-- Table: public.sesion_usuario
CREATE TABLE "public"."sesion_usuario" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "usuario_id" uuid NOT NULL,
    "ip_address" character varying(45),
    "latitud" numeric(10,7),
    "longitud" numeric(10,7),
    "precision_metros" numeric(8,2),
    "user_agent" character varying(500),
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "public"."sesion_usuario" ADD CONSTRAINT "sesion_usuario_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."sesion_usuario" ADD CONSTRAINT "fk_sesion_usuario_usuario" FOREIGN KEY (usuario_id) REFERENCES usuario(id);

CREATE INDEX idx_sesion_usuario_created ON public.sesion_usuario USING btree (created_at DESC);
CREATE INDEX idx_sesion_usuario_usuario ON public.sesion_usuario USING btree (usuario_id);
CREATE INDEX idx_sesion_usuario_usuario_created ON public.sesion_usuario USING btree (usuario_id, created_at DESC);

-- Table: public.usuario
CREATE TABLE "public"."usuario" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "email" character varying(255) NOT NULL,
    "password_hash" character varying(255) NOT NULL,
    "nombre" character varying(100) NOT NULL,
    "apellido" character varying(100) NOT NULL,
    "rol" character varying(20) NOT NULL,
    "profesor_id" uuid,
    "activo" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "rut" character varying(20),
    "apoderado_id" uuid,
    "refresh_token" character varying(255)
);

ALTER TABLE ONLY "public"."usuario" ADD CONSTRAINT "usuario_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "public"."usuario" ADD CONSTRAINT "usuario_email_key" UNIQUE (email);
ALTER TABLE ONLY "public"."usuario" ADD CONSTRAINT "fk_usuario_profesor" FOREIGN KEY (profesor_id) REFERENCES profesor(id);
ALTER TABLE ONLY "public"."usuario" ADD CONSTRAINT "usuario_apoderado_id_fkey" FOREIGN KEY (apoderado_id) REFERENCES apoderado(id);
ALTER TABLE ONLY "public"."usuario" ADD CONSTRAINT "chk_usuario_rol" CHECK (rol::text = ANY (ARRAY['ADMIN'::character varying, 'PROFESOR'::character varying, 'APODERADO'::character varying]::text[]));

CREATE INDEX idx_usuario_apoderado ON public.usuario USING btree (apoderado_id);
CREATE INDEX idx_usuario_profesor ON public.usuario USING btree (profesor_id) WHERE (profesor_id IS NOT NULL);
CREATE INDEX idx_usuario_rol ON public.usuario USING btree (rol);
CREATE UNIQUE INDEX usuario_email_key ON public.usuario USING btree (email);
CREATE UNIQUE INDEX ux_usuario_refresh_token_not_null ON public.usuario USING btree (refresh_token) WHERE (refresh_token IS NOT NULL);
CREATE UNIQUE INDEX ux_usuario_rut_not_null ON public.usuario USING btree (rut) WHERE (rut IS NOT NULL);

-- Table: realtime.schema_migrations
CREATE TABLE "realtime"."schema_migrations" (
    "version" bigint NOT NULL,
    "inserted_at" timestamp(0) without time zone
);

ALTER TABLE ONLY "realtime"."schema_migrations" ADD CONSTRAINT "schema_migrations_pkey" PRIMARY KEY (version);


-- Table: realtime.subscription
CREATE TABLE "realtime"."subscription" (
    "id" bigint NOT NULL,
    "subscription_id" uuid NOT NULL,
    "entity" regclass NOT NULL,
    "filters" realtime.user_defined_filter[] DEFAULT '{}'::realtime.user_defined_filter[] NOT NULL,
    "claims" jsonb NOT NULL,
    "claims_role" regrole DEFAULT realtime.to_regrole((claims ->> 'role'::text)) NOT NULL,
    "created_at" timestamp without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    "action_filter" text DEFAULT '*'::text
);

ALTER TABLE ONLY "realtime"."subscription" ADD CONSTRAINT "pk_subscription" PRIMARY KEY (id);
ALTER TABLE ONLY "realtime"."subscription" ADD CONSTRAINT "subscription_action_filter_check" CHECK (action_filter = ANY (ARRAY['*'::text, 'INSERT'::text, 'UPDATE'::text, 'DELETE'::text]));

CREATE INDEX ix_realtime_subscription_entity ON realtime.subscription USING btree (entity);
CREATE UNIQUE INDEX subscription_subscription_id_entity_filters_action_filter_key ON realtime.subscription USING btree (subscription_id, entity, filters, action_filter);

-- Table: storage.buckets
CREATE TABLE "storage"."buckets" (
    "id" text NOT NULL,
    "name" text NOT NULL,
    "owner" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "public" boolean DEFAULT false,
    "avif_autodetection" boolean DEFAULT false,
    "file_size_limit" bigint,
    "allowed_mime_types" text[],
    "owner_id" text,
    "type" storage.buckettype DEFAULT 'STANDARD'::storage.buckettype NOT NULL
);

ALTER TABLE ONLY "storage"."buckets" ADD CONSTRAINT "buckets_pkey" PRIMARY KEY (id);

CREATE UNIQUE INDEX bname ON storage.buckets USING btree (name);

-- Table: storage.buckets_analytics
CREATE TABLE "storage"."buckets_analytics" (
    "name" text NOT NULL,
    "type" storage.buckettype DEFAULT 'ANALYTICS'::storage.buckettype NOT NULL,
    "format" text DEFAULT 'ICEBERG'::text NOT NULL,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT now() NOT NULL,
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "deleted_at" timestamp with time zone
);

ALTER TABLE ONLY "storage"."buckets_analytics" ADD CONSTRAINT "buckets_analytics_pkey" PRIMARY KEY (id);

CREATE UNIQUE INDEX buckets_analytics_unique_name_idx ON storage.buckets_analytics USING btree (name) WHERE (deleted_at IS NULL);

-- Table: storage.buckets_vectors
CREATE TABLE "storage"."buckets_vectors" (
    "id" text NOT NULL,
    "type" storage.buckettype DEFAULT 'VECTOR'::storage.buckettype NOT NULL,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "storage"."buckets_vectors" ADD CONSTRAINT "buckets_vectors_pkey" PRIMARY KEY (id);


-- Table: storage.migrations
CREATE TABLE "storage"."migrations" (
    "id" integer NOT NULL,
    "name" character varying(100) NOT NULL,
    "hash" character varying(40) NOT NULL,
    "executed_at" timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ONLY "storage"."migrations" ADD CONSTRAINT "migrations_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "storage"."migrations" ADD CONSTRAINT "migrations_name_key" UNIQUE (name);

CREATE UNIQUE INDEX migrations_name_key ON storage.migrations USING btree (name);

-- Table: storage.objects
CREATE TABLE "storage"."objects" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "bucket_id" text,
    "name" text,
    "owner" uuid,
    "created_at" timestamp with time zone DEFAULT now(),
    "updated_at" timestamp with time zone DEFAULT now(),
    "last_accessed_at" timestamp with time zone DEFAULT now(),
    "metadata" jsonb,
    "path_tokens" text[] DEFAULT string_to_array(name, '/'::text),
    "version" text,
    "owner_id" text,
    "user_metadata" jsonb
);

ALTER TABLE ONLY "storage"."objects" ADD CONSTRAINT "objects_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "storage"."objects" ADD CONSTRAINT "objects_bucketId_fkey" FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id);

CREATE UNIQUE INDEX bucketid_objname ON storage.objects USING btree (bucket_id, name);
CREATE INDEX idx_objects_bucket_id_name ON storage.objects USING btree (bucket_id, name COLLATE "C");
CREATE INDEX idx_objects_bucket_id_name_lower ON storage.objects USING btree (bucket_id, lower(name) COLLATE "C");
CREATE INDEX name_prefix_search ON storage.objects USING btree (name text_pattern_ops);

-- Table: storage.s3_multipart_uploads
CREATE TABLE "storage"."s3_multipart_uploads" (
    "id" text NOT NULL,
    "in_progress_size" bigint DEFAULT 0 NOT NULL,
    "upload_signature" text NOT NULL,
    "bucket_id" text NOT NULL,
    "key" text NOT NULL,
    "version" text NOT NULL,
    "owner_id" text,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "user_metadata" jsonb
);

ALTER TABLE ONLY "storage"."s3_multipart_uploads" ADD CONSTRAINT "s3_multipart_uploads_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "storage"."s3_multipart_uploads" ADD CONSTRAINT "s3_multipart_uploads_bucket_id_fkey" FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id);

CREATE INDEX idx_multipart_uploads_list ON storage.s3_multipart_uploads USING btree (bucket_id, key, created_at);

-- Table: storage.s3_multipart_uploads_parts
CREATE TABLE "storage"."s3_multipart_uploads_parts" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "upload_id" text NOT NULL,
    "size" bigint DEFAULT 0 NOT NULL,
    "part_number" integer NOT NULL,
    "bucket_id" text NOT NULL,
    "key" text NOT NULL,
    "etag" text NOT NULL,
    "owner_id" text,
    "version" text NOT NULL,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "storage"."s3_multipart_uploads_parts" ADD CONSTRAINT "s3_multipart_uploads_parts_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "storage"."s3_multipart_uploads_parts" ADD CONSTRAINT "s3_multipart_uploads_parts_bucket_id_fkey" FOREIGN KEY (bucket_id) REFERENCES storage.buckets(id);
ALTER TABLE ONLY "storage"."s3_multipart_uploads_parts" ADD CONSTRAINT "s3_multipart_uploads_parts_upload_id_fkey" FOREIGN KEY (upload_id) REFERENCES storage.s3_multipart_uploads(id) ON DELETE CASCADE;


-- Table: storage.vector_indexes
CREATE TABLE "storage"."vector_indexes" (
    "id" text DEFAULT gen_random_uuid() NOT NULL,
    "name" text NOT NULL,
    "bucket_id" text NOT NULL,
    "data_type" text NOT NULL,
    "dimension" integer NOT NULL,
    "distance_metric" text NOT NULL,
    "metadata_configuration" jsonb,
    "created_at" timestamp with time zone DEFAULT now() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT now() NOT NULL
);

ALTER TABLE ONLY "storage"."vector_indexes" ADD CONSTRAINT "vector_indexes_pkey" PRIMARY KEY (id);
ALTER TABLE ONLY "storage"."vector_indexes" ADD CONSTRAINT "vector_indexes_bucket_id_fkey" FOREIGN KEY (bucket_id) REFERENCES storage.buckets_vectors(id);

CREATE UNIQUE INDEX vector_indexes_name_bucket_id_idx ON storage.vector_indexes USING btree (name, bucket_id);

-- Table: vault.secrets
CREATE TABLE "vault"."secrets" (
    "id" uuid DEFAULT gen_random_uuid() NOT NULL,
    "name" text,
    "description" text DEFAULT ''::text NOT NULL,
    "secret" text NOT NULL,
    "key_id" uuid,
    "nonce" bytea DEFAULT vault._crypto_aead_det_noncegen(),
    "created_at" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    "updated_at" timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE ONLY "vault"."secrets" ADD CONSTRAINT "secrets_pkey" PRIMARY KEY (id);

CREATE UNIQUE INDEX secrets_name_idx ON vault.secrets USING btree (name) WHERE (name IS NOT NULL);

