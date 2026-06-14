package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * O unealta din inventarul firmei (ex. ciocan, bormasina).
 * `cantitate_disponibila` = cate sunt libere acum; in uz = totala - disponibila.
 *
 * Schema DB asteptata (tabela `unelte`):
 *   id_unealta (pk), id_firma (fk), denumire, cantitate_totala, cantitate_disponibila
 */
@Serializable
data class Unealta(
    @SerialName("id_unealta") val id: Long,
    @SerialName("id_firma") val companyId: Long,
    @SerialName("denumire") val name: String,
    @SerialName("cantitate_totala") val totalQuantity: Int,
    @SerialName("cantitate_disponibila") val availableQuantity: Int,
) {
    val inUse: Int get() = (totalQuantity - availableQuantity).coerceAtLeast(0)
}

@Serializable
data class UnealtaInsert(
    @SerialName("id_firma") val companyId: Long,
    @SerialName("denumire") val name: String,
    @SerialName("cantitate_totala") val totalQuantity: Int,
    @SerialName("cantitate_disponibila") val availableQuantity: Int,
)
