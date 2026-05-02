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
}