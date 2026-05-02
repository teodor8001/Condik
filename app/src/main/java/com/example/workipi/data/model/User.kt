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
)

fun User.toUser(): MockUser = MockUser(
    id = idUser.toString(),
    name = fullName,
    email = email,
    phone = phoneNumber.toString(),
    role = when (role?.lowercase()) {
        "admin" -> UserRole.ADMIN
        "inginer" -> UserRole.PROJECT_MANAGER
        "angajat" -> UserRole.ANGAJAT
        "client" -> UserRole.CLIENT
        else -> UserRole.ANGAJAT
    },
)