package com.example.calendar.state

import com.example.calendar.data.entity.Tag

data class TaskInputState(
    val title: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val memo: String = "",
    val checkList: String = "",
    val color: Int? = null,
    val attachmentPath: String = "",
    val url: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isAutoCompleted: Boolean = false,
    val remindMinutes: Int? = null,
    val selectedTags: List<Tag> = emptyList(),

    val isAllDay: Boolean = false // ★ この1行を最後（または途中）に追記してください！
)