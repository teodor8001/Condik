package com.example.workipi.util

import java.security.SecureRandom

object InvitationCodeGenerator {
    // Excluse: 0/O, 1/I/L — usor de confundat la introducere manuala
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 8
    private val random = SecureRandom()

    fun generate(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) {
            append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
    }
}