package com.example.workipi.data.model

data class MockUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val role: UserRole,
    val idCompany: Long? = null,
)