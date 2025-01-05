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
package com.apps.adrcotfas.goodtime.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.apps.adrcotfas.goodtime.data.local.migrations.MIGRATIONS

@Database(
    entities = [LocalLabel::class, LocalSession::class],
    version = 7,
    exportSchema = true,
)
@ConstructedBy(ProductivityDatabaseConstructor::class)
abstract class ProductivityDatabase : RoomDatabase() {
    abstract fun labelsDao(): LabelDao
    abstract fun sessionsDao(): SessionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ProductivityDatabaseConstructor : RoomDatabaseConstructor<ProductivityDatabase> {
    override fun initialize(): ProductivityDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ProductivityDatabase>,
): ProductivityDatabase {
    return builder
        .addMigrations(*MIGRATIONS)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .build()
}

const val DATABASE_NAME = "goodtime-db"
