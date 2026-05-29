package com.example.workipi.repository

import com.example.workipi.data.model.ZoneHistory
import com.example.workipi.data.model.ZoneHistoryInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "zone_lucrari"

class ZoneHistoryRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getByZone(zoneId: Long): Result<List<ZoneHistory>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_zona", zoneId) } }
            .decodeList()
    }

    suspend fun getByZones(zoneIds: List<Long>): Result<List<ZoneHistory>> = runCatching {
        if (zoneIds.isEmpty()) emptyList()
        else client.from(TABLE)
            .select { filter { isIn("id_zona", zoneIds) } }
            .decodeList()
    }

    suspend fun add(insert: ZoneHistoryInsert): Result<ZoneHistory> = runCatching {
        client.from(TABLE)
            .insert(insert) { select() }
            .decodeSingle()
    }

    suspend fun updateQuantity(zoneId: Long, skillId: Long, newQuantity: Float): Result<Unit> = runCatching {
        client.from(TABLE).update(
            { set("cantitate_totala", newQuantity) }
        ) {
            filter {
                eq("id_zona", zoneId)
                eq("id_lucrare", skillId)
            }
        }
    }

    suspend fun remove(zoneId: Long, skillId: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete {
            filter {
                eq("id_zona", zoneId)
                eq("id_lucrare", skillId)
            }
        }
    }

    suspend fun incrementCompleted(zoneId: Long, skillId: Long, delta: Float): Result<Unit> = runCatching {
        val current = client.from(TABLE)
            .select { filter { eq("id_zona", zoneId); eq("id_lucrare", skillId) } }
            .decodeSingleOrNull<ZoneHistory>()
            ?: error("zone_lucrari nu contine (zona=$zoneId, lucrare=$skillId)")
        val newValue = current.completedQuantity + delta
        client.from(TABLE).update({ set("cantitate_lucrata", newValue) }) {
            filter { eq("id_zona", zoneId); eq("id_lucrare", skillId) }
        }
    }
}
