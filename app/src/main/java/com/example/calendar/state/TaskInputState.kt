package com.example.calendar.state

import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.notification.ReminderSetting

data class TaskInputState(
    val title: String = "",

    val startTime: Long = 0L,
    val endTime: Long = 0L,

    val memo: String = "",

    // ▼ チェックリスト
    val checkList: List<ChecklistItem> = emptyList(),

    val color: Int? = null,

    val attachmentPath: String = "",
    val url: String = "",

    // ▼ 場所情報
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    val isAutoCompleted: Boolean = false,

    val reminderSetting: ReminderSetting = ReminderSetting.None,

    val selectedTags: List<Tag> = emptyList(),

    val isAllDay: Boolean = false
)