package com.example.workipi.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvitationCodeInsert(
    @SerialName("cod") val code: String,
    @SerialName("id_firma") val companyId: Long,
    @SerialName("rol") val role: String,
    @SerialName("email") val email: String,
    @SerialName("numar_telefon") val phoneNumber: String,
    @SerialName("nume_complet") val fullName: String,
    @SerialName("data_expirare") val expirationDate: Instant,
    @SerialName("salariu") val salary: Float? = null,
)

@Serializable
data class InvitationCode(
    @SerialName("id_cod") val id: Long,
    @SerialName("cod") val code: String,
    @SerialName("id_firma") val companyId: Long,
    @SerialName("rol") val role: String,
    @SerialName("email") val email: String,
    @SerialName("numar_telefon") val phoneNumber: String,
    @SerialName("nume_complet") val fullName: String,
    @SerialName("este_folosit") val isUsed: Boolean,
    @SerialName("data_expirare") val expirationDate: Instant,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("salariu") val salary: Float? = null,
)