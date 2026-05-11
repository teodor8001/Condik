package com.example.workipi.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PontareInsert(
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("id_lucrare") val idLucrare: Long,
    @SerialName("id_zona") val idZona: Long,
    @SerialName("ore") val hours: Double,
    @SerialName("cantitate") val quantity: Float,
    @SerialName("calitate") val quality: Float,
    @SerialName("data_pontarii") val workDate: LocalDate,
)

@Serializable
data class Pontare(
    @SerialName("id_pontare") val id: Long,
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("id_lucrare") val idLucrare: Long,
    @SerialName("id_zona") val idZona: Long,
    @SerialName("ore") val hours: Double? = null,
    @SerialName("cantitate") val quantity: Float,
    @SerialName("calitate") val quality: Float? = null,
    @SerialName("data_pontarii") val workDate: LocalDate? = null,
)
