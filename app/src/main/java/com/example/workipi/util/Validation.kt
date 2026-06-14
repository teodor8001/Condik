package com.example.workipi.util

/**
 * Validari reutilizabile pentru stringurile din aplicatie (email, telefon).
 * Mesajele sunt in romana ca sa fie afisate direct in UI.
 */
object Validation {

    // Email simplu: ceva@ceva.ceva, fara spatii.
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    // Telefon: exact 10 cifre (format RO), doar numere.
    private val PHONE_REGEX = Regex("^\\d{10}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPhone(phone: String): Boolean = PHONE_REGEX.matches(phone.trim())

    /** Returneaza un mesaj de eroare daca email-ul e invalid, altfel null. */
    fun emailError(email: String): String? =
        if (isValidEmail(email)) null else "Email invalid — trebuie sa contina @ si un domeniu (ex: nume@firma.ro)."

    /** Returneaza un mesaj de eroare daca telefonul e invalid, altfel null. */
    fun phoneError(phone: String): String? =
        if (isValidPhone(phone)) null else "Numarul de telefon trebuie sa aiba exact 10 cifre."
}
