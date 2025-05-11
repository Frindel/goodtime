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
package com.apps.adrcotfas.goodtime.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.local.backup.BackupType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class ActivityResultLauncherManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private var importActivityResultLauncher: ManagedActivityResultLauncher<String, Uri?>? = null
    private var backupActivityResultLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>? = null
    private var importedFilePath: String? = null
    private var exportedFilePath: okio.Path? = null
    private var importCallback: (suspend (Boolean) -> Unit)? = null
    private var exportCallback: (suspend (Boolean) -> Unit)? = null

    fun setup(
        importActivityResultLauncher: ManagedActivityResultLauncher<String, Uri?>,
        backupActivityResultLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    ) {
        this.importActivityResultLauncher = importActivityResultLauncher
        this.backupActivityResultLauncher = backupActivityResultLauncher
    }

    fun launchImport(importedFilePath: String, callback: suspend (Boolean) -> Unit) {
        this.importedFilePath = importedFilePath
        this.importCallback = callback

        importActivityResultLauncher?.launch("application/*")
    }

    fun launchExport(intent: Intent, exportedFilePath: okio.Path, callback: suspend (Boolean) -> Unit) {
        backupActivityResultLauncher?.launch(intent)
        this.exportedFilePath = exportedFilePath
        this.exportCallback = callback
    }

    fun importCallback(uri: Uri?) {
        uri?.let {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.let { input ->
                        Files.copy(
                            input,
                            Paths.get(importedFilePath!!),
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                        input.close()
                        importCallback!!.invoke(true)
                    } ?: importCallback!!.invoke(false)
                }
            }
        }
    }

    fun exportCallback(uri: Uri?) {
        uri?.let {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.let { output ->
                        Files.newInputStream(exportedFilePath!!.toNioPath()).use { input ->
                            input.copyTo(output)
                        }
                        output.close()
                        exportCallback!!.invoke(true)
                    } ?: exportCallback!!.invoke(false)
                }
            }
        }
    }
}

class AndroidBackupPrompter(
    private val activityResultLauncherManager: ActivityResultLauncherManager,
) : BackupPrompter {
    override suspend fun promptUserForBackup(
        backupType: BackupType,
        fileToSharePath: okio.Path,
        callback: suspend (Boolean) -> Unit,
    ) {
        delay(100)
        val intent = Intent().apply {
            action = Intent.ACTION_CREATE_DOCUMENT
            type = when (backupType) {
                BackupType.DB -> "application/*"
                BackupType.JSON -> "application/json"
                BackupType.CSV -> "text/csv"
            }
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileToSharePath.name)
        }
        activityResultLauncherManager.launchExport(intent, fileToSharePath, callback)
    }

    override suspend fun promptUserForRestore(
        importedFilePath: String,
        callback: suspend (Boolean) -> Unit,
    ) {
        activityResultLauncherManager.launchImport(importedFilePath, callback)
    }
}
