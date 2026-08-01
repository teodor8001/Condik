package com.example.workipi.data.model

enum class UserRole {
    ADMIN,
    MANAGER,
    INGINER,
    SEF_ECHIPA,
    ANGAJAT,
    CLIENT
}

fun UserRole.toDbValue(): String = when (this) {
    UserRole.ADMIN       -> "admin"
    UserRole.MANAGER     -> "manager"
    UserRole.INGINER     -> "inginer"
    UserRole.SEF_ECHIPA  -> "sef_echipa"
    UserRole.ANGAJAT     -> "angajat"
    UserRole.CLIENT      -> "client"
}

fun userRoleFromDbValue(value: String?): UserRole = when (value?.lowercase()) {
    "admin" -> UserRole.ADMIN
    "manager" -> UserRole.MANAGER
    "inginer" -> UserRole.INGINER
    "sef_echipa" -> UserRole.SEF_ECHIPA
    "angajat" -> UserRole.ANGAJAT
    "client" -> UserRole.CLIENT
    else -> UserRole.ANGAJAT
}
