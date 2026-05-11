package com.example.workipi.repository

import com.example.workipi.data.model.InvitationCode
import com.example.workipi.data.model.InvitationCodeInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

private const val TABLE = "coduri_invitatie"

class InvitationCodeRepository @Inject constructor(
    val client: SupabaseClient,
) {
    suspend fun generateInvitationCode(data: InvitationCodeInsert): InvitationCode =
        client.from(TABLE)
            .insert(data) { select() }
            .decodeSingle()

    suspend fun getCodeByName(code: String): InvitationCode? =
        client.from(TABLE)
            .select {
                filter { eq("cod", code) }
            }
            .decodeSingleOrNull()

    suspend fun deleteByCode(code: String): Result<Unit> = runCatching {
        client.from(TABLE).delete { filter { eq("cod", code) } }
    }
}