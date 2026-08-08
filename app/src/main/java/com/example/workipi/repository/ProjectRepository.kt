package com.example.workipi.repository

import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TABLE = "proiecte"

class ProjectRepository @Inject constructor(
    val client: SupabaseClient
) {
    suspend fun getProjectsByCompanyId(companyId: Long): List<Project> =
         client.postgrest
            .rpc("get_visible_projects")
            .decodeList<Project>()
            .filter { it.companyId == companyId && !it.isOffer }

    suspend fun getOffersByCompanyId(companyId: Long): List<Project> =
        client.postgrest
            .rpc("get_visible_projects")
            .decodeList<Project>()
            .filter { it.companyId == companyId && it.isOffer }

    suspend fun getProjectById(projectId: Long): Project? =
        client.postgrest
            .rpc(
                "get_visible_project",
                buildJsonObject { put("p_project_id", projectId) },
            )
            .decodeSingleOrNull()

    suspend fun createProject(data: ProjectInsert): Result<Project> = runCatching {
        client.from(TABLE)
            .insert(data) { select() }
            .decodeSingle()
    }

    suspend fun updateSalaryCost(projectId: Long, value: Float): Result<Unit> = runCatching {
        client.from(TABLE).update(
            { set("costuri_salarii", value) }
        ) {
            filter { eq("id_proiect", projectId) }
        }
    }

    suspend fun acceptOffer(projectId: Long): Result<Unit> = runCatching {
        client.from(TABLE).update({ set("este_oferta", false) }) {
            filter { eq("id_proiect", projectId) }
        }
    }

    suspend fun deleteProject(projectId: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete {
            filter { eq("id_proiect", projectId) }
        }
    }
}
