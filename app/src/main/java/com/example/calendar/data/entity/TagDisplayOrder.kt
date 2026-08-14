package com.example.calendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag_display_orders"
)
data class TagDisplayOrder(

    @PrimaryKey
    val tagId: Long,

    val position: Int
)