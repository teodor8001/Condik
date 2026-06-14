package com.example.workipi.repository

import com.example.workipi.data.model.Unealta
import com.example.workipi.data.model.UnealtaInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "unelte"

class UneltaRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getByCompany(companyId: Long): Result<List<Unealta>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_firma", companyId) } }
            .decodeList()
    }

    suspend fun add(insert: UnealtaInsert): Result<Unit> = runCatching {
        client.from(TABLE).insert(insert)
    }

    suspend fun updateAvailable(id: Long, available: Int): Result<Unit> = runCatching {
        client.from(TABLE).update({ set("cantitate_disponibila", available) }) {
            filter { eq("id_unealta", id) }
        }
    }

    suspend fun remove(id: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete { filter { eq("id_unealta", id) } }
    }
}
