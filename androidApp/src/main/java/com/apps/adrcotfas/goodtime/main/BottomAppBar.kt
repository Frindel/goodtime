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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.apps.adrcotfas.goodtime.bl.LabelData
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.shared.R
import com.apps.adrcotfas.goodtime.stats.LabelChip
import com.apps.adrcotfas.goodtime.ui.common.BadgedBoxWithCount
import com.apps.adrcotfas.goodtime.ui.getLabelColor
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.BalloonWindow
import com.skydoves.balloon.compose.rememberBalloonBuilder
import com.skydoves.balloon.compose.setBackgroundColor
import com.skydoves.balloon.compose.setTextColor
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Menu2

@Composable
fun BottomAppBar(
    modifier: Modifier,
    badgeItemCount: Int,
    hide: Boolean,
    labelData: LabelData,
    sessionCountToday: Int,
    showTimeProfileTutorial: Boolean,
    onTimeProfileTutorialFinished: () -> Unit,
    onShowSheet: () -> Unit,
    onLabelClick: () -> Unit,
    navController: NavController,
) {
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(
        modifier = modifier,
        visible = !hide,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val isDefaultLabel = labelData.name == Label.DEFAULT_LABEL_NAME
        val color = MaterialTheme.getLabelColor(labelData.colorIndex)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onShowSheet()
                },
            ) {
                BadgedBoxWithCount(count = badgeItemCount) {
                    Icon(
                        imageVector = EvaIcons.Outline.Menu2,
                        contentDescription = stringResource(R.string.main_open_app_menu),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            val onNavigateToSelectLabelDialog = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onLabelClick()
            }
            var balloonWindow: BalloonWindow? by remember { mutableStateOf(null) }
            balloonWindow?.setOnBalloonDismissListener {
                onTimeProfileTutorialFinished()
            }

            val textColor = MaterialTheme.colorScheme.onSurface
            val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
            val builder = rememberBalloonBuilder {
                setArrowSize(10)
                setArrowPosition(0.5f)
                setElevation(1)
                setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                setWidth(BalloonSizeSpec.WRAP)
                setHeight(BalloonSizeSpec.WRAP)
                setBalloonAnimation(BalloonAnimation.FADE)
                setPadding(12)
                setMarginHorizontal(12)
                setCornerRadius(8f)
                setBackgroundColor(backgroundColor)
                setTextColor(textColor)
                setDismissWhenClicked(true)
            }

            Balloon(
                builder = builder,
                onBalloonWindowInitialized = { balloonWindow = it },
                onComposedAnchor = {
                    if (showTimeProfileTutorial) {
                        balloonWindow?.showAlignTop()
                    }
                },
                balloonContent = {
                    Text(text = "To modify the time profile, edit the associated label", style = MaterialTheme.typography.labelMedium)
                },
            ) {
                if (isDefaultLabel) {
                    IconButton(onClick = onNavigateToSelectLabelDialog) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = stringResource(R.string.labels_title),
                            tint = color,
                        )
                    }
                } else {
                    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                        LabelChip(
                            name = labelData.name,
                            color = color,
                            selected = true,
                            showIcon = true,
                        ) { onNavigateToSelectLabelDialog() }
                    }
                }
            }
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navController.navigate(StatsDest)
            }) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        ),
                ) {
                    Text(
                        text = sessionCountToday.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
