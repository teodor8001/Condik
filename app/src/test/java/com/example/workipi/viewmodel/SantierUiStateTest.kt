package com.example.workipi.viewmodel

import com.example.workipi.data.model.SiteAttendance
import com.example.workipi.data.model.SiteCorrection
import com.example.workipi.data.model.SiteJournalEntry
import com.example.workipi.data.model.User
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SantierUiStateTest {
    @Test
    fun dailyMetricsAggregateOperationalData() {
        val user = User(
            idUser = 10L,
            fullName = "Angajat Test",
            email = "test@example.com",
            phoneNumber = 700000000L,
        )
        val date = LocalDate(2026, 8, 2)
        val state = SantierUiState(
            attendance = listOf(
                SiteAttendanceRow(
                    user,
                    SiteAttendance(1L, 2L, 10L, date, "prezent", recordedBy = 11L),
                )
            ),
            workItems = listOf(SiteWorkRow("Etaj 1", "Glet", "mp", 40f, 100f, 12.5f)),
            corrections = listOf(
                SiteCorrection(
                    id = 1L,
                    projectId = 2L,
                    title = "Muchie",
                    description = "Necesită refacere",
                    severity = "medie",
                    status = "deschisa",
                    createdBy = 11L,
                    createdAt = Instant.parse("2026-08-02T08:00:00Z"),
                )
            ),
            journal = listOf(
                SiteJournalEntry(
                    id = 1L,
                    projectId = 2L,
                    date = date,
                    type = "incident",
                    title = "Incident minor",
                    description = "Fără accidentare",
                    createdBy = 11L,
                    createdAt = Instant.parse("2026-08-02T09:00:00Z"),
                )
            ),
        )

        assertEquals(1, state.presentCount)
        assertEquals(12.5f, state.todayQuantity)
        assertEquals(1, state.openCorrections)
        assertEquals(0.4f, state.workItems.single().progress)
    }

    @Test
    fun workProgressIsBounded() {
        assertEquals(1f, SiteWorkRow("Z", "L", "mp", 120f, 100f, 0f).progress)
        assertEquals(0f, SiteWorkRow("Z", "L", "mp", 10f, 0f, 0f).progress)
    }
}
