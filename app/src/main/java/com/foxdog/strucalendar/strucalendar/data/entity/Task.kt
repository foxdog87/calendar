package com.foxdog.strucalendar.data.entity

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
    val isPinned: Boolean = false,

    // ▼ 通知関連
    val reminderType: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val reminderDayOffset: Int? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,

    // ▼ 追加：繰り返し関連
    // recurrenceGroupId: 同じ繰り返しシリーズを識別するUUID。繰り返しなしのタスクはnull。
    val recurrenceGroupId: String? = null,
    // recurrenceType: "INTERVAL_DAYS" または "MONTHLY_NTH_WEEKDAY"。繰り返しなしはnull。
    val recurrenceType: String? = null,
    // X日ごと の X（type=INTERVAL_DAYSのとき使用）
    val recurrenceIntervalDays: Int? = null,
    // 第何週か（1〜4、5=最終週）（type=MONTHLY_NTH_WEEKDAYのとき使用）
    val recurrenceNth: Int? = null,
    // 曜日（DayOfWeek.value: 月=1〜日=7）（type=MONTHLY_NTH_WEEKDAYのとき使用）
    val recurrenceWeekday: Int? = null,
    // 繰り返しの最終日（epoch秒、その日の0時などで保存）
    val recurrenceEndDate: Long? = null
)