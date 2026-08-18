package com.foxdog.strucalendar.state

import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.TagCustomField
import com.foxdog.strucalendar.data.recurrence.RecurrenceType
import com.foxdog.strucalendar.notification.ReminderSetting

data class TemplateInputState(
    val title: String = "",
    val durationMinutes: Int = 60,
    val description: String = "",
    val memo: String = "",
    val checkList: List<ChecklistItem> = emptyList(),
    val color: Int? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isAutoCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val reminderSetting: ReminderSetting = ReminderSetting.None,
    val selectedTags: List<Tag> = emptyList(),

    val customFields: List<TagCustomField> = emptyList(),
    val customFieldValues: Map<Long, String> = emptyMap(),

    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceIntervalDays: Int = 1,
    val recurrenceNth: Int = 1,
    val recurrenceWeekday: Int = 1,
    val recurrenceEndTime: Long? = null
)