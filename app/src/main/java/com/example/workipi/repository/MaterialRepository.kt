package com.example.workipi.repository

import com.example.workipi.data.model.Material
import com.example.workipi.data.model.MaterialInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "materiale"

class MaterialRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getByProject(projectId: Long): Result<List<Material>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_proiect", projectId) } }
            .decodeList()
    }

    suspend fun add(insert: MaterialInsert): Result<Material> = runCatching {
        client.from(TABLE)
            .insert(insert) { select() }
            .decodeSingle()
    }

    suspend fun update(
        id: Long,
        name: String,
        quantity: Float,
        unit: String,
        unitCost: Float,
    ): Result<Unit> = runCatching {
        client.from(TABLE).update(
            {
                set("denumire", name)
                set("cantitate", quantity)
                set("unitate_masura", unit)
                set("cost_unitate", unitCost)
            }
        ) {
            filter { eq("id_material", id) }
        }
    }

    suspend fun remove(id: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete {
            filter { eq("id_material", id) }
        }
    }
}
