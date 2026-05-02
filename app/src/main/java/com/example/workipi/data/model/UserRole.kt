package com.example.workipi.data.model

enum class UserRole {
    ADMIN,
    PROJECT_MANAGER,
    ANGAJAT,
    CLIENT
}

fun UserRole.toDbValue(): String = when (this) {
    UserRole.ADMIN           -> "admin"
    UserRole.PROJECT_MANAGER -> "inginer"
    UserRole.ANGAJAT         -> "angajat"
    UserRole.CLIENT          -> "client"
}
