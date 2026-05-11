package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Zone(
    @SerialName("id_zona") val id: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("denumire_zona") val name: String? = null,
    @SerialName("suprafata_totala") val surface: Float = 0f,
    @SerialName("suprafata_terminata") val surfaceCompleted: Float? = null,
)

@Serializable
data class ZoneInsert(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("denumire_zona") val name: String,
    @SerialName("suprafata_totala") val surface: Float,
)
