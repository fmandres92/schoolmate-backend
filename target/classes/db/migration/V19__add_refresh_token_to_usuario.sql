alter table public.usuario
  add column if not exists refresh_token varchar(255);

create unique index if not exists ux_usuario_refresh_token_not_null
  on public.usuario (refresh_token)
  where refresh_token is not null;
