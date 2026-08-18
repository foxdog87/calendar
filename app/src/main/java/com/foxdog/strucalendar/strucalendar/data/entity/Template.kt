package com.foxdog.strucalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val templateId: Long = 0,
    val title: String,
    val icon: String?,
    val timeLength: Long,
    val description: String?,
    val color: Int,
    val memo: String?,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dayCountTarget: Long?,
    val url: String?,
    val attachmentPath: String?,
    val isAutoCompleted: Boolean,
    val isAllDay: Boolean = false,

    val reminderType: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val reminderDayOffset: Int? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,

    val position: Int = 0,

    val lastUsedAt: Long? = null,

    // ▼ 追加：繰り返し関連（Taskと同じ意味）。テンプレート適用時にTaskInputStateへコピーする。
    val recurrenceType: String? = null,
    val recurrenceIntervalDays: Int? = null,
    val recurrenceNth: Int? = null,
    val recurrenceWeekday: Int? = null,
    val recurrenceEndDate: Long? = null
)