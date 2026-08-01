package com.example.workipi.data.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SiteAttendance(
    @SerialName("id_prezenta") val id: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("data") val date: LocalDate,
    @SerialName("status") val status: String,
    @SerialName("ora_intrare") val checkIn: LocalTime? = null,
    @SerialName("ora_iesire") val checkOut: LocalTime? = null,
    @SerialName("observatii") val notes: String? = null,
    @SerialName("inregistrat_de") val recordedBy: Long,
)

@Serializable
data class SiteAttendanceInsert(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("id_utilizator") val userId: Long,
    @SerialName("data") val date: LocalDate,
    @SerialName("status") val status: String,
    @SerialName("ora_intrare") val checkIn: LocalTime? = null,
    @SerialName("ora_iesire") val checkOut: LocalTime? = null,
    @SerialName("observatii") val notes: String? = null,
    @SerialName("inregistrat_de") val recordedBy: Long,
)

@Serializable
data class SiteCorrection(
    @SerialName("id_corectura") val id: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("id_zona") val zoneId: Long? = null,
    @SerialName("titlu") val title: String,
    @SerialName("descriere") val description: String,
    @SerialName("severitate") val severity: String,
    @SerialName("status") val status: String,
    @SerialName("atribuit_catre") val assignedTo: Long? = null,
    @SerialName("creat_de") val createdBy: Long,
    @SerialName("termen") val dueDate: LocalDate? = null,
    @SerialName("rezolvat_la") val resolvedAt: Instant? = null,
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
data class SiteCorrectionInsert(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("id_zona") val zoneId: Long? = null,
    @SerialName("titlu") val title: String,
    @SerialName("descriere") val description: String,
    @SerialName("severitate") val severity: String,
    @SerialName("atribuit_catre") val assignedTo: Long? = null,
    @SerialName("creat_de") val createdBy: Long,
    @SerialName("termen") val dueDate: LocalDate? = null,
)

@Serializable
data class SiteJournalEntry(
    @SerialName("id_intrare") val id: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("data") val date: LocalDate,
    @SerialName("tip") val type: String,
    @SerialName("titlu") val title: String,
    @SerialName("descriere") val description: String,
    @SerialName("severitate") val severity: String? = null,
    @SerialName("cale_fotografie") val photoPath: String? = null,
    @SerialName("creat_de") val createdBy: Long,
    @SerialName("created_at") val createdAt: Instant,
)

@Serializable
data class SiteJournalEntryInsert(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("data") val date: LocalDate,
    @SerialName("tip") val type: String,
    @SerialName("titlu") val title: String,
    @SerialName("descriere") val description: String,
    @SerialName("severitate") val severity: String? = null,
    @SerialName("cale_fotografie") val photoPath: String? = null,
    @SerialName("creat_de") val createdBy: Long,
)

@Serializable
data class SiteDailyClosure(
    @SerialName("id_inchidere") val id: Long,
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("data") val date: LocalDate,
    @SerialName("rezumat") val summary: String,
    @SerialName("blocaje") val blockers: String? = null,
    @SerialName("plan_urmatoarea_zi") val nextDayPlan: String? = null,
    @SerialName("inchis_de") val closedBy: Long,
    @SerialName("inchis_la") val closedAt: Instant,
    @SerialName("redeschis_la") val reopenedAt: Instant? = null,
)

@Serializable
data class SiteDailyClosureInsert(
    @SerialName("id_proiect") val projectId: Long,
    @SerialName("data") val date: LocalDate,
    @SerialName("rezumat") val summary: String,
    @SerialName("blocaje") val blockers: String? = null,
    @SerialName("plan_urmatoarea_zi") val nextDayPlan: String? = null,
    @SerialName("inchis_de") val closedBy: Long,
)
