package com.example.workipi.repository

import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.LucrareInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "lucrari"

class SkillRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getSkillsForCompany(companyId: Long): Result<List<Lucrare>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_firma", companyId) } }
            .decodeList()
    }

    suspend fun createSkill(insert: LucrareInsert): Result<Lucrare> = runCatching {
        client.from(TABLE)
            .insert(insert) { select() }
            .decodeSingle()
    }

    suspend fun updateSkill(
        id: Long,
        name: String,
        unit: String,
        price: Float,
        points: Long,
    ): Result<Unit> = runCatching {
        client.from(TABLE).update(
            {
                set("denumire", name)
                set("unitate_masura", unit)
                set("pret", price)
                set("punctaj", points)
            }
        ) {
            filter { eq("id_lucrare", id) }
        }
    }

    suspend fun deleteSkill(id: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete {
            filter { eq("id_lucrare", id) }
        }
    }
}
