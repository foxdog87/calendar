package com.example.calendar.data.settings

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
            showWeekNumber = prefs[Keys.SHOW_WEEK_NUMBER] ?: false
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
}