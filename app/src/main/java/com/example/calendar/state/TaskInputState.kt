package com.example.calendar.state

import com.example.calendar.data.entity.Tag
import java.time.LocalDateTime
import java.time.ZoneOffset

data class TaskInputState(
    val templateId: Long? = null,
    val title: String = "",
    val startTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
    val endTime: Long = LocalDateTime.now().plusHours(1).toEpochSecond(ZoneOffset.UTC),
    val color: Int? = null,
    val memo: String = "",
    val checkList: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dayCountTarget: Long? = null,
    val url: String = "",
    val attachmentPath: String = "",
    val isAutoCompleted: Boolean = false,
    val selectedTags: List<Tag> = emptyList(),
    val remindMinutes: Int? = null
)