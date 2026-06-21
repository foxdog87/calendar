package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val tagId: Long = 0L,
    val name: String,
    val color: Int,
    val icon: String?
)