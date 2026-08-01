package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id_utilizator") val idUser: Long,
    @SerialName("nume_prenume") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("numar_telefon") val phoneNumber: Long,
    @SerialName("rol") val role: String? = null,
    @SerialName("id_firma") val idCompany: Long? = null,
    @SerialName("salariu") val salary: Double? = null,
    @SerialName("punctaj") val points: Double? = null,
    @SerialName("auth_utilizator_id") val authUserId: String? = null,
    @SerialName("este_checked_in") val isCheckedIn: Boolean = false,
    // true cat timp angajatul nu si-a schimbat parola initiala setata de admin ("In asteptare").
    @SerialName("necesita_schimbare_parola") val needsPasswordChange: Boolean = false,
)
