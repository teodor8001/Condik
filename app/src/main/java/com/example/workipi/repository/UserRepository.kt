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

    suspend fun findById(id: Long): Result<User?> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_utilizator", id) } }
            .decodeSingleOrNull()
    }

    suspend fun insertUserAccount(data: UtilizatorInsert): User =
        client.from(TABLE)
            .insert(data) { select() }
            .decodeSingle()

    suspend fun getEmployeesByCompanyId(companyId: Long): Result<List<User>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_firma", companyId) } }
            .decodeList()
    }

    suspend fun updateEmployee(
        userId: Long,
        fullName: String,
        phoneNumber: Long,
        role: String,
        salary: Double?,
    ): Result<Unit> = runCatching {
        client.from(TABLE).update(
            {
                set("nume_prenume", fullName)
                set("numar_telefon", phoneNumber)
                set("rol", role)
                set("salariu", salary)
            }
        ) {
            filter { eq("id_utilizator", userId) }
        }
    }

    suspend fun addPoints(userId: Long, delta: Double): Result<Unit> = runCatching {
        val user = findById(userId).getOrThrow()
            ?: error("Utilizatorul $userId nu exista")
        val newValue = (user.points ?: 0.0) + delta
        client.from(TABLE).update(
            { set("punctaj", newValue) }
        ) {
            filter { eq("id_utilizator", userId) }
        }
    }

    /**
     * Sterge un angajat si toate dependintele lui (skills, asignari proiecte, istoric pontari).
     * Operatie ireversibila — istoricul pontarilor se pierde.
     */
    suspend fun setCheckedIn(userId: Long, value: Boolean): Result<Unit> = runCatching {
        client.from(TABLE).update(
            { set("este_checked_in", value) }
        ) {
            filter { eq("id_utilizator", userId) }
        }
    }

    suspend fun countCheckedIn(companyId: Long): Result<Long> = runCatching {
        client.from(TABLE)
            .select {
                filter {
                    eq("id_firma", companyId)
                    eq("este_checked_in", true)
                }
            }
            .decodeList<User>()
            .size.toLong()
    }

    suspend fun deleteEmployee(userId: Long): Result<Unit> = runCatching {
        client.from("utilizatori_lucrari").delete { filter { eq("id_utilizator", userId) } }
        client.from("utilizatori_proiecte").delete { filter { eq("id_utilizator", userId) } }
        client.from("istoric_pontari").delete { filter { eq("id_utilizator", userId) } }
        client.from(TABLE).delete { filter { eq("id_utilizator", userId) } }
    }

    companion object {
        private const val TABLE = "utilizatori"
    }
}