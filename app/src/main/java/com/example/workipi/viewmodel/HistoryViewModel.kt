package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.History
import com.example.workipi.data.model.Lucrare
import com.example.workipi.data.model.HistoryInsert
import com.example.workipi.data.model.Zone
import com.example.workipi.repository.AuthRepository
import com.example.workipi.repository.SkillRepository
import com.example.workipi.repository.HistoryRepository
import com.example.workipi.repository.UserRepository
import com.example.workipi.repository.ZoneHistoryRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

private const val UNIT_MP = "mp"

data class PontareUiState(
    val skills: List<Lucrare> = emptyList(),
    val zones: List<Zone> = emptyList(),
    val selectedSkillId: Long? = null,
    val selectedZoneId: Long? = null,
    val quantity: String = "",
    val hours: String = "",
    val quality: Int = 5,
    val workDateMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val duplicateWarning: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val skillRepository: SkillRepository,
    private val zoneRepository: ZoneRepository,
    private val historyRepository: HistoryRepository,
    private val zoneHistoryRepository: ZoneHistoryRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PontareUiState())
    val uiState: StateFlow<PontareUiState> = _uiState.asStateFlow()

    fun load(projectId: Long) {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val skills = skillRepository.getSkillsForCompany(companyId).getOrNull() ?: emptyList()
            val zones = zoneRepository.getZonesForProject(projectId).getOrNull() ?: emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    skills = skills,
                    zones = zones,
                    selectedZoneId = zones.firstOrNull()?.id,
                    errorMessage = if (zones.isEmpty()) "Proiectul nu are zone definite." else null,
                )
            }
        }
    }

    fun selectSkill(id: Long) =
        _uiState.update { it.copy(selectedSkillId = id, errorMessage = null) }

    fun selectZone(id: Long) =
        _uiState.update { it.copy(selectedZoneId = id, errorMessage = null) }

    fun onQuantityChange(value: String) =
        _uiState.update { it.copy(quantity = value.filter { c -> c.isDigit() || c == '.' }, errorMessage = null) }

    fun onHoursChange(value: String) =
        _uiState.update { it.copy(hours = value.filter { c -> c.isDigit() || c == '.' }, errorMessage = null) }

    fun onQualityChange(value: Int) =
        _uiState.update { it.copy(quality = value.coerceIn(1, 5), errorMessage = null) }

    fun onWorkDateChange(millis: Long) =
        _uiState.update { it.copy(workDateMillis = millis, errorMessage = null) }

    fun submit(userId: Long) = trySubmit(userId, force = false)

    /** Salveaza chiar daca exista deja o pontare identica (userul a confirmat in dialog). */
    fun confirmSubmitAnyway(userId: Long) = trySubmit(userId, force = true)

    fun dismissDuplicateWarning() = _uiState.update { it.copy(duplicateWarning = false) }

    private fun trySubmit(userId: Long, force: Boolean) {
        val state = _uiState.value
        val skill = state.skills.firstOrNull { it.id == state.selectedSkillId }
        val quantity = state.quantity.toFloatOrNull()
        val hours = state.hours.toDoubleOrNull()
        val zoneId = state.selectedZoneId

        val error = when {
            zoneId == null -> "Alege o zona."
            skill == null -> "Alege tipul de lucrare."
            quantity == null || quantity <= 0f -> "Cantitatea trebuie sa fie un numar pozitiv."
            hours == null || hours <= 0.0 -> "Numarul de ore trebuie sa fie un numar pozitiv."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val workDate = Instant.fromEpochMilliseconds(state.workDateMillis)
                    .toLocalDateTime(TimeZone.UTC).date

                // Verifica daca exista deja o pontare identica in proiect (acelasi angajat,
                // aceeasi lucrare, aceeasi zi) — ca sa nu se dubleze din greseala.
                if (!force) {
                    val zoneIds = state.zones.map { it.id }
                    val existing = historyRepository.getByZones(zoneIds).getOrDefault(emptyList())
                    val isDuplicate = existing.any {
                        it.userId == userId && it.idLucrare == skill!!.id && it.workDate == workDate
                    }
                    if (isDuplicate) {
                        _uiState.update { it.copy(isSaving = false, duplicateWarning = true) }
                        return@launch
                    }
                }

                historyRepository.createHistory(
                    HistoryInsert(
                        userId = userId,
                        idLucrare = skill!!.id,
                        idZona = zoneId!!,
                        hours = hours!!,
                        quantity = quantity!!,
                        quality = state.quality.toFloat(),
                        workDate = workDate,
                    )
                ).getOrThrow()

                val earnedPoints = skill.points.toDouble() * quantity
                userRepository.addPoints(userId, earnedPoints).getOrThrow()

                // Increment progres lucrare in zone_lucrari (sursa de adevar pentru bara de progres)
                zoneHistoryRepository.incrementCompleted(zoneId, skill.id, quantity)
                    .onFailure { e -> Log.e(TAG, "Increment cantitate_lucrata esuat", e) }

                if (skill.unit.equals(UNIT_MP, ignoreCase = true)) {
                    zoneRepository.addCompletedSurface(zoneId, quantity).getOrThrow()
                }

                _uiState.update { it.copy(isSaving = false, saved = true, duplicateWarning = false) }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la salvarea pontarii", e)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Salvarea a esuat. Incearca din nou.",
                    )
                }
            }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }

    companion object {
        private const val TAG = "PontareVM"
    }
}
