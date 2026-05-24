package com.example.calendar.data.entity

import java.time.LocalDateTime // ★インポート

data class Task(
    val taskId: Int,
    val title: String,
    // ★修正：日付＋時刻をセットで保持する構造へアップデート
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val color: Int,
    val memo: String? = null,
    val location: String? = null,
    val url: String? = null,
    val checkList: String? = null,
    val attachmentPath: String? = null,
    val dayCountTarget: Boolean = false,
    val completeState: String = "NOT_COMPLETED",
    val autoCompleted: Boolean = false
)