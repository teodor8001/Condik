# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build and install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.example.workipi.ExampleUnitTest"

# Full build
./gradlew build
```

## Architecture Overview

**WorkIPI** is a single-module Android app for construction site (santier) management. Built with Kotlin + Jetpack Compose + Material3.

**Three layers:**
- `data/model/` — pure data classes and enums (User, Employee, Project, Skill, etc.)
- `data/mock/` — in-memory mock data (`MockData.kt`) and `MockSession.currentUser` (global session state); planned for replacement with Supabase
- `ui/` — screens, reusable components, and theme

**Navigation** is centralized in `navigation/NavGraph.kt`. Routes are defined as a sealed class in `navigation/Screen.kt`. The app uses `AppNavigationDrawer` (in `ui/components/`) which renders as a permanent drawer on tablets (>600dp) or a modal drawer on phones.

**Role-based access:** `UserRole` enum (ADMIN, PROJECT_MANAGER, ANGAJAT) controls which nav items are visible and which home screen variant is shown (`AdminHomeScreen` vs `EmployeeHomeScreen`).

**Responsive layout:** Screens use `BoxWithConstraints` to detect tablet vs. phone and switch between 1-column and 2-column grid layouts accordingly.

## Key Conventions

- **Language:** App UI labels, error messages, and code comments are in Romanian (e.g., `Angajati` = Employees, `Proiecte` = Projects, `Pontare` = Time Tracking, `Preturi` = Pricing)
- **Mock data is the source of truth** until Supabase is integrated — all reads/writes go through `MockData` and `MockSession`
- **Theme colors:** Primary orange `#E07B39`, light `#F5A876`, dark `#B85C1A`. Defined in `ui/theme/Color.kt`
- **SDK targets:** compileSdk 36, minSdk 24, Java 11 toolchain

## Planned Migration

The current mock-data architecture is a temporary MVP. Comments in `MockData.kt` indicate planned migration to Supabase for authentication and data persistence.

## Supabase Schema (target)

The Supabase database has 12 tables:

**Entities:** `firme`, `utilizatori`, `proiecte`, `lucrari`, `zone`, `notificari`, `revizii`, `istoric_pontari`

**Join tables (many-to-many):** `utilizatori_proiecte`, `utilizatori_lucrari`, `utilizatori_notificari`, `proiecte_lucrari`

**Auxiliary tables:** `coduri_invitatie` (one-time codes for inviting non-admin users — see invite flow below)

**Naming convention:** all table names are lowercase, snake_case, and plural. Avoid `"PascalCase"` table names — they require quoted identifiers in every query.

**Auth linkage:** `utilizatori.auth_utilizator_id` (uuid, nullable) is a foreign key to `auth.users.id`. Nullable because admin pre-creates user rows before the person activates via invite code. The `parola` column was REMOVED from `utilizatori` — passwords live exclusively in Supabase Auth.

### Multi-tenant model

`firma` is the workspace/tenant boundary. A user from one firma must NOT see data from another firma. Every query must be scoped by the logged-in user's `id_firma`. Enforce with Supabase Row Level Security (RLS) policies — do NOT rely on client-side filtering.

For a `client` user, `id_firma` is the firma that EXECUTES the project (not the client's own company), so RLS correctly scopes them into that workspace.

### Naming caveats

- **`istoric_pontari` is NOT a join table** despite its `X_Y` name — no `istoric` or `pontari` entities exist. It's a transactional log of time-tracking entries: `id_utilizator`, `id_zona`, `ore_lucrate`, `cantitate`, `calitate`, `data`.
- **`lucrare` is polysemantic** — same entity represents both "a skill an angajat can perform" (`utilizator_lucrari`) and "a skill required for a project" (`proiecte_lucrari`).

### Key relationships

- `zone` belongs to exactly one `proiect` (1:N). A `proiect` has many `zone`.
- `istoric_pontari` stores `id_zona` but NOT `id_proiect` — the project is derived via `zone.id_proiect`. Do not duplicate.
- `progres` (integer 0-100) lives on `proiecte_lucrari`, not on `lucrari` — same lucrare has different progress per project.
- `revizii` (inspections) are scoped to a `proiect`. Created by admin, visible to inginer, should also generate a `notificare`. A `status` field is planned (todo/done/approved) but not yet in schema.

### Roles and access control

Supabase roles: `admin`, `inginer`, `client`, `angajat` (the current `UserRole` enum has only `ADMIN`, `PROJECT_MANAGER`, `ANGAJAT` — `PROJECT_MANAGER` maps to `inginer`, and `client` needs to be added during migration).

- **`admin`**: full access within own firma.
- **`inginer`** (sub-șef): CANNOT create proiecte (admin-only). Sees only proiecte he is assigned to. CAN update proiecte he is assigned to. MUST NOT see company revenue.
- **`angajat`**: sees only proiecte he is assigned to. Read-only for most fields.
- **`client`**: sees only his own proiect (progres, angajați working on it). MUST NOT see total employee count across firma, top-angajat rankings, or other proiecte. `client` stays as a role on `utilizator` (not a separate table) — auth/fields are shared with other users.

Access rules must be enforced in two layers: RLS policies in Supabase (source of truth) AND UI guards in Android (hide buttons/menus that would otherwise fail — for UX, not security).