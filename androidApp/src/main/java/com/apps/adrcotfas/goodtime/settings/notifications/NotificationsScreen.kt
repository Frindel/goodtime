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
package com.apps.adrcotfas.goodtime.settings.notifications

import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.notifications.TorchManager
import com.apps.adrcotfas.goodtime.bl.notifications.VibrationPlayer
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.ui.common.BetterListItem
import com.apps.adrcotfas.goodtime.ui.common.CheckboxListItem
import com.apps.adrcotfas.goodtime.ui.common.CompactPreferenceGroupTitle
import com.apps.adrcotfas.goodtime.ui.common.SliderListItem
import com.apps.adrcotfas.goodtime.ui.common.SubtleHorizontalDivider
import com.apps.adrcotfas.goodtime.ui.common.TimePicker
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.common.toSecondOfDay
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: SettingsViewModel = koinViewModel(viewModelStoreOwner = LocalActivity.current as ComponentActivity),
    onNavigateBack: () -> Boolean,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

    val vibrationPlayer = koinInject<VibrationPlayer>()
    val torchManager = koinInject<TorchManager>()
    val isTorchAvailable = torchManager.isTorchAvailable()
    val workRingTone = toSoundData(settings.workFinishedSound)
    val breakRingTone = toSoundData(settings.breakFinishedSound)
    val candidateRingTone = uiState.notificationSoundCandidate?.let { toSoundData(it) }

    val listState = rememberScrollState()
    Scaffold(
        topBar = {
            TopBar(
                title = "Notifications",
                onNavigateBack = { onNavigateBack() },
                showSeparator = listState.canScrollBackward,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(listState)
                .background(MaterialTheme.colorScheme.background),
        ) {
            CompactPreferenceGroupTitle(text = "Productivity Reminder")
            val reminderSettings = settings.productivityReminderSettings
            ProductivityReminderListItem(
                firstDayOfWeek = DayOfWeek(settings.firstDayOfWeek),
                selectedDays = reminderSettings.days.map { DayOfWeek(it) }.toSet(),
                reminderSecondOfDay = reminderSettings.secondOfDay,
                onSelectDay = viewModel::onToggleProductivityReminderDay,
                onReminderTimeClick = { viewModel.setShowTimePicker(true) },
            )
            SubtleHorizontalDivider()
            CompactPreferenceGroupTitle(text = "Notifications")
            BetterListItem(
                title = "Work finished sound",
                subtitle = notificationSoundName(workRingTone),
                onClick = { viewModel.setShowSelectWorkSoundPicker(true) },
            )

            BetterListItem(
                title = "Break finished sound",
                subtitle = notificationSoundName(breakRingTone),
                onClick = { viewModel.setShowSelectBreakSoundPicker(true) },
            )

            CheckboxListItem(
                title = "Override sound profile",
                subtitle = "The notification sound behaves like an alarm",
                checked = settings.overrideSoundProfile,
            ) {
                viewModel.setOverrideSoundProfile(it)
            }

            var selectedStrength = settings.vibrationStrength
            SliderListItem(
                title = "Vibration strength",
                value = settings.vibrationStrength,
                min = 0,
                max = 5,
                onValueChange = {
                    selectedStrength = it
                    viewModel.setVibrationStrength(it)
                },
                onValueChangeFinished = { vibrationPlayer.start(selectedStrength) },
            )
            if (isTorchAvailable) {
                CheckboxListItem(
                    title = "Torch",
                    subtitle = "A visual notification for silent environments",
                    checked = settings.enableTorch,
                ) {
                    viewModel.setEnableTorch(it)
                }
            }

            CheckboxListItem(
                title = "Insistent notification",
                subtitle = "Repeat the notification until it's cancelled",
                checked = settings.insistentNotification,
            ) {
                viewModel.setInsistentNotification(it)
            }
            CheckboxListItem(
                title = "Auto start work",
                subtitle = "Start the work session after a break without user interaction",
                checked = settings.autoStartWork,
            ) {
                viewModel.setAutoStartWork(it)
            }
            CheckboxListItem(
                title = "Auto start break",
                subtitle = "Start the break session after a work session without user interaction",
                checked = settings.autoStartBreak,
            ) {
                viewModel.setAutoStartBreak(it)
            }
        }

        if (uiState.showSelectWorkSoundPicker) {
            NotificationSoundPickerDialog(
                title = "Work finished sound",
                selectedItem = candidateRingTone ?: workRingTone,
                onSelected = {
                    viewModel.setNotificationSoundCandidate(Json.encodeToString(it))
                },
                onSave = { viewModel.setWorkFinishedSound(Json.encodeToString(it)) },
                onDismiss = { viewModel.setShowSelectWorkSoundPicker(false) },
            )
        }
        if (uiState.showSelectBreakSoundPicker) {
            NotificationSoundPickerDialog(
                title = "Break finished sound",
                selectedItem = candidateRingTone ?: breakRingTone,
                onSelected = {
                    viewModel.setNotificationSoundCandidate(Json.encodeToString(it))
                },
                onSave = { viewModel.setBreakFinishedSound(Json.encodeToString(it)) },
                onDismiss = { viewModel.setShowSelectBreakSoundPicker(false) },
            )
        }
        if (uiState.showTimePicker) {
            val reminderTime =
                LocalTime.fromSecondOfDay(settings.productivityReminderSettings.secondOfDay)
            val timePickerState = rememberTimePickerState(
                initialHour = reminderTime.hour,
                initialMinute = reminderTime.minute,
                is24Hour = DateFormat.is24HourFormat(context),
            )
            TimePicker(
                onDismiss = { viewModel.setShowTimePicker(false) },
                onConfirm = {
                    viewModel.setReminderTime(timePickerState.toSecondOfDay())
                    viewModel.setShowTimePicker(false)
                },
                timePickerState = timePickerState,
            )
        }
    }
}

@Composable
private fun notificationSoundName(it: SoundData) =
    if (it.isSilent) {
        "Silent"
    } else if (it.name.isEmpty()) {
        "Default notification sound"
    } else {
        it.name
    }
