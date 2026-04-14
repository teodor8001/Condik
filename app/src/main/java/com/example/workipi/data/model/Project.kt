package com.example.workipi.data.model

data class Project(
    val id: String,
    val name: String,
    val status: ProjectStatus,
    val startDate: String,          // format: "dd MMM yyyy"
    val endDate: String,            // format: "dd MMM yyyy"
    val progress: Float,            // 0f – 1f
    val contractValue: Double,      // RON – valoarea totala a contractului
    val budget: Double,             // RON – buget intern alocat
    val currentCosts: Double,       // RON – costuri curente
    val revenueLastMonth: Double,   // RON – venit luna trecuta
    val revenueThisMonth: Double,   // RON – venit luna aceasta
    val tasks: List<ProjectTask>,
    val employeeIds: List<String>   // referinte catre Employee.id
)