package com.example.workipi.repository

import com.example.workipi.data.model.Project
import com.example.workipi.data.model.ProjectInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "proiecte"

class ProjectRepository @Inject constructor(
    val client: SupabaseClient
) {
    suspend fun getProjectsByCompanyId(companyId: Long): List<Project> =
         client.from(TABLE)
            .select {
                filter {
                    eq("id_firma", companyId)
                    eq("este_oferta", false)
                }
            }
            .decodeList<Project>()

    suspend fun getOffersByCompanyId(companyId: Long): List<Project> =
        client.from(TABLE)
            .select {
                filter {
                    eq("id_firma", companyId)
                    eq("este_oferta", true)
                }
            }
            .decodeList<Project>()

    suspend fun getProjectById(projectId: Long): Project? =
        client.from(TABLE)
            .select {
                filter { eq("id_proiect", projectId)}
            }
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