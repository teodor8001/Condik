package com.example.workipi.repository

import com.example.workipi.data.model.History
import com.example.workipi.data.model.HistoryInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

private const val TABLE = "istoric_pontari"

class HistoryRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun createHistory(insert: HistoryInsert): Result<Unit> = runCatching {
        client.from(TABLE).insert(insert)
    }

    suspend fun getByUser(userId: Long): Result<List<History>> = runCatching {
        client.from(TABLE)
            .select {
                filter { eq("id_utilizator", userId) }
                order("data_pontarii", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun getByZones(zoneIds: List<Long>): Result<List<History>> = runCatching {
        if (zoneIds.isEmpty()) emptyList()
        else client.from(TABLE)
            .select {
                filter { isIn("id_zona", zoneIds) }
                order("data_pontarii", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun getBySkill(skillId: Long): Result<List<History>> = runCatching {
        client.from(TABLE)
            .select {
                filter { eq("id_lucrare", skillId) }
                order("data_pontarii", Order.DESCENDING)
            }
            .decodeList()
    }

}
