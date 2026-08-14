package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true) val templateId: Long = 0,
    val title: String,
    val icon: String?,
    val timeLength: Long,
    val description: String?,
    val color: Int,
    val memo: String?,
    val locationName: String? = null,
    val locationAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dayCountTarget: Long?,
    val url: String?,
    val attachmentPath: String?,
    val isAutoCompleted: Boolean,

    val reminderType: String? = null,
    val reminderOffsetMinutes: Int? = null,
    val reminderDayOffset: Int? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,

    val position: Int = 0,

    // ▼ 追加：最後にタスク作成へ適用した時刻(Unix time ms)。未使用ならnull。
    val lastUsedAt: Long? = null
)