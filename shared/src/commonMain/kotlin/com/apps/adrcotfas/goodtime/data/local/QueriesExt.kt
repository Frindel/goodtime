package com.apps.adrcotfas.goodtime.data.local

import com.apps.adrcotfas.goodtime.LocalLabel
import com.apps.adrcotfas.goodtime.LocalLabelQueries
import com.apps.adrcotfas.goodtime.LocalSession
import com.apps.adrcotfas.goodtime.LocalSessionQueries
import com.apps.adrcotfas.goodtime.data.model.TimerProfile

fun LocalLabelQueries.insert(label: LocalLabel) {
    insert(
        name = label.name,
        colorIndex = label.colorIndex,
        orderIndex = label.orderIndex,
        useDefaultTimeProfile = label.useDefaultTimeProfile,
        isCountdown = label.isCountdown,
        workDuration = label.workDuration,
        breakDuration = label.breakDuration,
        longBreakDuration = label.longBreakDuration,
        sessionsBeforeLongBreak = label.sessionsBeforeLongBreak,
        workBreakRatio = label.workBreakRatio,
        isArchived = label.isArchived
    )
}

fun LocalSessionQueries.insert(session: LocalSession) {
    insert(
        startTimestamp = session.startTimestamp,
        endTimestamp = session.endTimestamp,
        duration = session.duration,
        labelName = session.labelName,
        notes = session.notes,
        isWork = session.isWork,
        isArchived = session.isArchived
    )
}

fun LocalSessionQueries.update(id: Long, newSession: LocalSession) {
    update(
        newStartTimestamp = newSession.startTimestamp,
        newEndTimestamp = newSession.endTimestamp,
        newDuration = newSession.duration,
        newLabel = newSession.labelName,
        newNotes = newSession.notes,
        id = id
    )
}