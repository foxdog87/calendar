package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Long = 0,
    val templateId: Long?,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val color: Int,
    val memo: String?,
    val checkList: String?,
    val latitude: Double?,
    val longitude: Double?,
    val dayCountTarget: Long?,
    val url: String?,
    val attachmentPath: String?,
    val isAutoCompleted: Boolean,
    val completeState: String = "UNCOMPLETED",
    // ★ 修正：Int? 型にして、null を「通知なし」の明示的な状態とする
    val remindMinutes: Int? = null,
    val isAllDay: Boolean = false
)