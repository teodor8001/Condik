package com.example.workipi.data.model

data class Employee(
    val id: String,
    val name: String,
    val age: Int,
    val email: String,
    val phone: String,
    val primarySpecialty: String,       // afisata in lista
    val specialties: List<String>,      // toate specializarile cunoscute
    val level: EmployeeLevel,
    val points: Int
)