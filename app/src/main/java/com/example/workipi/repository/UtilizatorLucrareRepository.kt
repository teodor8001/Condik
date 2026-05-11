package com.example.workipi.repository

import com.example.workipi.data.model.UtilizatorLucrare
import com.example.workipi.data.model.UtilizatorLucrareInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "utilizatori_lucrari"

class UtilizatorLucrareRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun assignSkills(rows: List<UtilizatorLucrareInsert>): Result<Unit> = runCatching {
        if (rows.isNotEmpty()) {
            client.from(TABLE).insert(rows)
        }
    }

    suspend fun getSkillsForUser(userId: Long): Result<List<UtilizatorLucrare>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_utilizator", userId) } }
            .decodeList()
    }

    suspend fun removeSkill(userId: Long, idLucrare: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete {
            filter {
                eq("id_utilizator", userId)
                eq("id_lucrare", idLucrare)
            }
        }
    }

    suspend fun updateLevel(userId: Long, idLucrare: Long, level: String): Result<Unit> = runCatching {
        client.from(TABLE).update(
            { set("skill_level", level) }
        ) {
            filter {
                eq("id_utilizator", userId)
                eq("id_lucrare", idLucrare)
            }
        }
    }
}
