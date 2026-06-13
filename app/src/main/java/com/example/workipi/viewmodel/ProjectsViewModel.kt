package com.example.workipi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workipi.data.mock.MockSession
import com.example.workipi.data.model.Project
import com.example.workipi.data.model.Zone
import com.example.workipi.repository.ProjectRepository
import com.example.workipi.repository.ZoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectWithProgress(
    val project: Project,
    val totalSurface: Float,
    val completedSurface: Float,
    val revisionsToDo: Int = 0,
) {
    val progress: Float
        get() = if (totalSurface <= 0f) 0f else (completedSurface / totalSurface).coerceIn(0f, 1f)

    val mpPerDay: Float
        get() {
            val today = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val start = project.startDate
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val days = (today.toEpochDays() - start.toEpochDays()).coerceAtLeast(1)
            return completedSurface / days
        }
}

data class ProjectsUiState(
    val projects: List<ProjectWithProgress> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val zoneRepository: ZoneRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        val companyId = MockSession.currentUser?.idCompany
        if (companyId == null) {
            _uiState.update { it.copy(errorMessage = "Nu am putut identifica firma. Reautentifica-te.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val projects = projectRepository.getProjectsByCompanyId(companyId)
                val zones = zoneRepository.getZonesForProjects(projects.map { it.projectId })
                    .getOrDefault(emptyList())
                val zonesByProject = zones.groupBy { it.projectId }
                val withProgress = projects.map { p ->
                    val pZones = zonesByProject[p.projectId].orEmpty()
                    ProjectWithProgress(
                        project = p,
                        totalSurface = pZones.sumOf { it.surface.toDouble() }.toFloat(),
                        completedSurface = pZones.sumOf { (it.surfaceCompleted ?: 0f).toDouble() }.toFloat(),
                    )
                }
                _uiState.update { it.copy(isLoading = false, projects = withProgress) }
            } catch (e: Throwable) {
                Log.e(TAG, "Eroare la incarcarea proiectelor", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Nu am putut incarca proiectele.",
                    )
                }
            }
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
                .onSuccess { loadProjects() }
                .onFailure { e ->
                    Log.e(TAG, "Stergerea proiectului a esuat", e)
                    _uiState.update {
                        it.copy(errorMessage = e.message ?: "Stergerea proiectului a esuat.")
                    }
                }
        }
    }

    companion object {
        private const val TAG = "ProjectsVM"
    }
}
