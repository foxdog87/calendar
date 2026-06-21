package com.example.calendar.data.entity


import androidx.room.Entity

@Entity(tableName = "template_tag", primaryKeys = ["templateId", "tagId"])
data class TemplateTag(
    val templateId: Long,
    val tagId: Long
)