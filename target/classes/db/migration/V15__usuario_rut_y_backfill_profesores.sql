create extension if not exists pgcrypto;

alter table public.usuario
  add column if not exists rut varchar(20);

create unique index if not exists ux_usuario_rut_not_null
  on public.usuario (rut)
  where rut is not null;

with prof as (
  select
    p.id as profesor_id,
    p.email as email,
    p.nombre as nombre,
    p.apellido as apellido,
    upper(regexp_replace(p.rut, '[^0-9kK]', '', 'g')) as rut_clean
  from public.profesor p
  where p.activo = true
),
prof_norm as (
  select
    profesor_id,
    email,
    nombre,
    apellido,
    case
      when rut_clean is null or length(rut_clean) < 2 then null
      else left(rut_clean, length(rut_clean) - 1) || '-' || right(rut_clean, 1)
    end as rut_norm
  from prof
),
upd as (
  update public.usuario u
  set rut = pn.rut_norm,
      updated_at = now()
  from prof_norm pn
  where u.profesor_id = pn.profesor_id
    and u.rut is null
  returning u.id
)
insert into public.usuario (
  id,
  email,
  rut,
  password_hash,
  nombre,
  apellido,
  rol,
  profesor_id,
  alumno_id,
  activo,
  created_at,
  updated_at
)
select
  gen_random_uuid()::text,
  pn.email,
  pn.rut_norm,
  crypt(pn.rut_norm, gen_salt('bf')),
  pn.nombre,
  pn.apellido,
  'PROFESOR',
  pn.profesor_id,
  null,
  true,
  now(),
  now()
from prof_norm pn
where pn.rut_norm is not null
  and not exists (
    select 1 from public.usuario u where u.profesor_id = pn.profesor_id
  );
