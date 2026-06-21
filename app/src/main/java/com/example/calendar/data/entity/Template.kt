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
    val checkList: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dayCountTarget: Long?,
    val url: String?,
    val attachmentPath: String?,
    val isAutoCompleted: Boolean,
    // ★ 修正：こちらも同様に null を初期値（通知なし）にする
    val remindMinutes: Int? = null
)