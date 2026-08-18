package com.foxdog.strucalendar.data.entity

import androidx.room.Entity

@Entity(tableName = "task_tag", primaryKeys = ["taskId", "tagId"])
data class TaskTag(
    val taskId: Long,
    val tagId: Long
)