create or replace function public.get_visible_projects()
returns table (
    id_proiect bigint,
    termen_inceput timestamptz,
    denumire text,
    adresa text,
    termen_finalizare date,
    buget double precision,
    costuri_salarii double precision,
    data_salariu date,
    id_firma bigint,
    este_oferta boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        p.id_proiect,
        p.termen_inceput,
        p.denumire,
        p.adresa,
        p.termen_finalizare,
        case when (select private.has_permission('financials.view')) then p.buget else 0::double precision end,
        case when (select private.has_permission('financials.view')) then p.costuri_salarii else 0::double precision end,
        case when (select private.has_permission('financials.view')) then p.data_salariu else null::date end,
        p.id_firma,
        p.este_oferta
    from public.proiecte p
    where (select auth.uid()) is not null
      and p.id_firma = (select private.current_company_id())
      and (
        (
            not coalesce(p.este_oferta, false)
            and (select private.can_access_project(p.id_proiect))
            and (select private.has_permission('projects.view'))
        )
        or (
            coalesce(p.este_oferta, false)
            and (select private.has_permission('offers.view'))
        )
      )
    order by p.termen_inceput desc
$$;

create or replace function public.get_visible_project(p_project_id bigint)
returns table (
    id_proiect bigint,
    termen_inceput timestamptz,
    denumire text,
    adresa text,
    termen_finalizare date,
    buget double precision,
    costuri_salarii double precision,
    data_salariu date,
    id_firma bigint,
    este_oferta boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select vp.*
    from public.get_visible_projects() vp
    where vp.id_proiect = p_project_id
    limit 1
$$;

create or replace function public.get_visible_users()
returns table (
    id_utilizator bigint,
    nume_prenume text,
    email text,
    numar_telefon text,
    salariu double precision,
    punctaj numeric,
    rol text,
    id_firma bigint,
    auth_utilizator_id uuid,
    este_checked_in boolean,
    necesita_schimbare_parola boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select
        u.id_utilizator,
        u.nume_prenume,
        case
            when u.auth_utilizator_id = (select auth.uid())
              or (select private.has_permission('team.manage'))
            then u.email else '0'
        end,
        case
            when u.auth_utilizator_id = (select auth.uid())
              or (select private.has_permission('team.manage'))
            then u.numar_telefon else '0'
        end,
        case
            when u.auth_utilizator_id = (select auth.uid())
              or (select private.has_permission('financials.view'))
              or (select private.has_permission('team.manage'))
            then u.salariu else null::double precision
        end,
        u.punctaj,
        u.rol,
        u.id_firma,
        case when u.auth_utilizator_id = (select auth.uid()) then u.auth_utilizator_id else null::uuid end,
        u.este_checked_in,
        case when u.auth_utilizator_id = (select auth.uid()) then u.necesita_schimbare_parola else false end
    from public.utilizatori u
    where (select auth.uid()) is not null
      and u.id_firma = (select private.current_company_id())
      and (
        u.auth_utilizator_id = (select auth.uid())
        or (select private.has_permission('team.view'))
      )
    order by u.nume_prenume
$$;

create or replace function public.get_visible_user(p_user_id bigint)
returns table (
    id_utilizator bigint,
    nume_prenume text,
    email text,
    numar_telefon text,
    salariu double precision,
    punctaj numeric,
    rol text,
    id_firma bigint,
    auth_utilizator_id uuid,
    este_checked_in boolean,
    necesita_schimbare_parola boolean
)
language sql
stable
security definer
set search_path = ''
as $$
    select vu.*
    from public.get_visible_users() vu
    where vu.id_utilizator = p_user_id
    limit 1
$$;

revoke all on function public.get_visible_projects() from public, anon, authenticated;
revoke all on function public.get_visible_project(bigint) from public, anon, authenticated;
revoke all on function public.get_visible_users() from public, anon, authenticated;
revoke all on function public.get_visible_user(bigint) from public, anon, authenticated;
grant execute on function public.get_visible_projects() to authenticated, service_role;
grant execute on function public.get_visible_project(bigint) to authenticated, service_role;
grant execute on function public.get_visible_users() to authenticated, service_role;
grant execute on function public.get_visible_user(bigint) to authenticated, service_role;

drop policy if exists "users can read accessible projects" on public.proiecte;
create policy "financial users can read project base rows"
on public.proiecte for select to authenticated
using (
    (select private.has_permission('financials.view'))
    and (
        (not coalesce(este_oferta, false) and (select private.can_access_project(id_proiect)))
        or (coalesce(este_oferta, false) and (select private.has_permission('offers.view')) and id_firma = (select private.current_company_id()))
    )
);

drop policy if exists "users can read visible team profiles" on public.utilizatori;
create policy "users can read own or managed profiles"
on public.utilizatori for select to authenticated
using (
    auth_utilizator_id = (select auth.uid())
    or (
        id_firma = (select private.current_company_id())
        and (select private.has_permission('team.manage'))
    )
);
