package com.example.workipi.data.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectInsert(
    @SerialName("denumire") val title: String,
    @SerialName("adresa") val adress: String,
    @SerialName("costuri_salarii") val totalSalaryPerMonth: Float,
    @SerialName("termen_inceput") val startDate: Instant,
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
