package com.example.workipi.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Lucrare(
    @SerialName("id_lucrare") val id: Long,
    @SerialName("denumire") val name: String,
    @SerialName("unitate_masura") val unit: String,
    @SerialName("pret") val price: Float,
    @SerialName("punctaj") val points: Long,
    @SerialName("id_firma") val idFirma: Long? = null,
)

@Serializable
data class LucrareInsert(
    @SerialName("denumire") val name: String,
    @SerialName("unitate_masura") val unit: String,
    @SerialName("pret") val price: Float,
    @SerialName("punctaj") val points: Long,
    @SerialName("id_firma") val idFirma: Long,
)

enum class SkillLevel(val dbValue: String, val label: String) {
    JUNIOR("Junior", "Junior"),
    MID("Mid", "Mid"),
    SENIOR("Senior", "Senior"),
    LEAD("Lead", "Lead"),
}

fun String.toSkillLevel(): SkillLevel =
    SkillLevel.entries.firstOrNull { it.dbValue.equals(this, ignoreCase = true) } ?: SkillLevel.JUNIOR
