package com.example.calendar.data.entity

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class Task(
    val taskId: String = UUID.randomUUID().toString(),
    val templateId: Int? = null,
    val title: String,
    val date: LocalDate,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val color: Int,
    val memo: String? = null,
    val checkList: String? = null,
    val location: String? = null,
    val dayCountTarget: LocalDate? = null, // あなたが必要と判断した目標日
    val url: String? = null,
    val attachmentPath: String? = null,
    val autoCompleted: Boolean = false,
    val completeState: String = "NOT_COMPLETED"
)