package com.example.workipi.data.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class MockProject(
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

@Serializable
data class ProjectInsert(
    @SerialName("denumire") val title: String,
    @SerialName("adresa") val adress: String,
    @SerialName("costuri_salarii") val totalSalaryPerMonth: Float,
    @SerialName("termen_finalizare") val endDate: LocalDate,
    @SerialName("buget") val budget: Float,
    @SerialName("id_firma") val companyId: Long,
    @SerialName("este_oferta") val isOffer: Boolean = false,
)

@Serializable
data class Project(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("termen_inceput") val startDate: Instant,
    @SerialName("denumire") val title: String,
    @SerialName("adresa") val adress: String,
    @SerialName("costuri_salarii") val totalSalaryPerMonth: Float,
    @SerialName("data_salariu") val salaryDate: LocalDate? = null,
    @SerialName("termen_finalizare") val endDate: LocalDate,
    @SerialName("buget") val budget: Float? = null,
    @SerialName("id_firma") val companyId: Long,
    @SerialName("este_oferta") val isOffer: Boolean = false,
)