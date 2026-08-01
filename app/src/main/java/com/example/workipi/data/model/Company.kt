package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Firma(
    @SerialName("id_firma") val idFirma: Long,
    @SerialName("denumire") val denumire: String,
)

@Serializable
data class FirmaInsert(
    @SerialName("denumire") val denumire: String,
)

@Serializable
data class UtilizatorInsert(
    @SerialName("nume_prenume") val numePrenume: String,
    @SerialName("email") val email: String,
    @SerialName("numar_telefon") val numarTelefon: String,
    @SerialName("rol") val rol: String,
    @SerialName("id_firma") val idFirma: Long,
    @SerialName("auth_utilizator_id") val authUtilizatorId: String,
    @SerialName("salariu") val salariu: Float? = null,
    @SerialName("necesita_schimbare_parola") val necesitaSchimbareParola: Boolean = false,
)