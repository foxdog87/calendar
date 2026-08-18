// data/preference/CalendarPreferences.kt
package com.foxdog.strucalendar.data.preference

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foxdog.strucalendar.viewmodel.CalendarDisplayMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.calendarDataStore by preferencesDataStore(name = "calendar_prefs")

class CalendarPreferences(private val context: Context) {
    private val displayModeKey = stringPreferencesKey("display_mode")

    suspend fun getDisplayMode(): CalendarDisplayMode {
        val saved = context.calendarDataStore.data
            .map { it[displayModeKey] }
            .first()
        return saved?.let { runCatching { CalendarDisplayMode.valueOf(it) }.getOrNull() }
            ?: CalendarDisplayMode.MONTH
    }

    suspend fun saveDisplayMode(mode: CalendarDisplayMode) {
        context.calendarDataStore.edit { it[displayModeKey] = mode.name }
    }
}