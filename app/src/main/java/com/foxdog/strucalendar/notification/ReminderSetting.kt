package com.foxdog.strucalendar.notification

import com.foxdog.strucalendar.data.entity.Task

// ★ UI・ドメイン・スケジューラーで共通利用する「真の通知モデル」
sealed interface ReminderSetting {
    data object None : ReminderSetting
    data object AtStartTime : ReminderSetting
    data class Before(val minutes: Int) : ReminderSetting
    data class DayBefore(val daysBack: Int = 1, val hour: Int, val minute: Int) : ReminderSetting
}

// ★ Entity(DB) -> Domain(UI) への変換マッパー
fun Task.getReminderSetting(): ReminderSetting {
    return when (this.reminderType) {
        "NONE", null -> ReminderSetting.None
        "AT_START_TIME" -> ReminderSetting.AtStartTime
        "BEFORE" -> ReminderSetting.Before(minutes = this.reminderOffsetMinutes ?: 0)
        "DAY_BEFORE" -> ReminderSetting.DayBefore(
            daysBack = this.reminderDayOffset ?: 1,
            hour = this.reminderHour ?: 9,
            minute = this.reminderMinute ?: 0
        )
        // 互換性維持（既存データへの対応）
        "THREE_DAYS_BEFORE" -> ReminderSetting.DayBefore(daysBack = 3, hour = 9, minute = 0)
        "MORNING" -> ReminderSetting.DayBefore(daysBack = 0, hour = 9, minute = 0)
        else -> ReminderSetting.None
    }
}