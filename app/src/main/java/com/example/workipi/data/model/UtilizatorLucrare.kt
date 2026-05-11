package com.example.workipi.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Rand in tabela "utilizatori_lucrari" — skill atasat unui utilizator existent.
 */
@Serializable
data class UtilizatorLucrareInsert(
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("id_lucrare") val idLucrare: Long,
    @SerialName("skill_level") val skillLevel: String,
)

@Serializable
data class UtilizatorLucrare(
    @SerialName("id_utilizator_lucrare") val id: Long,
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("id_lucrare") val idLucrare: Long,
    @SerialName("skill_level") val skillLevel: String,
)

/**
 * Rand in tabela "coduri_invitatie_lucrari" — skill pre-configurat pe o invitatie.
 * La activarea contului, randurile astea sunt copiate in "utilizatori_lucrari".
 */
@Serializable
data class InvitationCodeLucrareInsert(
    @SerialName("id_cod") val codeId: Long,
    @SerialName("id_lucrare") val idLucrare: Long,
    @SerialName("skill_level") val skillLevel: String,
)

@Serializable
data class InvitationCodeLucrare(
    @SerialName("id_cod_invitatie_lucrare") val id: Long,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("id_cod") val codeId: Long,
    @SerialName("id_lucrare") val idLucrare: Long,
    @SerialName("skill_level") val skillLevel: String,
)
