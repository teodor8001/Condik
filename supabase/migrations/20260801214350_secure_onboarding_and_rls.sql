-- Tenant helpers used by policies. They deliberately live outside the exposed
-- schema and never trust ids supplied by the mobile client as company scope.
revoke all on function public.get_my_permissions() from anon;

create or replace function private.same_company_user(target_user_id bigint)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select (select auth.uid()) is not null
       and exists (
            select 1
            from public.utilizatori u
            where u.id_utilizator = target_user_id
              and u.id_firma = (select private.current_company_id())
       )
$$;

create or replace function private.project_company_id(target_project_id bigint)
returns bigint
language sql
stable
security definer
set search_path = ''
as $$
    select p.id_firma
    from public.proiecte p
    where p.id_proiect = target_project_id
$$;

create or replace function private.can_access_project(target_project_id bigint)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select (select auth.uid()) is not null
       and (select private.project_company_id(target_project_id)) = (select private.current_company_id())
       and (
            (select private.has_permission('projects.manage'))
            or exists (
                select 1
                from public.utilizatori_proiecte up
                where up.id_proiect = target_project_id
                  and up.id_utilizator = (select private.current_user_id())
            )
       )
$$;

revoke all on function private.same_company_user(bigint) from public;
revoke all on function private.project_company_id(bigint) from public;
revoke all on function private.can_access_project(bigint) from public;
grant execute on function private.same_company_user(bigint) to authenticated, service_role;
grant execute on function private.project_company_id(bigint) to authenticated, service_role;
grant execute on function private.can_access_project(bigint) to authenticated, service_role;

-- A user may update a very small set of fields on their own profile. Broader
-- changes are checked by permission even if a future client accidentally sends
-- extra columns in an UPDATE request.
create or replace function private.guard_user_update()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    is_own boolean := old.auth_utilizator_id = (select auth.uid());
    can_manage_team boolean := (select private.has_permission('team.manage'));
    can_manage_site boolean := (select private.has_permission('site.manage'));
    can_manage_performance boolean := (select private.has_permission('performance.manage'));
begin
    if (select auth.uid()) is null then
        return new;
    end if;

    if new.auth_utilizator_id is distinct from old.auth_utilizator_id
       or new.id_firma is distinct from old.id_firma then
        raise exception 'Campurile de identitate si firma nu pot fi modificate';
    end if;

    if can_manage_team and old.id_firma = (select private.current_company_id()) then
        return new;
    end if;

    if can_manage_site and old.id_firma = (select private.current_company_id())
       and new.nume_prenume is not distinct from old.nume_prenume
       and new.numar_telefon is not distinct from old.numar_telefon
       and new.email is not distinct from old.email
       and new.salariu is not distinct from old.salariu
       and new.punctaj is not distinct from old.punctaj
       and new.rol is not distinct from old.rol
       and new.necesita_schimbare_parola is not distinct from old.necesita_schimbare_parola then
        return new;
    end if;

    if can_manage_performance and old.id_firma = (select private.current_company_id())
       and new.nume_prenume is not distinct from old.nume_prenume
       and new.numar_telefon is not distinct from old.numar_telefon
       and new.email is not distinct from old.email
       and new.salariu is not distinct from old.salariu
       and new.rol is not distinct from old.rol
       and new.este_checked_in is not distinct from old.este_checked_in
       and new.necesita_schimbare_parola is not distinct from old.necesita_schimbare_parola then
        return new;
    end if;

    if is_own
       and new.nume_prenume is not distinct from old.nume_prenume
       and new.numar_telefon is not distinct from old.numar_telefon
       and new.email is not distinct from old.email
       and new.salariu is not distinct from old.salariu
       and new.punctaj is not distinct from old.punctaj
       and new.rol is not distinct from old.rol then
        return new;
    end if;

    raise exception 'Nu ai permisiunea de a modifica aceste campuri';
end
$$;

revoke all on function private.guard_user_update() from public;

drop trigger if exists guard_user_update on public.utilizatori;
create trigger guard_user_update
before update on public.utilizatori
for each row execute function private.guard_user_update();

-- Atomic self-service company bootstrap. The authenticated identity determines
-- the profile auth id; callers cannot attach themselves to an existing company.
create or replace function public.bootstrap_company(
    p_company_name text,
    p_full_name text,
    p_email text,
    p_phone text
)
returns setof public.utilizatori
language plpgsql
security definer
set search_path = ''
as $$
declare
    new_company_id bigint;
begin
    if (select auth.uid()) is null then
        raise exception 'Autentificare necesara';
    end if;
    if trim(coalesce(p_company_name, '')) = ''
       or trim(coalesce(p_full_name, '')) = ''
       or trim(coalesce(p_email, '')) = ''
       or trim(coalesce(p_phone, '')) = '' then
        raise exception 'Toate campurile sunt obligatorii';
    end if;
    if lower(trim(coalesce((select auth.jwt() ->> 'email'), ''))) <> lower(trim(p_email)) then
        raise exception 'Emailul nu corespunde contului autentificat';
    end if;
    if exists (
        select 1 from public.utilizatori u
        where u.auth_utilizator_id = (select auth.uid())
    ) then
        raise exception 'Contul are deja un profil';
    end if;
    if exists (
        select 1 from public.firme f
        where lower(trim(f.denumire)) = lower(trim(p_company_name))
    ) then
        raise exception 'Exista deja o firma cu acest nume';
    end if;

    insert into public.firme (denumire)
    values (trim(p_company_name))
    returning id_firma into new_company_id;

    return query
    insert into public.utilizatori (
        nume_prenume, email, numar_telefon, rol, id_firma, auth_utilizator_id
    ) values (
        trim(p_full_name), lower(trim(p_email)), trim(p_phone), 'admin',
        new_company_id, (select auth.uid())
    )
    returning *;
end
$$;

-- A random invitation code is a bearer secret. This endpoint reveals only the
-- exact, still-valid invitation requested by the caller, never a searchable list.
create or replace function public.get_valid_invitation(p_code text)
returns setof public.coduri_invitatie
language sql
stable
security definer
set search_path = ''
as $$
    select ci.*
    from public.coduri_invitatie ci
    where ci.cod = upper(trim(p_code))
      and coalesce(ci.este_folosit, false) = false
      and ci.data_expirare > now()
    limit 1
$$;

-- Claims the invitation atomically after Auth signup, creates the profile, copies
-- configured skills and consumes the bearer code in the same transaction.
create or replace function public.claim_invitation(
    p_code text,
    p_needs_password_change boolean default false
)
returns setof public.utilizatori
language plpgsql
security definer
set search_path = ''
as $$
declare
    invitation public.coduri_invitatie%rowtype;
    new_user_id bigint;
begin
    if (select auth.uid()) is null then
        raise exception 'Autentificare necesara';
    end if;

    select ci.* into invitation
    from public.coduri_invitatie ci
    where ci.cod = upper(trim(p_code))
      and coalesce(ci.este_folosit, false) = false
      and ci.data_expirare > now()
    for update;

    if not found then
        raise exception 'Invitatia este invalida, expirata sau deja folosita';
    end if;
    if lower(trim(coalesce((select auth.jwt() ->> 'email'), ''))) <> lower(trim(invitation.email)) then
        raise exception 'Invitatia apartine altei adrese de email';
    end if;
    if exists (
        select 1 from public.utilizatori u
        where u.auth_utilizator_id = (select auth.uid())
    ) then
        raise exception 'Contul are deja un profil';
    end if;

    insert into public.utilizatori (
        nume_prenume, email, numar_telefon, salariu, rol, id_firma,
        auth_utilizator_id, necesita_schimbare_parola
    ) values (
        trim(invitation.nume_complet), lower(trim(invitation.email)),
        trim(invitation.numar_telefon), invitation.salariu, invitation.rol,
        invitation.id_firma, (select auth.uid()), coalesce(p_needs_password_change, false)
    )
    returning id_utilizator into new_user_id;

    insert into public.utilizatori_lucrari (id_utilizator, id_lucrare, skill_level)
    select new_user_id, cil.id_lucrare, cil.skill_level
    from public.coduri_invitatie_lucrari cil
    where cil.id_cod = invitation.id_cod;

    delete from public.coduri_invitatie where id_cod = invitation.id_cod;

    return query
    select u.* from public.utilizatori u where u.id_utilizator = new_user_id;
end
$$;

revoke all on function public.bootstrap_company(text, text, text, text) from public, anon, authenticated;
revoke all on function public.get_valid_invitation(text) from public, anon, authenticated;
revoke all on function public.claim_invitation(text, boolean) from public, anon, authenticated;
grant execute on function public.bootstrap_company(text, text, text, text) to authenticated, service_role;
grant execute on function public.get_valid_invitation(text) to anon, authenticated, service_role;
grant execute on function public.claim_invitation(text, boolean) to authenticated, service_role;

-- Remove legacy policies before installing one coherent tenant policy set.
drop policy if exists "authenticated users can insert invitation codes" on public.coduri_invitatie;
drop policy if exists "Users can view own profile" on public.utilizatori;

alter table public.coduri_invitatie enable row level security;
alter table public.coduri_invitatie_lucrari enable row level security;
alter table public.firme enable row level security;
alter table public.istoric_pontari enable row level security;
alter table public.lucrari enable row level security;
alter table public.materiale enable row level security;
alter table public.notificari enable row level security;
alter table public.proiecte enable row level security;
alter table public.revizii enable row level security;
alter table public.utilizatori enable row level security;
alter table public.utilizatori_lucrari enable row level security;
alter table public.utilizatori_notificari enable row level security;
alter table public.utilizatori_proiecte enable row level security;
alter table public.zone enable row level security;
alter table public.zone_lucrari enable row level security;

create policy "company members can read their company"
on public.firme for select to authenticated
using (id_firma = (select private.current_company_id()));

create policy "users can read visible team profiles"
on public.utilizatori for select to authenticated
using (
    auth_utilizator_id = (select auth.uid())
    or (
        id_firma = (select private.current_company_id())
        and (select private.has_permission('team.view'))
    )
);
create policy "authorized users can update team profiles"
on public.utilizatori for update to authenticated
using (
    auth_utilizator_id = (select auth.uid())
    or (
        id_firma = (select private.current_company_id())
        and (
            (select private.has_permission('team.manage'))
            or (select private.has_permission('site.manage'))
            or (select private.has_permission('performance.manage'))
        )
    )
)
with check (id_firma = (select private.current_company_id()));
create policy "team managers can delete team profiles"
on public.utilizatori for delete to authenticated
using (
    id_firma = (select private.current_company_id())
    and id_utilizator <> (select private.current_user_id())
    and (select private.has_permission('team.manage'))
);

create policy "team managers can read invitations"
on public.coduri_invitatie for select to authenticated
using (id_firma = (select private.current_company_id()) and (select private.has_permission('team.manage')));
create policy "team managers can create invitations"
on public.coduri_invitatie for insert to authenticated
with check (id_firma = (select private.current_company_id()) and (select private.has_permission('team.manage')));
create policy "team managers can update invitations"
on public.coduri_invitatie for update to authenticated
using (id_firma = (select private.current_company_id()) and (select private.has_permission('team.manage')))
with check (id_firma = (select private.current_company_id()) and (select private.has_permission('team.manage')));
create policy "team managers can delete invitations"
on public.coduri_invitatie for delete to authenticated
using (id_firma = (select private.current_company_id()) and (select private.has_permission('team.manage')));

create policy "team managers can read invitation skills"
on public.coduri_invitatie_lucrari for select to authenticated
using (exists (
    select 1 from public.coduri_invitatie ci
    where ci.id_cod = coduri_invitatie_lucrari.id_cod
      and ci.id_firma = (select private.current_company_id())
      and (select private.has_permission('team.manage'))
));
create policy "team managers can create invitation skills"
on public.coduri_invitatie_lucrari for insert to authenticated
with check (exists (
    select 1 from public.coduri_invitatie ci
    where ci.id_cod = coduri_invitatie_lucrari.id_cod
      and ci.id_firma = (select private.current_company_id())
      and (select private.has_permission('team.manage'))
));
create policy "team managers can delete invitation skills"
on public.coduri_invitatie_lucrari for delete to authenticated
using (exists (
    select 1 from public.coduri_invitatie ci
    where ci.id_cod = coduri_invitatie_lucrari.id_cod
      and ci.id_firma = (select private.current_company_id())
      and (select private.has_permission('team.manage'))
));

create policy "users can read accessible projects"
on public.proiecte for select to authenticated
using ((select private.can_access_project(id_proiect)));
create policy "authorized users can create projects"
on public.proiecte for insert to authenticated
with check (
    id_firma = (select private.current_company_id())
    and (
        (coalesce(este_oferta, false) and (select private.has_permission('offers.manage')))
        or (not coalesce(este_oferta, false) and (select private.has_permission('projects.create')))
    )
);
create policy "project managers can update projects"
on public.proiecte for update to authenticated
using (
    id_firma = (select private.current_company_id())
    and ((select private.has_permission('projects.manage')) or (select private.has_permission('offers.manage')))
)
with check (id_firma = (select private.current_company_id()));
create policy "project managers can delete projects"
on public.proiecte for delete to authenticated
using (
    id_firma = (select private.current_company_id())
    and ((select private.has_permission('projects.manage')) or (select private.has_permission('offers.manage')))
);

create policy "users can read company work catalog"
on public.lucrari for select to authenticated
using (id_firma = (select private.current_company_id()));
create policy "administrators can create work catalog"
on public.lucrari for insert to authenticated
with check (id_firma = (select private.current_company_id()) and (select private.has_permission('administration.manage')));
create policy "administrators can update work catalog"
on public.lucrari for update to authenticated
using (id_firma = (select private.current_company_id()) and (select private.has_permission('administration.manage')))
with check (id_firma = (select private.current_company_id()));
create policy "administrators can delete work catalog"
on public.lucrari for delete to authenticated
using (id_firma = (select private.current_company_id()) and (select private.has_permission('administration.manage')));

create policy "users can read accessible zones"
on public.zone for select to authenticated
using ((select private.can_access_project(id_proiect)));
create policy "site managers can create zones"
on public.zone for insert to authenticated
with check ((select private.can_access_project(id_proiect)) and (select private.has_permission('site.manage')));
create policy "site managers can update zones"
on public.zone for update to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('site.manage')))
with check ((select private.can_access_project(id_proiect)));
create policy "site managers can delete zones"
on public.zone for delete to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('site.manage')));

create policy "users can read accessible zone work"
on public.zone_lucrari for select to authenticated
using (exists (
    select 1 from public.zone z
    where z.id_zona = zone_lucrari.id_zona
      and (select private.can_access_project(z.id_proiect))
));
create policy "site managers can create zone work"
on public.zone_lucrari for insert to authenticated
with check (exists (
    select 1 from public.zone z
    where z.id_zona = zone_lucrari.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.manage'))
));
create policy "site managers can update zone work"
on public.zone_lucrari for update to authenticated
using (exists (
    select 1 from public.zone z
    where z.id_zona = zone_lucrari.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.manage'))
));
create policy "site managers can delete zone work"
on public.zone_lucrari for delete to authenticated
using (exists (
    select 1 from public.zone z
    where z.id_zona = zone_lucrari.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.manage'))
));

create policy "users can read accessible time entries"
on public.istoric_pontari for select to authenticated
using (
    id_utilizator = (select private.current_user_id())
    or (
        (select private.has_permission('time_entries.review'))
        and (select private.same_company_user(id_utilizator))
    )
);
create policy "users can create own time entries"
on public.istoric_pontari for insert to authenticated
with check (
    id_utilizator = (select private.current_user_id())
    and (select private.has_permission('time_entries.create'))
    and exists (
        select 1 from public.zone z
        where z.id_zona = istoric_pontari.id_zona
          and (select private.can_access_project(z.id_proiect))
    )
);
create policy "reviewers can update time entries"
on public.istoric_pontari for update to authenticated
using ((select private.has_permission('time_entries.review')) and (select private.same_company_user(id_utilizator)))
with check ((select private.same_company_user(id_utilizator)));
create policy "reviewers can delete time entries"
on public.istoric_pontari for delete to authenticated
using ((select private.has_permission('time_entries.review')) and (select private.same_company_user(id_utilizator)));

create policy "users can read visible user skills"
on public.utilizatori_lucrari for select to authenticated
using (
    id_utilizator = (select private.current_user_id())
    or ((select private.has_permission('team.view')) and (select private.same_company_user(id_utilizator)))
);
create policy "team managers can create user skills"
on public.utilizatori_lucrari for insert to authenticated
with check ((select private.has_permission('team.manage')) and (select private.same_company_user(id_utilizator)));
create policy "team managers can update user skills"
on public.utilizatori_lucrari for update to authenticated
using ((select private.has_permission('team.manage')) and (select private.same_company_user(id_utilizator)))
with check ((select private.same_company_user(id_utilizator)));
create policy "team managers can delete user skills"
on public.utilizatori_lucrari for delete to authenticated
using ((select private.has_permission('team.manage')) and (select private.same_company_user(id_utilizator)));

create policy "users can read accessible project assignments"
on public.utilizatori_proiecte for select to authenticated
using (
    id_utilizator = (select private.current_user_id())
    or (
        (select private.has_permission('team.view'))
        and (select private.can_access_project(id_proiect))
        and (select private.same_company_user(id_utilizator))
    )
);
create policy "project managers can create assignments"
on public.utilizatori_proiecte for insert to authenticated
with check (
    (select private.has_permission('projects.manage'))
    and (select private.project_company_id(id_proiect)) = (select private.current_company_id())
    and (select private.same_company_user(id_utilizator))
);
create policy "project managers can delete assignments"
on public.utilizatori_proiecte for delete to authenticated
using (
    (select private.has_permission('projects.manage'))
    and (select private.project_company_id(id_proiect)) = (select private.current_company_id())
    and (select private.same_company_user(id_utilizator))
);

create policy "users can read accessible materials"
on public.materiale for select to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('resources.view')));
create policy "resource managers can create materials"
on public.materiale for insert to authenticated
with check ((select private.can_access_project(id_proiect)) and (select private.has_permission('resources.manage')));
create policy "resource managers can update materials"
on public.materiale for update to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('resources.manage')))
with check ((select private.can_access_project(id_proiect)));
create policy "resource managers can delete materials"
on public.materiale for delete to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('resources.manage')));

create policy "users can read accessible revisions"
on public.revizii for select to authenticated
using (exists (
    select 1 from public.zone z
    where z.id_zona = revizii.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.view'))
));
create policy "site managers can create revisions"
on public.revizii for insert to authenticated
with check (exists (
    select 1 from public.zone z
    where z.id_zona = revizii.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.manage'))
));
create policy "site managers can update revisions"
on public.revizii for update to authenticated
using (exists (
    select 1 from public.zone z
    where z.id_zona = revizii.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.manage'))
));
create policy "site managers can delete revisions"
on public.revizii for delete to authenticated
using (exists (
    select 1 from public.zone z
    where z.id_zona = revizii.id_zona
      and (select private.can_access_project(z.id_proiect))
      and (select private.has_permission('site.manage'))
));

create policy "users can read accessible notifications"
on public.notificari for select to authenticated
using ((select private.can_access_project(id_proiect)));
create policy "site managers can create notifications"
on public.notificari for insert to authenticated
with check ((select private.can_access_project(id_proiect)) and (select private.has_permission('site.manage')));
create policy "site managers can update notifications"
on public.notificari for update to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('site.manage')))
with check ((select private.can_access_project(id_proiect)));
create policy "site managers can delete notifications"
on public.notificari for delete to authenticated
using ((select private.can_access_project(id_proiect)) and (select private.has_permission('site.manage')));

create policy "users can read own notification links"
on public.utilizatori_notificari for select to authenticated
using (id_utilizator = (select private.current_user_id()));
create policy "site managers can create notification links"
on public.utilizatori_notificari for insert to authenticated
with check ((select private.same_company_user(id_utilizator)) and (select private.has_permission('site.manage')));
create policy "users can delete own notification links"
on public.utilizatori_notificari for delete to authenticated
using (id_utilizator = (select private.current_user_id()));

-- Explicit Data API grants are required independently of RLS.
revoke all on table public.coduri_invitatie, public.coduri_invitatie_lucrari,
    public.firme, public.istoric_pontari, public.lucrari, public.materiale,
    public.notificari, public.proiecte, public.revizii, public.utilizatori,
    public.utilizatori_lucrari, public.utilizatori_notificari,
    public.utilizatori_proiecte, public.zone, public.zone_lucrari
from anon, authenticated;

grant select on public.firme to authenticated;
grant select, update, delete on public.utilizatori to authenticated;
grant select, insert, update, delete on public.coduri_invitatie to authenticated;
grant select, insert, delete on public.coduri_invitatie_lucrari to authenticated;
grant select, insert, update, delete on public.proiecte to authenticated;
grant select, insert, update, delete on public.lucrari to authenticated;
grant select, insert, update, delete on public.zone to authenticated;
grant select, insert, update, delete on public.zone_lucrari to authenticated;
grant select, insert, update, delete on public.istoric_pontari to authenticated;
grant select, insert, update, delete on public.utilizatori_lucrari to authenticated;
grant select, insert, delete on public.utilizatori_proiecte to authenticated;
grant select, insert, update, delete on public.materiale to authenticated;
grant select, insert, update, delete on public.revizii to authenticated;
grant select, insert, update, delete on public.notificari to authenticated;
grant select, insert, delete on public.utilizatori_notificari to authenticated;

grant all on table public.coduri_invitatie, public.coduri_invitatie_lucrari,
    public.firme, public.istoric_pontari, public.lucrari, public.materiale,
    public.notificari, public.proiecte, public.revizii, public.utilizatori,
    public.utilizatori_lucrari, public.utilizatori_notificari,
    public.utilizatori_proiecte, public.zone, public.zone_lucrari
to service_role;
