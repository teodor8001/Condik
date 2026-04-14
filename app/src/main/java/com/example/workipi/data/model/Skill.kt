package com.example.workipi.data.model

data class Skill(
    val id: String,
    val name: String,
    val unit: MeasureUnit,
    val pricePerUnit: Double,   // RON per unitate
    val pointsPerUnit: Double   // puncte per unitate
)