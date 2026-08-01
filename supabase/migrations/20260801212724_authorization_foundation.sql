create schema if not exists private;

revoke all on schema private from public;
grant usage on schema private to authenticated, service_role;

create table public.roluri (
    cod text primary key,
    denumire text not null,
    descriere text,
    ordine smallint not null default 0,
    created_at timestamptz not null default now()
);

create table public.permisiuni (
    cod text primary key,
    denumire text not null,
    descriere text,
    created_at timestamptz not null default now()
);

create table public.roluri_permisiuni (
    cod_rol text not null references public.roluri(cod) on update cascade on delete cascade,
    cod_permisiune text not null references public.permisiuni(cod) on update cascade on delete cascade,
    created_at timestamptz not null default now(),
    primary key (cod_rol, cod_permisiune)
);

insert into public.roluri (cod, denumire, descriere, ordine) values
    ('admin', 'Administrator', 'Administreaza firma si toate modulele.', 10),
    ('manager', 'Manager', 'Urmareste proiectele, performanta, riscurile si rezultatele financiare.', 20),
    ('inginer', 'Inginer', 'Coordoneaza executia si calitatea proiectelor alocate.', 30),
    ('sef_echipa', 'Sef de echipa', 'Coordoneaza prezenta, pontajele si activitatea echipei.', 40),
    ('angajat', 'Angajat', 'Executa si raporteaza activitatea proprie.', 50),
    ('client', 'Client', 'Urmareste proiectele la care este alocat.', 60)
on conflict (cod) do update set
    denumire = excluded.denumire,
    descriere = excluded.descriere,
    ordine = excluded.ordine;

insert into public.permisiuni (cod, denumire, descriere) values
    ('dashboard.view', 'Vizualizare Acasa', 'Permite accesul la pagina Acasa adaptata rolului.'),
    ('projects.view', 'Vizualizare proiecte', 'Permite accesul la proiectele vizibile utilizatorului.'),
    ('projects.create', 'Creare proiecte', 'Permite crearea proiectelor noi.'),
    ('projects.manage', 'Administrare proiecte', 'Permite modificarea proiectelor accesibile.'),
    ('site.view', 'Vizualizare santier', 'Permite vizualizarea activitatii zilnice din santier.'),
    ('site.manage', 'Administrare santier', 'Permite raportarea, verificarea si corectarea activitatii zilnice.'),
    ('team.view', 'Vizualizare echipa', 'Permite vizualizarea membrilor relevanti ai echipei.'),
    ('team.manage', 'Administrare echipa', 'Permite invitarea si administrarea membrilor echipei.'),
    ('performance.view', 'Vizualizare performanta', 'Permite vizualizarea clasamentelor si scorurilor disponibile.'),
    ('performance.manage', 'Administrare performanta', 'Permite administrarea regulilor si recompenselor.'),
    ('resources.view', 'Vizualizare resurse', 'Permite vizualizarea materialelor si uneltelor accesibile.'),
    ('resources.manage', 'Administrare resurse', 'Permite administrarea materialelor si uneltelor.'),
    ('offers.view', 'Vizualizare ofertare', 'Permite vizualizarea ofertelor firmei.'),
    ('offers.manage', 'Administrare ofertare', 'Permite crearea si modificarea ofertelor.'),
    ('administration.view', 'Vizualizare administrare', 'Permite accesul la configurarea firmei.'),
    ('administration.manage', 'Administrare firma', 'Permite modificarea configurarii firmei.'),
    ('financials.view', 'Vizualizare financiar', 'Permite accesul la bugete, costuri, marja si profit.'),
    ('time_entries.create', 'Creare pontaj', 'Permite raportarea activitatii proprii.'),
    ('time_entries.review', 'Verificare pontaje', 'Permite verificarea pontajelor echipei.'),
    ('settings.view', 'Vizualizare setari', 'Permite accesul la setarile personale.')
on conflict (cod) do update set
    denumire = excluded.denumire,
    descriere = excluded.descriere;

insert into public.roluri_permisiuni (cod_rol, cod_permisiune)
select 'admin', cod from public.permisiuni
on conflict do nothing;

insert into public.roluri_permisiuni (cod_rol, cod_permisiune) values
    ('manager', 'dashboard.view'),
    ('manager', 'projects.view'),
    ('manager', 'projects.manage'),
    ('manager', 'site.view'),
    ('manager', 'site.manage'),
    ('manager', 'team.view'),
    ('manager', 'team.manage'),
    ('manager', 'performance.view'),
    ('manager', 'performance.manage'),
    ('manager', 'resources.view'),
    ('manager', 'resources.manage'),
    ('manager', 'offers.view'),
    ('manager', 'offers.manage'),
    ('manager', 'financials.view'),
    ('manager', 'time_entries.create'),
    ('manager', 'time_entries.review'),
    ('manager', 'settings.view'),
    ('inginer', 'dashboard.view'),
    ('inginer', 'projects.view'),
    ('inginer', 'projects.manage'),
    ('inginer', 'site.view'),
    ('inginer', 'site.manage'),
    ('inginer', 'team.view'),
    ('inginer', 'performance.view'),
    ('inginer', 'resources.view'),
    ('inginer', 'time_entries.create'),
    ('inginer', 'time_entries.review'),
    ('inginer', 'settings.view'),
    ('sef_echipa', 'dashboard.view'),
    ('sef_echipa', 'projects.view'),
    ('sef_echipa', 'site.view'),
    ('sef_echipa', 'site.manage'),
    ('sef_echipa', 'team.view'),
    ('sef_echipa', 'performance.view'),
    ('sef_echipa', 'resources.view'),
    ('sef_echipa', 'time_entries.create'),
    ('sef_echipa', 'time_entries.review'),
    ('sef_echipa', 'settings.view'),
    ('angajat', 'dashboard.view'),
    ('angajat', 'projects.view'),
    ('angajat', 'site.view'),
    ('angajat', 'performance.view'),
    ('angajat', 'resources.view'),
    ('angajat', 'time_entries.create'),
    ('angajat', 'settings.view'),
    ('client', 'dashboard.view'),
    ('client', 'projects.view'),
    ('client', 'settings.view')
on conflict do nothing;

alter table public.utilizatori
    add constraint utilizatori_rol_fkey
    foreign key (rol) references public.roluri(cod) on update cascade;

alter table public.coduri_invitatie
    add constraint coduri_invitatie_rol_fkey
    foreign key (rol) references public.roluri(cod) on update cascade;

create or replace function private.current_user_id()
returns bigint
language sql
stable
security definer
set search_path = ''
as $$
    select u.id_utilizator
    from public.utilizatori u
    where (select auth.uid()) is not null
      and u.auth_utilizator_id = (select auth.uid())
    limit 1
$$;

create or replace function private.current_company_id()
returns bigint
language sql
stable
security definer
set search_path = ''
as $$
    select u.id_firma
    from public.utilizatori u
    where (select auth.uid()) is not null
      and u.auth_utilizator_id = (select auth.uid())
    limit 1
$$;

create or replace function private.current_user_role()
returns text
language sql
stable
security definer
set search_path = ''
as $$
    select u.rol
    from public.utilizatori u
    where (select auth.uid()) is not null
      and u.auth_utilizator_id = (select auth.uid())
    limit 1
$$;

create or replace function private.has_permission(permission_code text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select (select auth.uid()) is not null
       and exists (
            select 1
            from public.roluri_permisiuni rp
            where rp.cod_rol = (select private.current_user_role())
              and rp.cod_permisiune = permission_code
       )
$$;

revoke all on function private.current_user_id() from public;
revoke all on function private.current_company_id() from public;
revoke all on function private.current_user_role() from public;
revoke all on function private.has_permission(text) from public;
grant execute on function private.current_user_id() to authenticated, service_role;
grant execute on function private.current_company_id() to authenticated, service_role;
grant execute on function private.current_user_role() to authenticated, service_role;
grant execute on function private.has_permission(text) to authenticated, service_role;

create or replace function public.get_my_permissions()
returns table (permission text)
language sql
stable
security definer
set search_path = ''
as $$
    select rp.cod_permisiune
    from public.roluri_permisiuni rp
    where (select auth.uid()) is not null
      and rp.cod_rol = (select private.current_user_role())
    order by rp.cod_permisiune
$$;

revoke all on function public.get_my_permissions() from public;
grant execute on function public.get_my_permissions() to authenticated, service_role;

alter table public.roluri enable row level security;
alter table public.permisiuni enable row level security;
alter table public.roluri_permisiuni enable row level security;

create policy "authenticated can read roles"
on public.roluri for select
to authenticated
using (true);

create policy "authenticated can read permissions"
on public.permisiuni for select
to authenticated
using (true);

create policy "authenticated can read role permissions"
on public.roluri_permisiuni for select
to authenticated
using (true);

revoke all on public.roluri from anon, authenticated;
revoke all on public.permisiuni from anon, authenticated;
revoke all on public.roluri_permisiuni from anon, authenticated;
grant select on public.roluri to authenticated;
grant select on public.permisiuni to authenticated;
grant select on public.roluri_permisiuni to authenticated;
grant all on public.roluri to service_role;
grant all on public.permisiuni to service_role;
grant all on public.roluri_permisiuni to service_role;
