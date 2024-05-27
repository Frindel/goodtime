package com.apps.adrcotfas.goodtime.data.model

import com.apps.adrcotfas.goodtime.LocalLabel
import com.apps.adrcotfas.goodtime.LocalSession

fun Label.toLocal(): LocalLabel {
    return LocalLabel(
        id = id,
        name = name,
        colorIndex = colorIndex,
        orderIndex = orderIndex,
        useDefaultTimeProfile = useDefaultTimeProfile,
        isCountdown = timerProfile.isCountdown,
        workDuration = timerProfile.workDuration,
        breakDuration = timerProfile.breakDuration,
        longBreakDuration = timerProfile.longBreakDuration,
        sessionsBeforeLongBreak = timerProfile.sessionsBeforeLongBreak,
        workBreakRatio = timerProfile.workBreakRatio,
        isArchived = isArchived
    )
}

fun toExternalLabelMapper(
    id: Long,
    name: String,
    colorIndex: Long,
    orderIndex: Long,
    useDefaultTimeProfile: Boolean,
    isCountdown: Boolean,
    workDuration: Int,
    breakDuration: Int,
    longBreakDuration: Int,
    sessionsBeforeLongBreak: Int,
    workBreakRatio: Int,
    isArchived: Boolean,
) = Label(
    id = id,
    name = name,
    colorIndex = colorIndex,
    orderIndex = orderIndex,
    useDefaultTimeProfile = useDefaultTimeProfile,
    timerProfile = TimerProfile(
        isCountdown = isCountdown,
        workDuration = workDuration,
        breakDuration = breakDuration,
        longBreakDuration = longBreakDuration,
        sessionsBeforeLongBreak = sessionsBeforeLongBreak,
        workBreakRatio = workBreakRatio
    ),
    isArchived = isArchived
)

fun Session.toLocal() = LocalSession(
    id = id,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    duration = duration,
    labelName = label,
    notes = notes,
    isWork = isWork,
    isArchived = isArchived
)

fun toExternalSessionMapper(
    id: Long,
    startTimestamp: Long,
    endTimestamp: Long,
    duration: Long,
    label: String?,
    notes: String?,
    isWork: Boolean,
    isArchived: Boolean
) = Session(
    id = id,
    startTimestamp = startTimestamp,
    endTimestamp = endTimestamp,
    duration = duration,
    label = label,
    notes = notes,
    isWork = isWork,
    isArchived = isArchived
)