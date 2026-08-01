package com.example.workipi.repository

import com.example.workipi.data.model.SiteAttendance
import com.example.workipi.data.model.SiteAttendanceInsert
import com.example.workipi.data.model.SiteCorrection
import com.example.workipi.data.model.SiteCorrectionInsert
import com.example.workipi.data.model.SiteDailyClosure
import com.example.workipi.data.model.SiteDailyClosureInsert
import com.example.workipi.data.model.SiteJournalEntry
import com.example.workipi.data.model.SiteJournalEntryInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes

@Singleton
class SiteOperationsRepository @Inject constructor(
    private val client: SupabaseClient,
) {
    suspend fun getAttendance(projectId: Long, date: LocalDate): List<SiteAttendance> =
        client.from("prezente_santier").select {
            filter {
                eq("id_proiect", projectId)
                eq("data", date.toString())
            }
        }.decodeList()

    suspend fun saveAttendance(data: SiteAttendanceInsert) {
        val existing = client.from("prezente_santier").select {
            filter {
                eq("id_proiect", data.projectId)
                eq("id_utilizator", data.userId)
                eq("data", data.date.toString())
            }
        }.decodeSingleOrNull<SiteAttendance>()

        if (existing == null) {
            client.from("prezente_santier").insert(data)
        } else {
            client.from("prezente_santier").update({
                set("status", data.status)
                set("ora_intrare", data.checkIn)
                set("ora_iesire", data.checkOut)
                set("observatii", data.notes)
                set("inregistrat_de", data.recordedBy)
            }) {
                filter { eq("id_prezenta", existing.id) }
            }
        }
    }

    suspend fun getCorrections(projectId: Long): List<SiteCorrection> =
        client.from("corecturi_santier").select {
            filter { eq("id_proiect", projectId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun createCorrection(data: SiteCorrectionInsert) {
        client.from("corecturi_santier").insert(data)
    }

    suspend fun updateCorrectionStatus(id: Long, status: String) {
        client.from("corecturi_santier").update({
            set("status", status)
            if (status == "rezolvata") set("rezolvat_la", Clock.System.now())
            else set("rezolvat_la", null as String?)
        }) { filter { eq("id_corectura", id) } }
    }

    suspend fun getJournal(projectId: Long): List<SiteJournalEntry> =
        client.from("jurnal_santier").select {
            filter { eq("id_proiect", projectId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun createJournalEntry(data: SiteJournalEntryInsert) {
        client.from("jurnal_santier").insert(data)
    }

    suspend fun uploadPhoto(projectId: Long, bytes: ByteArray, extension: String): String {
        val path = "$projectId/${Clock.System.now().toEpochMilliseconds()}.$extension"
        client.storage.from(PHOTO_BUCKET).upload(path, bytes) { upsert = false }
        return path
    }

    suspend fun createPhotoUrl(path: String): String =
        client.storage.from(PHOTO_BUCKET).createSignedUrl(path, 30.minutes)

    suspend fun getClosure(projectId: Long, date: LocalDate): SiteDailyClosure? =
        client.from("inchideri_santier").select {
            filter {
                eq("id_proiect", projectId)
                eq("data", date.toString())
            }
        }.decodeSingleOrNull()

    suspend fun closeDay(data: SiteDailyClosureInsert) {
        client.from("inchideri_santier").insert(data)
    }

    companion object {
        const val PHOTO_BUCKET = "santier-fotografii"
    }
}
