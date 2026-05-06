package com.example.workipi.data.mock

import com.example.workipi.data.model.*

// ---------------------------------------------------------------------------
// MockData — va fi inlocuit cu Supabase
// ---------------------------------------------------------------------------
object MockData {

    // ---- Useri (autentificare) ----
    val users = listOf(
        MockUser("1", "Ion Popescu",    "admin@workipi.com",   "0721000001", UserRole.ADMIN),
        MockUser("2", "Maria Ionescu",  "manager@workipi.com", "0721000002", UserRole.PROJECT_MANAGER),
        MockUser("3", "Gheorghe Marin", "angajat@workipi.com", "0721000003", UserRole.ANGAJAT)
    )

    // ---- Angajati (muncitori) ----
    val employees = listOf(
        MockEmployee(
            id = "e1", name = "Constantin Vasile", age = 42,
            email = "c.vasile@workipi.ro", phone = "0721 111 001",
            primarySpecialty = "Betonist",
            specialties = listOf("Betonare", "Cofraje", "Armaturi", "Finisaje beton"),
            level = EmployeeLevel.LEAD, points = 4850
        ),
        MockEmployee(
            id = "e2", name = "Popa Gheorghe", age = 36,
            email = "g.popa@workipi.ro", phone = "0721 111 002",
            primarySpecialty = "Zidar",
            specialties = listOf("Zidarie BCA", "Zidarie caramida", "Tencuiala"),
            level = EmployeeLevel.SENIOR, points = 3920
        ),
        MockEmployee(
            id = "e3", name = "Dobre Mihai", age = 28,
            email = "m.dobre@workipi.ro", phone = "0721 111 003",
            primarySpecialty = "Zugrav",
            specialties = listOf("Zugravit interior", "Vopsit fatade", "Glet"),
            level = EmployeeLevel.MID, points = 2710
        ),
        MockEmployee(
            id = "e4", name = "Stan Alexandru", age = 39,
            email = "a.stan@workipi.ro", phone = "0721 111 004",
            primarySpecialty = "Fierar Betonist",
            specialties = listOf("Armaturi", "Sudura", "Cofraje metalice", "Betonare"),
            level = EmployeeLevel.SENIOR, points = 4430
        ),
        MockEmployee(
            id = "e5", name = "Ionescu Florin", age = 33,
            email = "f.ionescu@workipi.ro", phone = "0721 111 005",
            primarySpecialty = "Instalator Sanitar",
            specialties = listOf("Instalatii sanitare", "Instalatii termice", "Montaj calorifere"),
            level = EmployeeLevel.MID, points = 2190
        ),
        MockEmployee(
            id = "e6", name = "Marin Cristian", age = 31,
            email = "c.marin@workipi.ro", phone = "0721 111 006",
            primarySpecialty = "Electrician",
            specialties = listOf("Instalatii electrice", "Tablouri electrice", "Automatizari"),
            level = EmployeeLevel.MID, points = 1870
        ),
        MockEmployee(
            id = "e7", name = "Oprea Radu", age = 22,
            email = "r.oprea@workipi.ro", phone = "0721 111 007",
            primarySpecialty = "Zidar",
            specialties = listOf("Zidarie BCA", "Tencuiala"),
            level = EmployeeLevel.JUNIOR, points = 980
        ),
        MockEmployee(
            id = "e8", name = "Dumitrescu Andrei", age = 35,
            email = "a.dumitrescu@workipi.ro", phone = "0721 111 008",
            primarySpecialty = "Tencuitor",
            specialties = listOf("Tencuiala manuala", "Tencuiala mecanizata", "Glet", "Zugravit"),
            level = EmployeeLevel.SENIOR, points = 3540
        ),
        MockEmployee(
            id = "e9", name = "Luca Bogdan", age = 26,
            email = "b.luca@workipi.ro", phone = "0721 111 009",
            primarySpecialty = "Betonist",
            specialties = listOf("Betonare", "Cofraje"),
            level = EmployeeLevel.JUNIOR, points = 1240
        ),
        MockEmployee(
            id = "e10", name = "Neagu Daniel", age = 30,
            email = "d.neagu@workipi.ro", phone = "0721 111 010",
            primarySpecialty = "Fierar Betonist",
            specialties = listOf("Armaturi", "Sudura", "Cofraje metalice"),
            level = EmployeeLevel.MID, points = 2480
        )
    )

    // ---- Proiecte ----
    val mockProjects = listOf(
        MockProject(
            id             = "p1",
            name           = "Bloc Rezidential Sector 3",
            status         = ProjectStatus.ACTIV,
            startDate      = "15 Mar 2024",
            endDate        = "30 Nov 2025",
            progress       = 0.73f,
            contractValue  = 500_000.0,
            budget         = 420_000.0,
            currentCosts   = 320_000.0,
            revenueLastMonth = 45_000.0,
            revenueThisMonth = 52_000.0,
            tasks = listOf(
                ProjectTask("Structura Beton",  1.00f),
                ProjectTask("Cofraj",           1.00f),
                ProjectTask("Armaturi",         0.95f),
                ProjectTask("Zidarie",          0.80f),
                ProjectTask("Tencuiala",        0.60f),
                ProjectTask("Zugravit",         0.40f),
                ProjectTask("Finisaje",         0.10f),
                ProjectTask("Instalatii",       0.25f)
            ),
            employeeIds = listOf("e1", "e2", "e3", "e4")
        ),
        MockProject(
            id             = "p2",
            name           = "Pod Autostrada A1",
            status         = ProjectStatus.INTARZIAT,
            startDate      = "01 Iun 2024",
            endDate        = "30 Iun 2026",
            progress       = 0.45f,
            contractValue  = 1_200_000.0,
            budget         = 1_050_000.0,
            currentCosts   = 540_000.0,
            revenueLastMonth = 80_000.0,
            revenueThisMonth = 75_000.0,
            tasks = listOf(
                ProjectTask("Structura Beton",  0.70f),
                ProjectTask("Cofraj",           0.65f),
                ProjectTask("Armaturi",         0.50f),
                ProjectTask("Hidroizolatie",    0.20f),
                ProjectTask("Finisaje",         0.00f)
            ),
            employeeIds = listOf("e1", "e4", "e9", "e10", "e5")
        ),
        MockProject(
            id             = "p3",
            name           = "Hala Industriala Ploiesti",
            status         = ProjectStatus.ACTIV,
            startDate      = "01 Ian 2024",
            endDate        = "31 Iul 2025",
            progress       = 0.91f,
            contractValue  = 300_000.0,
            budget         = 270_000.0,
            currentCosts   = 275_000.0,
            revenueLastMonth = 28_000.0,
            revenueThisMonth = 30_000.0,
            tasks = listOf(
                ProjectTask("Structura Beton",  1.00f),
                ProjectTask("Cofraj",           1.00f),
                ProjectTask("Armaturi",         1.00f),
                ProjectTask("Zidarie",          1.00f),
                ProjectTask("Tencuiala",        0.90f),
                ProjectTask("Zugravit",         0.75f),
                ProjectTask("Finisaje",         0.60f),
                ProjectTask("Instalatii",       0.55f)
            ),
            employeeIds = listOf("e2", "e3", "e6", "e8")
        ),
        MockProject(
            id             = "p4",
            name           = "Ansamblu Rezidential Pipera",
            status         = ProjectStatus.ACTIV,
            startDate      = "01 Sep 2024",
            endDate        = "31 Dec 2026",
            progress       = 0.30f,
            contractValue  = 800_000.0,
            budget         = 720_000.0,
            currentCosts   = 240_000.0,
            revenueLastMonth = 55_000.0,
            revenueThisMonth = 62_000.0,
            tasks = listOf(
                ProjectTask("Structura Beton",  0.60f),
                ProjectTask("Cofraj",           0.55f),
                ProjectTask("Armaturi",         0.45f),
                ProjectTask("Zidarie",          0.15f),
                ProjectTask("Tencuiala",        0.00f),
                ProjectTask("Zugravit",         0.00f),
                ProjectTask("Finisaje",         0.00f)
            ),
            employeeIds = listOf("e2", "e4", "e7", "e9", "e10")
        )
    )

    // ---- Skill-uri companie (editabile din PreturiScreen) ----
    val skills = mutableListOf(
        Skill("s1",  "Zugravit interior",     MeasureUnit.MP, pricePerUnit = 12.0,  pointsPerUnit = 5.0),
        Skill("s2",  "Vopsit fatade",         MeasureUnit.MP, pricePerUnit = 18.0,  pointsPerUnit = 7.0),
        Skill("s3",  "Glet",                  MeasureUnit.MP, pricePerUnit = 10.0,  pointsPerUnit = 4.0),
        Skill("s4",  "Tencuiala manuala",     MeasureUnit.MP, pricePerUnit = 15.0,  pointsPerUnit = 6.0),
        Skill("s5",  "Tencuiala mecanizata",  MeasureUnit.MP, pricePerUnit = 10.0,  pointsPerUnit = 4.0),
        Skill("s6",  "Zidarie BCA",           MeasureUnit.MP, pricePerUnit = 20.0,  pointsPerUnit = 8.0),
        Skill("s7",  "Zidarie caramida",      MeasureUnit.MP, pricePerUnit = 25.0,  pointsPerUnit = 10.0),
        Skill("s8",  "Betonare",              MeasureUnit.MC, pricePerUnit = 80.0,  pointsPerUnit = 15.0),
        Skill("s9",  "Cofraje",               MeasureUnit.MP, pricePerUnit = 22.0,  pointsPerUnit = 9.0),
        Skill("s10", "Armaturi",              MeasureUnit.ML, pricePerUnit = 5.0,   pointsPerUnit = 3.0),
        Skill("s11", "Sudura",                MeasureUnit.ML, pricePerUnit = 8.0,   pointsPerUnit = 5.0),
        Skill("s12", "Hidroizolatie",         MeasureUnit.MP, pricePerUnit = 30.0,  pointsPerUnit = 12.0),
        Skill("s13", "Instalatii sanitare",   MeasureUnit.ML, pricePerUnit = 25.0,  pointsPerUnit = 10.0),
        Skill("s14", "Instalatii electrice",  MeasureUnit.ML, pricePerUnit = 20.0,  pointsPerUnit = 8.0),
        Skill("s15", "Finisaje interioare",   MeasureUnit.MP, pricePerUnit = 35.0,  pointsPerUnit = 14.0)
    )

    fun login(identifier: String, password: String): MockUser? {
        if (password.length < 4) return null
        return users.find {
            it.email.equals(identifier.trim(), ignoreCase = true) ||
            it.phone == identifier.trim()
        }
    }

    fun getProjectById(id: String): MockProject? = mockProjects.find { it.id == id }

    fun getEmployeesByIds(ids: List<String>): List<MockEmployee> =
        employees.filter { it.id in ids }
}

// Sesiunea curenta — va fi inlocuita cu Supabase Auth
object MockSession {
    var currentUser: MockUser? = null
}