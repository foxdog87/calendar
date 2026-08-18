package com.foxdog.strucalendar.data.converter

import androidx.room.TypeConverter
import com.foxdog.strucalendar.notification.ReminderType

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