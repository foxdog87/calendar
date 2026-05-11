package com.example.calendar.state // パッケージ名を state に変更

import java.time.LocalDate
import java.time.LocalTime

data class TaskInputState(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val color: Int? = null,
    val memo: String = "",
    val dayCountTarget: LocalDate? = null
)