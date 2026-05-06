package com.example.workipi.repository

import com.example.workipi.data.model.User
import com.example.workipi.data.model.UtilizatorInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val client: SupabaseClient,
) {

    suspend fun findByAuthId(authUserId: String): User? =
        client.from(TABLE)
            .select { filter { eq("auth_utilizator_id", authUserId) } }
            .decodeSingleOrNull()

    suspend fun findByPhoneNumber(phoneNumber: String): User? =
        client.from(TABLE)
            .select { filter { eq("numar_telefon", phoneNumber) } }
            .decodeSingleOrNull()

    suspend fun findByEmail(email: String): User? =
        client.from(TABLE)
            .select { filter { eq("email", email) } }
            .decodeSingleOrNull()

    suspend fun insertUserAccount(data: UtilizatorInsert): User =
        client.from(TABLE)
            .insert(data) { select() }
            .decodeSingle()

    companion object {
        private const val TABLE = "utilizatori"
    }
}