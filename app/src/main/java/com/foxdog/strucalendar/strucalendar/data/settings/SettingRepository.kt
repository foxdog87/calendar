package com.foxdog.strucalendar.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEEK_START_DAY = stringPreferencesKey("week_start_day")
        val SHOW_TAG_COLOR = booleanPreferencesKey("show_tag_color")
        val SHOW_COMPLETED_TASKS = booleanPreferencesKey("show_completed_tasks")
        val SHOW_WEEK_NUMBER = booleanPreferencesKey("show_week_number")
        val AUTO_COMPLETE_OVERDUE = booleanPreferencesKey("auto_complete_overdue")
        val HOLIDAY_COUNTRY_CODE = stringPreferencesKey("holiday_country_code")
        val CONFIRM_BEFORE_DELETE_TASK = booleanPreferencesKey("confirm_before_delete_task")

        val CALENDAR_ONBOARDING_COMPLETED = booleanPreferencesKey("calendar_onboarding_completed")
        val TASK_CREATE_ONBOARDING_COMPLETED = booleanPreferencesKey("task_create_onboarding_completed")
        val TASK_LIST_ONBOARDING_COMPLETED = booleanPreferencesKey("task_list_onboarding_completed")

        // ★ 追加：タスク作成時の詳細設定を常に表示するフラグ
        val ALWAYS_SHOW_DETAILED_TASK_SETTINGS = booleanPreferencesKey("always_show_detailed_task_settings")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            isNotificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true,
            defaultReminderOffsetMinutes = prefs[Keys.DEFAULT_REMINDER_MINUTES] ?: 10,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            weekStartDay = prefs[Keys.WEEK_START_DAY]?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
                ?: DayOfWeek.SUNDAY,
            showTagColorOnCalendar = prefs[Keys.SHOW_TAG_COLOR] ?: true,
            showCompletedTasks = prefs[Keys.SHOW_COMPLETED_TASKS] ?: true,
            showWeekNumber = prefs[Keys.SHOW_WEEK_NUMBER] ?: false,
            autoCompleteOverdueTasks = prefs[Keys.AUTO_COMPLETE_OVERDUE] ?: false,
            holidayCountryCode = prefs[Keys.HOLIDAY_COUNTRY_CODE],
            confirmBeforeDeleteTask = prefs[Keys.CONFIRM_BEFORE_DELETE_TASK] ?: true,
            calendarOnboardingCompleted = prefs[Keys.CALENDAR_ONBOARDING_COMPLETED] ?: false,
            taskCreateOnboardingCompleted = prefs[Keys.TASK_CREATE_ONBOARDING_COMPLETED] ?: false,
            taskListOnboardingCompleted = prefs[Keys.TASK_LIST_ONBOARDING_COMPLETED] ?: false,

            // ★ 追加：設定値の読み込み（初期値は false）
            alwaysShowDetailedTaskSettings = prefs[Keys.ALWAYS_SHOW_DETAILED_TASK_SETTINGS] ?: false
        )
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setDefaultReminderOffsetMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_REMINDER_MINUTES] = minutes }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setWeekStartDay(day: DayOfWeek) {
        context.settingsDataStore.edit { it[Keys.WEEK_START_DAY] = day.name }
    }

    suspend fun setShowTagColorOnCalendar(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_TAG_COLOR] = enabled }
    }

    suspend fun setShowCompletedTasks(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_COMPLETED_TASKS] = enabled }
    }

    suspend fun setShowWeekNumber(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_WEEK_NUMBER] = enabled }
    }

    suspend fun setAutoCompleteOverdueTasks(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_COMPLETE_OVERDUE] = enabled }
    }

    suspend fun setHolidayCountryCode(code: String?) {
        context.settingsDataStore.edit { prefs ->
            if (code == null) {
                prefs.remove(Keys.HOLIDAY_COUNTRY_CODE)
            } else {
                prefs[Keys.HOLIDAY_COUNTRY_CODE] = code
            }
        }
    }

    suspend fun setConfirmBeforeDeleteTask(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.CONFIRM_BEFORE_DELETE_TASK] = enabled }
    }

    suspend fun setCalendarOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.CALENDAR_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setTaskCreateOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.TASK_CREATE_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setTaskListOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.TASK_LIST_ONBOARDING_COMPLETED] = completed }
    }

    // ★ 追加：設定値の書き込み用関数
    suspend fun setAlwaysShowDetailedTaskSettings(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.ALWAYS_SHOW_DETAILED_TASK_SETTINGS] = enabled }
    }
}