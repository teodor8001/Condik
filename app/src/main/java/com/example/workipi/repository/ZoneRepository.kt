package com.example.workipi.repository

import com.example.workipi.data.model.Zone
import com.example.workipi.data.model.ZoneInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "zone"

class ZoneRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getZonesForProject(projectId: Long): Result<List<Zone>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_proiect", projectId) } }
            .decodeList()
    }

    suspend fun createZone(insert: ZoneInsert): Result<Zone> = runCatching {
        client.from(TABLE)
            .insert(insert) { select() }
            .decodeSingle()
    }

    suspend fun createZones(rows: List<ZoneInsert>): Result<Unit> = runCatching {
        if (rows.isNotEmpty()) {
            client.from(TABLE).insert(rows)
        }
    }

    suspend fun getZonesForProjects(projectIds: List<Long>): Result<List<Zone>> = runCatching {
        if (projectIds.isEmpty()) emptyList()
        else client.from(TABLE)
            .select { filter { isIn("id_proiect", projectIds) } }
            .decodeList()
    }

    suspend fun addCompletedSurface(zoneId: Long, delta: Float): Result<Unit> = runCatching {
        val zone = client.from(TABLE)
            .select { filter { eq("id_zona", zoneId) } }
            .decodeSingleOrNull<Zone>()
            ?: error("Zona $zoneId nu exista")
        val newValue = (zone.surfaceCompleted ?: 0f) + delta
        client.from(TABLE).update(
            { set("suprafata_terminata", newValue) }
        ) {
            filter { eq("id_zona", zoneId) }
        }
    }
}
