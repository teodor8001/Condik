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

    /** Codurile de invitatie nefolosite ale unei firme — angajati invitati care nu si-au activat inca contul. */
    suspend fun getUnusedByCompany(companyId: Long): Result<List<InvitationCode>> = runCatching {
        client.from(TABLE)
            .select {
                filter {
                    eq("id_firma", companyId)
                    eq("este_folosit", false)
                }
            }
            .decodeList()
    }

    suspend fun deleteByCode(code: String): Result<Unit> = runCatching {
        client.from(TABLE).delete { filter { eq("cod", code) } }
    }
}