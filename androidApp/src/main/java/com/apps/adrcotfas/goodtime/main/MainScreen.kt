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
package com.apps.adrcotfas.goodtime.main

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.isBreak
import com.apps.adrcotfas.goodtime.bl.isWork
import com.apps.adrcotfas.goodtime.common.isPortrait
import com.apps.adrcotfas.goodtime.common.screenWidth
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialConfig
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControl
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControlButton
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialRegion
import com.apps.adrcotfas.goodtime.main.dialcontrol.rememberDialControlState
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.settings.timerstyle.InitTimerStyle
import com.apps.adrcotfas.goodtime.ui.common.DragHandle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity),
    settingsViewModel: SettingsViewModel = koinViewModel(),
) {
    val mainUiState by viewModel.uiState.collectAsStateWithLifecycle(MainUiState())
    if (mainUiState.isLoading) return
    InitTimerStyle(settingsViewModel)

    val timerUiState by viewModel.timerUiState.collectAsStateWithLifecycle(TimerUiState())
    val historyUiState by viewModel.historyUiState.collectAsStateWithLifecycle(HistoryUiState())

    val timerStyle = mainUiState.timerStyle
    val label = timerUiState.label

    val configuration = LocalConfiguration.current
    val dialControlState = rememberDialControlState(
        options = DialRegion.entries,
        config = DialConfig(size = configuration.screenWidth),
        onSelected = {
            when (it) {
                DialRegion.TOP -> {
                    viewModel.addOneMinute()
                }

                DialRegion.RIGHT -> {
                    viewModel.skip()
                }

                DialRegion.BOTTOM -> {
                    viewModel.resetTimer()
                }

                else -> {
                }
            }
        },
    )

    val yOffset = remember { Animatable(0f) }
    ScreensaverMode(
        screensaverMode = mainUiState.screensaverMode,
        isActive = timerUiState.isActive,
        screenWidth = configuration.screenWidth,
        yOffset = yOffset,
    )

    val thereIsNoBreakBudget =
        timerUiState.breakBudgetMinutes == 0L
    val isCountUpWithoutBreaks = !label.profile.isCountdown && !label.profile.isBreakEnabled

    val disabledOptions = listOfNotNull(
        DialRegion.LEFT,
        if (!label.profile.isCountdown) {
            DialRegion.TOP
        } else {
            null
        },
        if ((!label.profile.isCountdown && thereIsNoBreakBudget && timerUiState.timerType.isWork) ||
            isCountUpWithoutBreaks
        ) {
            DialRegion.RIGHT
        } else {
            null
        },
    )

    dialControlState.updateEnabledOptions(disabledOptions)

    val gestureModifier = dialControlState.let {
        Modifier
            .pointerInput(it) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    it.onDown()
                    var change =
                        awaitTouchSlopOrCancellation(pointerId = down.id) { change, _ ->
                            change.consume()
                        }
                    while (change != null && change.pressed) {
                        change = awaitDragOrCancellation(change.id)?.also { inputChange ->
                            if (inputChange.pressed && timerUiState.isActive) {
                                dialControlState.onDrag(dragAmount = inputChange.positionChange())
                            }
                        }
                    }
                    it.onRelease()
                }
            }
    }

    val alphaModifier = Modifier.graphicsLayer {
        alpha = if (dialControlState.isDragging) 0.38f else 1f
    }

    val backgroundColor by animateColorAsState(
        if (mainUiState.isDarkTheme(isSystemInDarkTheme()) &&
            mainUiState.trueBlackMode &&
            timerUiState.isActive
        ) {
            Color.Black
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "main background color",
    )

    AnimatedVisibility(timerUiState.isReady, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val modifier = Modifier.offset {
                if (configuration.isPortrait) {
                    IntOffset(
                        0,
                        yOffset.value.roundToInt(),
                    )
                } else {
                    IntOffset(yOffset.value.roundToInt(), 0)
                }
            }

            MainTimerView(
                modifier = alphaModifier.then(modifier),
                state = dialControlState,
                gestureModifier = gestureModifier,
                timerUiState = timerUiState,
                timerStyle = timerStyle,
                domainLabel = label,
                onStart = viewModel::startTimer,
                onToggle = viewModel::toggleTimer,
            )
            DialControl(
                modifier = modifier,
                state = dialControlState,
                dialContent = { region ->
                    DialControlButton(
                        disabled = dialControlState.isDisabled(region),
                        selected = region == dialControlState.selectedOption,
                        region = region,
                    )
                },
            )
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showBottomSheet by rememberSaveable(timerUiState.isFinished) { mutableStateOf(timerUiState.isFinished) }

    val hideSheet = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                showBottomSheet = false
            }
        }
    }

    if (showBottomSheet) {
        var updateWorkTime by rememberSaveable { mutableStateOf(false) }
        val isBreak = timerUiState.timerType.isBreak

        var notes by rememberSaveable { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = {
                viewModel.resetTimer(updateWorkTime = updateWorkTime)
                viewModel.updateNotesForLastCompletedSession(notes)
                showBottomSheet = false
            },
            dragHandle = {
                DragHandle(
                    buttonText = if (isBreak) "Start work" else "Start break",
                    onClose = {
                        viewModel.resetTimer(updateWorkTime = updateWorkTime)
                        viewModel.updateNotesForLastCompletedSession(notes)
                        hideSheet()
                    },
                    onClick = {
                        viewModel.next(updateWorkTime = updateWorkTime)
                        viewModel.updateNotesForLastCompletedSession(notes)
                        hideSheet()
                    },
                    isEnabled = true,
                )
            },
            sheetState = sheetState,
        ) {
            FinishedSessionContent(
                timerUiState = timerUiState,
                historyUiState = historyUiState,
                addIdleMinutes = updateWorkTime,
                onChangeAddIdleMinutes = { updateWorkTime = it },
                notes = notes,
                onNotesChanged = { notes = it },
            )
        }
    }
}
