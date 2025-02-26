/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.apps.adrcotfas.goodtime.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.data.model.Session
import com.apps.adrcotfas.goodtime.data.model.getLabelData
import com.apps.adrcotfas.goodtime.data.model.toExternal
import com.apps.adrcotfas.goodtime.data.settings.OverviewDurationType
import com.apps.adrcotfas.goodtime.data.settings.OverviewType
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.StatisticsSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val isPro: Boolean = false,
    val labels: List<LabelData> = emptyList(),
    val selectedLabels: List<String> = emptyList(),

    // Selection UI related fields
    val selectedSessions: List<Long> = emptyList(),
    val unselectedSessions: List<Long> = emptyList(), // for the case with select all active
    val selectedSessionsCountWhenAllSelected: Int = 0,
    val isSelectAllEnabled: Boolean = false,
    val selectedLabelToBulkEdit: String? = null,

    // Add/Edit session related fields
    val sessionToEdit: Session? = null, // this does not change after initialization
    val newSession: Session = Session.default(),
    val showAddSession: Boolean = false,
    val canSave: Boolean = true,

    // Overview Tab related fields
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val workDayStart: Int = 0,
    val statisticsSettings: StatisticsSettings = StatisticsSettings(),
    val statisticsData: StatisticsData = StatisticsData(),
) {
    val showSelectionUi: Boolean
        get() = selectedSessions.isNotEmpty() || isSelectAllEnabled

    val selectionCount: Int
        get() =
            if (isSelectAllEnabled) {
                selectedSessionsCountWhenAllSelected - unselectedSessions.size
            } else {
                selectedSessions.size
            }
}

class StatisticsViewModel(
    private val localDataRepo: LocalDataRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState = _uiState
        .onStart { loadData() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatisticsUiState())

    val pagedSessions: Flow<PagingData<Session>> =
        uiState.distinctUntilChanged { old, new ->
            old.selectedLabels == new.selectedLabels
        }.flatMapLatest {
            selectSessionsForTimelinePaged(it.selectedLabels)
        }

    private fun selectSessionsForTimelinePaged(
        labels: List<String>,
    ): Flow<PagingData<Session>> =
        Pager(PagingConfig(pageSize = 50, prefetchDistance = 50)) {
            localDataRepo.selectSessionsForTimelinePaged(labels, showBreaks = true)
        }.flow.map { value ->
            value.map {
                it.toExternal()
            }
        }

    private fun loadData() {
        val settingsFlow = settingsRepository.settings.distinctUntilChanged { old, new ->
            old.firstDayOfWeek == new.firstDayOfWeek &&
                old.workdayStart == new.workdayStart &&
                old.isPro == new.isPro
        }
        val uiStateFlow = uiState.distinctUntilChanged { old, new ->
            old.selectedLabels == new.selectedLabels
        }

        // on first load, selected labels are all labels
        viewModelScope.launch {
            localDataRepo.selectLabelsByArchived(isArchived = false).collect { labels ->
                _uiState.update {
                    it.copy(
                        labels = labels.map { label ->
                            label.getLabelData()
                        },
                        selectedLabels = labels.map { label -> label.name },
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.settings.map { it.statisticsSettings }.collect {
                _uiState.update { state ->
                    state.copy(
                        statisticsSettings = it,
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(settingsFlow, uiStateFlow) { settings, uiState ->
                settings to uiState
            }.flatMapLatest {
                val isPro = it.first.isPro
                val firstDayOfWeek = DayOfWeek(it.first.firstDayOfWeek)
                val workDayStart = it.first.workdayStart
                _uiState.update { uiState ->
                    uiState.copy(
                        isLoading = true,
                        isPro = isPro,
                        workDayStart = workDayStart,
                        firstDayOfWeek = firstDayOfWeek,
                    )
                }
                localDataRepo.selectSessionsByLabels(it.second.selectedLabels)
                    .map { sessions ->
                        withContext(Dispatchers.Default) {
                            computeStatisticsData(
                                sessions = sessions,
                                firstDayOfWeek = firstDayOfWeek,
                                workDayStart = workDayStart,
                            )
                        }
                    }
            }.collect { data ->
                _uiState.update { it.copy(statisticsData = data, isLoading = false) }
            }
        }
    }

    fun setSelectedLabels(selectedLabels: List<String>) {
        _uiState.update { it.copy(selectedLabels = selectedLabels) }
    }

    fun toggleSessionIsSelected(index: Long) {
        _uiState.update {
            if (it.isSelectAllEnabled) {
                val unselectedSessions = it.unselectedSessions.toMutableList()
                if (unselectedSessions.contains(index)) {
                    unselectedSessions.remove(index)
                } else {
                    unselectedSessions.add(index)
                }
                it.copy(
                    unselectedSessions = unselectedSessions,
                    isSelectAllEnabled = unselectedSessions.size != it.selectedSessionsCountWhenAllSelected,
                )
            } else {
                val selectedSessions = it.selectedSessions.toMutableList()
                if (selectedSessions.contains(index)) {
                    selectedSessions.remove(index)
                } else {
                    selectedSessions.add(index)
                }
                it.copy(selectedSessions = selectedSessions)
            }
        }
    }

    fun clearShowSelectionUi() {
        _uiState.update {
            it.copy(
                isSelectAllEnabled = false,
                selectedSessions = emptyList(),
                unselectedSessions = emptyList(),
                selectedSessionsCountWhenAllSelected = 0,
            )
        }
    }

    fun selectAllSessions(allSessionsCount: Int) {
        _uiState.update {
            it.copy(
                isSelectAllEnabled = true,
                selectedSessionsCountWhenAllSelected = allSessionsCount,
                selectedSessions = emptyList(),
                unselectedSessions = emptyList(),
            )
        }
    }

    fun deleteSelectedSessions() {
        viewModelScope.launch {
            if (uiState.value.isSelectAllEnabled) {
                localDataRepo.deleteSessionsExcept(
                    uiState.value.unselectedSessions,
                    uiState.value.selectedLabels,
                )
            } else {
                localDataRepo.deleteSessions(uiState.value.selectedSessions)
            }
        }
    }

    fun updateSessionToEdit(session: Session) {
        _uiState.update { state ->
            state.copy(newSession = session)
        }
    }

    fun setCanSave(isValid: Boolean) {
        _uiState.update { it.copy(canSave = isValid) }
    }

    fun saveSession() {
        viewModelScope.launch {
            val newSession = uiState.value.newSession
            val sessionToEditId = uiState.value.sessionToEdit?.id
            sessionToEditId?.let {
                localDataRepo.updateSession(newSession.id, newSession)
            } ?: localDataRepo.insertSession(newSession)
            settingsRepository.setShouldAskForReview(true)
        }
    }

    fun onAddEditSession(sessionToEdit: Session? = null) {
        val session = sessionToEdit ?: generateNewSession()
        _uiState.update {
            it.copy(
                sessionToEdit = sessionToEdit,
                newSession = session,
                showAddSession = true,
                canSave = sessionToEdit != null,
            )
        }
    }

    fun clearAddEditSession() {
        _uiState.update { it.copy(showAddSession = false) }
    }

    private fun generateNewSession(): Session {
        return Session.create(
            duration = 0,
            timestamp = timeProvider.now(),
            interruptions = 0,
            label = Label.DEFAULT_LABEL_NAME,
            isWork = true,
        )
    }

    fun setSelectedLabelToBulkEdit(label: String) {
        _uiState.update { it.copy(selectedLabelToBulkEdit = label) }
    }

    fun bulkEditLabel() {
        viewModelScope.launch {
            val label = uiState.value.selectedLabelToBulkEdit
            label?.let {
                if (uiState.value.isSelectAllEnabled) {
                    localDataRepo.updateSessionsLabelByIdsExcept(
                        label,
                        uiState.value.unselectedSessions,
                        uiState.value.selectedLabels,
                    )
                } else {
                    localDataRepo.updateSessionsLabelByIds(label, uiState.value.selectedSessions)
                }
            }
        }
    }

    fun setOverviewType(type: OverviewType) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(overviewType = type) }
        }
    }

    fun setOverviewDurationType(type: OverviewDurationType) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(overviewDurationType = type) }
        }
    }

    fun setPieChartViewType(type: OverviewDurationType) {
        viewModelScope.launch {
            settingsRepository.updateStatisticsSettings { it.copy(pieChartViewType = type) }
        }
    }

    fun setShouldAskForReview() = viewModelScope.launch { settingsRepository.setShouldAskForReview(true) }
}
