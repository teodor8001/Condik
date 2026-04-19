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