// Task.kt (Entity)
package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val taskId: Long = 0,

    val templateId: Long?,

    val title: String,
    val startTime: Long,
    val endTime: Long,

    val color: Int,
    val memo: String?,

    // ▼ 場所情報
    // OSM検索結果から取得した場所名・住所・座標を保存
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,

    val dayCountTarget: Long?,
    val url: String?,
    val attachmentPath: String?,

    val isAutoCompleted: Boolean,

    val completeState: String = "UNCOMPLETED",

    val isAllDay: Boolean = false,

    // ▼ 通知関連（フラットなDBカラムとして定義）
    val reminderType: String? = null,
    // "NONE", "AT_START_TIME", "BEFORE", "DAY_BEFORE"

    val reminderOffsetMinutes: Int? = null,
    // ○分前

    val reminderDayOffset: Int? = null,
    // ○日前（通常は1）

    val reminderHour: Int? = null,
    // 時

    val reminderMinute: Int? = null
    // 分
)