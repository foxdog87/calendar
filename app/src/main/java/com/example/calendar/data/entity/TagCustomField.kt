package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_custom_fields")
data class TagCustomField(
    @PrimaryKey(autoGenerate = true) val fieldId: Long = 0,
    val tagId: Long,                 // BIGINT (FK)
    val fieldName: String            // VARCHAR(100)
)