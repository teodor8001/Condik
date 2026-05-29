package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Material(
    @SerialName("id_material") val id: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("denumire") val name: String,
    @SerialName("cantitate") val quantity: Float,
    @SerialName("unitate_masura") val unit: String,
    @SerialName("cost_unitate") val unitCost: Float,
) {
    val totalCost: Float get() = quantity * unitCost
}

@Serializable
data class MaterialInsert(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("denumire") val name: String,
    @SerialName("cantitate") val quantity: Float,
    @SerialName("unitate_masura") val unit: String,
    @SerialName("cost_unitate") val unitCost: Float,
)
