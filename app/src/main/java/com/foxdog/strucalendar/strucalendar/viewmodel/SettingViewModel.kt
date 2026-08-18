package com.foxdog.strucalendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.settings.AppSettings
import com.foxdog.strucalendar.data.settings.AppThemeMode
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class SettingViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotificationEnabled(enabled) }
        AnalyticsLogger.logSettingChanged("notification_enabled")
    }

    fun setDefaultReminderOffsetMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDefaultReminderOffsetMinutes(minutes) }
        AnalyticsLogger.logSettingChanged("default_reminder_offset_minutes")
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
        AnalyticsLogger.logSettingChanged("theme_mode")
    }

    fun setWeekStartDay(day: DayOfWeek) {
        viewModelScope.launch { settingsRepository.setWeekStartDay(day) }
        AnalyticsLogger.logSettingChanged("week_start_day")
    }

    fun setShowTagColorOnCalendar(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowTagColorOnCalendar(enabled) }
        AnalyticsLogger.logSettingChanged("show_tag_color_on_calendar")
    }

    fun setShowCompletedTasks(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowCompletedTasks(enabled) }
        AnalyticsLogger.logSettingChanged("show_completed_tasks")
    }

    fun setShowWeekNumber(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowWeekNumber(enabled) }
        AnalyticsLogger.logSettingChanged("show_week_number")
    }

    fun setHolidayCountryCode(code: String?) {
        viewModelScope.launch { settingsRepository.setHolidayCountryCode(code) }
        AnalyticsLogger.logSettingChanged("holiday_country_code")
    }

    fun setConfirmBeforeDeleteTask(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setConfirmBeforeDeleteTask(enabled) }
        AnalyticsLogger.logSettingChanged("confirm_before_delete_task")
    }

    // ★ 変更：予定一覧画面のオンボーディングも合わせてリセットする
    fun resetTutorialGuides() {
        viewModelScope.launch {
            settingsRepository.setCalendarOnboardingCompleted(false)
            settingsRepository.setTaskCreateOnboardingCompleted(false)
            settingsRepository.setTaskListOnboardingCompleted(false)
        }
        AnalyticsLogger.logSettingChanged("tutorial_guides_reset")
    }
    fun setAlwaysShowDetailedTaskSettings(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAlwaysShowDetailedTaskSettings(enabled) }
        AnalyticsLogger.logSettingChanged("always_show_detailed_task_settings")
    }
}