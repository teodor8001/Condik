package com.example.workipi.repository

import com.example.workipi.data.model.InvitationCodeLucrare
import com.example.workipi.data.model.InvitationCodeLucrareInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "coduri_invitatie_lucrari"

class InvitationCodeLucrareRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun assignSkillsToCode(rows: List<InvitationCodeLucrareInsert>): Result<Unit> = runCatching {
        if (rows.isNotEmpty()) {
            client.from(TABLE).insert(rows)
        }
    }

    suspend fun getSkillsForCode(codeId: Long): Result<List<InvitationCodeLucrare>> = runCatching {
        client.from(TABLE)
            .select { filter { eq("id_cod", codeId) } }
            .decodeList()
    }
}
