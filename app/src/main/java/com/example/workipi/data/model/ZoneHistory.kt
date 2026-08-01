 package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZoneHistory(
    @SerialName("id_zona") val zoneId: Long,
    @SerialName("id_lucrare") val lucrareId: Long,
    @SerialName("cantitate_totala") val totalQuantity: Float,
    @SerialName("cantitate_lucrata") val completedQuantity: Float = 0f,
)

@Serializable
data class ZoneHistoryInsert(
    @SerialName("id_zona") val zoneId: Long,
    @SerialName("id_lucrare") val lucrareId: Long,
    @SerialName("cantitate_totala") val totalQuantity: Float,
)