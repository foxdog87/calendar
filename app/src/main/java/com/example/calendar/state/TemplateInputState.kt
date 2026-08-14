package com.example.calendar.state

import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.entity.Tag
import com.example.calendar.notification.ReminderSetting

data class TemplateInputState(
    val title: String = "",
    val durationMinutes: Int = 60,
    val memo: String = "",
    val checkList: List<ChecklistItem> = emptyList(),
    val color: Int? = null,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isAutoCompleted: Boolean = false,
    val reminderSetting: ReminderSetting = ReminderSetting.None,
    val selectedTags: List<Tag> = emptyList()
)