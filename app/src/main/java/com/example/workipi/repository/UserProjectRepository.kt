package com.example.workipi.repository

import com.example.workipi.data.model.UserProject
import com.example.workipi.data.model.UserProjectInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "utilizatori_proiecte"

class UserProjectRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getAssignmentsForProject(projectId: Long): Result<List<UserProject>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_proiect", projectId) } }
            .decodeList()
    }

    suspend fun assignUser(userId: Long, projectId: Long): Result<Unit> = runCatching {
        client.from(TABLE).insert(UserProjectInsert(userId = userId, projectId = projectId))
    }

    suspend fun unassignUser(userId: Long, projectId: Long): Result<Unit> = runCatching {
        client.from(TABLE).delete {
            filter {
                eq("id_utilizator", userId)
                eq("id_proiect", projectId)
            }
        }
    }
}
