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
package com.apps.adrcotfas.goodtime.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.bugsnag.BugsnagLogWriter
import co.touchlab.kermit.platformLogWriter
import com.apps.adrcotfas.goodtime.bl.FinishedSessionsHandler
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.createTimeProvider
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepositoryImpl
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.local.backup.BackupManager
import com.apps.adrcotfas.goodtime.data.local.backup.BackupPrompter
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.KoinApplication
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module

const val WORKER_SCOPE = "worker_scope"
const val MAIN_SCOPE = "main_scope"
const val IO_SCOPE = "io_scope"

private val coroutineScopeModule = module {
    single<CoroutineScope>(named(WORKER_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<CoroutineScope>(named(MAIN_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<CoroutineScope>(named(IO_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
}

fun insertKoin(appModule: Module, flavorModule: Module): KoinApplication {
    return startKoin {
        modules(
            appModule,
            flavorModule,
            coroutineScopeModule,
            platformModule,
            coreModule,
            localDataModule,
            timerManagerModule,
            viewModelModule,
        )
    }
}

expect fun isDebug(): Boolean
expect val platformModule: Module

@OptIn(ExperimentalKermitApi::class)
private val coreModule = module {
    val baseLogger =
        Logger(
            config = StaticConfig(
                logWriterList = listOf(platformLogWriter(), BugsnagLogWriter()),
                minSeverity = if (isDebug()) Severity.Verbose else Severity.Info,
            ),
            tag = "Goodtime",
        )
    factory { (tag: String?) -> if (tag != null) baseLogger.withTag(tag) else baseLogger }

    single<LocalDataRepository> {
        LocalDataRepositoryImpl(
            get<ProductivityDatabase>().sessionsDao(),
            get<ProductivityDatabase>().labelsDao(),
            get<CoroutineScope>(named(IO_SCOPE)),
        )
    }
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            get<DataStore<Preferences>>(named(SETTINGS_NAME)),
            getWith("SettingsRepository"),
        )
    }
    single<TimeProvider> {
        createTimeProvider()
    }

    single<FinishedSessionsHandler> {
        FinishedSessionsHandler(
            get<CoroutineScope>(named(IO_SCOPE)),
            get<LocalDataRepository>(),
            get<SettingsRepository>(),
            getWith("FinishedSessionsHandler"),
        )
    }

    single<BackupManager> {
        BackupManager(
            get<FileSystem>(),
            get<String>(named(DB_PATH_KEY)),
            get<String>(named(FILES_DIR_PATH_KEY)),
            get<ProductivityDatabase>(),
            get<TimeProvider>(),
            get<BackupPrompter>(),
            get<LocalDataRepository>(),
            getWith("BackupManager"),
        )
    }
}

internal const val SETTINGS_NAME = "productivity_settings.preferences"
internal const val SETTINGS_FILE_NAME = SETTINGS_NAME + "_pb"
internal const val DB_PATH_KEY = "db_path"
internal const val FILES_DIR_PATH_KEY = "tmp_path"

internal fun getDataStore(producePath: () -> String): DataStore<Preferences> {
    return PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })
}

inline fun <reified T> Scope.getWith(vararg params: Any?): T {
    return get(parameters = { parametersOf(*params) })
}

fun KoinComponent.injectLogger(tag: String): Lazy<Logger> = inject { parametersOf(tag) }
