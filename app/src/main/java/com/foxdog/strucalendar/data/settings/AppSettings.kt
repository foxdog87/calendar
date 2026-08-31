package com.foxdog.strucalendar.data.settings

import java.time.DayOfWeek

data class AppSettings(
    val isNotificationEnabled: Boolean = false,
    val defaultReminderOffsetMinutes: Int = 10,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val weekStartDay: DayOfWeek = DayOfWeek.SUNDAY,
    val showTagColorOnCalendar: Boolean = true,
    val showCompletedTasks: Boolean = true,
    val showWeekNumber: Boolean = false,
    val autoCompleteOverdueTasks: Boolean = false,
    val holidayCountryCode: String? = null,
    val confirmBeforeDeleteTask: Boolean = true,
    val confirmDiscardChanges: Boolean = true,
    val calendarOnboardingCompleted: Boolean = false,
    val taskCreateOnboardingCompleted: Boolean = false,
    val taskListOnboardingCompleted: Boolean = false,
    // タスク作成画面で詳細設定を常に表示するかどうか
    val alwaysShowDetailedTaskSettings: Boolean = false,
    // 週表示の「選択日の予定」を時刻表レイアウトで表示するかどうか（falseはリスト表示）
    val weekDayPreviewIsTimetable: Boolean = false
)

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}