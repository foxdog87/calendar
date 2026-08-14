package com.example.calendar.data.converter

import androidx.room.TypeConverter
import com.example.calendar.notification.ReminderType

class ReminderTypeConverter {

    @TypeConverter
    fun fromReminderType(type: ReminderType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toReminderType(value: String?): ReminderType? {
        return value?.let { ReminderType.valueOf(it) }
    }
}