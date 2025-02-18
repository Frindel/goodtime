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

import com.apps.adrcotfas.goodtime.data.local.backup.BackupViewModel
import com.apps.adrcotfas.goodtime.labels.main.LabelsViewModel
import com.apps.adrcotfas.goodtime.main.TimerViewModel
import com.apps.adrcotfas.goodtime.main.finishedsession.FinishedSessionViewModel
import com.apps.adrcotfas.goodtime.onboarding.MainViewModel
import com.apps.adrcotfas.goodtime.settings.SettingsViewModel
import com.apps.adrcotfas.goodtime.stats.StatisticsHistoryViewModel
import com.apps.adrcotfas.goodtime.stats.StatisticsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

actual val viewModelModule: Module = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::TimerViewModel)
    viewModelOf(::FinishedSessionViewModel)
    viewModelOf(::LabelsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { BackupViewModel(get(), get(), get(named(IO_SCOPE))) }
    viewModelOf(::StatisticsViewModel)
    viewModelOf(::StatisticsHistoryViewModel)
}
