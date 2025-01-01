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
package com.apps.adrcotfas.goodtime.labels.archived

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.labels.DeleteConfirmationDialog
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.labels.main.archivedLabels
import com.apps.adrcotfas.goodtime.ui.common.TopBar
import com.apps.adrcotfas.goodtime.ui.localColorsPalette
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.MoreVertical
import compose.icons.evaicons.outline.Trash
import compose.icons.evaicons.outline.Undo
import org.koin.androidx.compose.koinViewModel

const val ARCHIVED_LABELS_SCREEN_DESTINATION_ID = "goodtime.productivity.archivedLabels"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedLabelsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LabelsViewModel = koinViewModel(),
    showTopBar: Boolean,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val labels = uiState.archivedLabels

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var labelToDelete by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopBar(
                title = "Archived labels",
                onNavigateBack = if (showTopBar) onNavigateBack else null,
            )
        },
        content = {
            LazyColumn(
                Modifier
                    .padding(it)
                    .fillMaxSize(),
            ) {
                items(labels, key = { label -> label.name }) { label ->
                    ArchivedLabelListItem(
                        Modifier.animateItem(),
                        label = label,
                        onUnarchive = { viewModel.setArchived(label.name, false) },
                        onDelete = {
                            labelToDelete = label.name
                            showDeleteConfirmationDialog = true
                        },
                        onLastItemUnarchive = if (labels.size == 1) {
                            onNavigateBack
                        } else {
                            null
                        },
                    )
                }
            }
            if (showDeleteConfirmationDialog) {
                DeleteConfirmationDialog(
                    labelToDeleteName = labelToDelete,
                    onConfirm = {
                        val lastLabelDeleted = labels.size == 1
                        viewModel.deleteLabel(labelToDelete)
                        showDeleteConfirmationDialog = false
                        if (lastLabelDeleted) {
                            onNavigateBack()
                        }
                    },
                    onDismiss = { showDeleteConfirmationDialog = false },
                )
            }
        },
    )
}

@Composable
fun ArchivedLabelListItem(
    modifier: Modifier,
    label: Label,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    onLastItemUnarchive: (() -> Unit)? = null,
) {
    val labelName = label.name

    // TODO: add empty state illustration
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
    ) {
        Icon(
            modifier = Modifier
                .padding(8.dp),
            imageVector = Icons.AutoMirrored.Outlined.Label,
            contentDescription = null,
            tint = MaterialTheme.localColorsPalette.colors[label.colorIndex.toInt()],
        )
        Text(
            text = label.name,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.weight(1f))

        var dropDownMenuExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { dropDownMenuExpanded = true }) {
                Icon(EvaIcons.Outline.MoreVertical, contentDescription = "More about $labelName")
            }

            DropdownMenu(
                expanded = dropDownMenuExpanded,
                onDismissRequest = { dropDownMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Unarchive") },
                    onClick = {
                        onUnarchive()
                        dropDownMenuExpanded = false
                        onLastItemUnarchive?.invoke()
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Undo,
                            contentDescription = "Unarchive $labelName",
                        )
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        dropDownMenuExpanded = false
                    },
                    leadingIcon = {
                        Icon(
                            EvaIcons.Outline.Trash,
                            contentDescription = "Delete $labelName",
                        )
                    },
                )
            }
        }
    }
}
