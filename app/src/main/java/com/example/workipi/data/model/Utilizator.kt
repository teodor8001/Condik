package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Utilizator(
    @SerialName("id_utilizator") val idUtilizator: Long,
    @SerialName("nume_prenume") val numePrenume: String,
    @SerialName("email") val email: String,
    @SerialName("numar_telefon") val numarTelefon: Long,
    @SerialName("rol") val rol: String? = null,
    @SerialName("id_firma") val idFirma: Long? = null,
    @SerialName("salariu") val salariu: Double? = null,
    @SerialName("punctaj") val punctaj: Double? = null,
    @SerialName("auth_utilizator_id") val authUtilizatorId: String? = null,
)

fun Utilizator.toUser(): User = User(
    id = idUtilizator.toString(),
    name = numePrenume,
    email = email,
    phone = numarTelefon.toString(),
    role = when (rol?.lowercase()) {
        "admin" -> UserRole.ADMIN
        "inginer" -> UserRole.PROJECT_MANAGER
        "angajat" -> UserRole.ANGAJAT
        "client" -> UserRole.CLIENT
        else -> UserRole.ANGAJAT
    },
)