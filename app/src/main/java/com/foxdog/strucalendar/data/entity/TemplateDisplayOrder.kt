package com.foxdog.strucalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "template_display_orders")
data class TemplateDisplayOrder(
    @PrimaryKey val templateId: Long,
    val position: Int
)
