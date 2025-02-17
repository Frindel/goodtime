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
import androidx.core.content.FileProvider
import com.apps.adrcotfas.goodtime.ActivityProvider
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.local.backup.BackupType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class RestoreActivityResultLauncherManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) {
    private var importActivityResultLauncher: ManagedActivityResultLauncher<String, Uri?>? = null
    private var importedFilePath: String? = null
    private var importCallback: (suspend () -> Unit)? = null

    fun setImportActivityResultLauncher(importActivityResultLauncher: ManagedActivityResultLauncher<String, Uri?>) {
        this.importActivityResultLauncher = importActivityResultLauncher
    }

    fun launch(importedFilePath: String, callback: suspend () -> Unit) {
        this.importedFilePath = importedFilePath
        this.importCallback = callback

        importActivityResultLauncher?.launch("application/zip")
    }

    fun importCallback(uri: Uri?) {
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            inputStream.use { input ->
                Files.copy(
                    input,
                    Paths.get(importedFilePath!!),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }

            coroutineScope.launch {
                importCallback!!.invoke()
            }
        }
    }
}

class AndroidBackupPrompter(
    private val context: Context,
    private val activityProvider: ActivityProvider,
    private val activityResultLauncherManager: RestoreActivityResultLauncherManager,
) : BackupPrompter {
    override suspend fun promptUserForBackup(backupType: BackupType, fileToSharePath: okio.Path) {
        delay(100)
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = when (backupType) {
                BackupType.DB -> "application/zip"
                BackupType.JSON -> "application/json"
                BackupType.CSV -> "text/csv"
            }
            putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    fileToSharePath.toFile(),
                ),
            )
            flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }

        activityProvider.activeActivity?.startActivity(
            Intent.createChooser(
                intent,
                "Backup",
            ),
        ) ?: throw IllegalStateException("No activity found")
    }

    override suspend fun promptUserForRestore(
        importedFilePath: String,
        callback: suspend () -> Unit,
    ) {
        activityResultLauncherManager.launch(importedFilePath, callback)
    }
}
