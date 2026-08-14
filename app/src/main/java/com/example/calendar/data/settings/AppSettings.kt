package com.example.calendar.data.settings

import java.time.DayOfWeek

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppSettings(
    val isNotificationEnabled: Boolean = true,
    val defaultReminderOffsetMinutes: Int = 10,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val weekStartDay: DayOfWeek = DayOfWeek.SUNDAY,
    val showTagColorOnCalendar: Boolean = true,
    val showCompletedTasks: Boolean = true,
    val showWeekNumber: Boolean = false
)