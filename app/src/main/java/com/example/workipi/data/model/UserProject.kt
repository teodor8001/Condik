package com.example.workipi.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProjectInsert(
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("id_proiect") val projectId: Long,
)

@Serializable
data class UserProject(
    @SerialName("id_utilizator_proiect") val id: Long,
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("created_at") val createdAt: Instant,
)
