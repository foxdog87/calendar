package com.foxdog.strucalendar.data.entity

import androidx.room.Entity

@Entity(tableName = "holidays", primaryKeys = ["date", "countryCode"])
data class HolidayEntity(
    val date: String,        // "2026-01-01" 形式で保存
    val countryCode: String, // "JP", "US" など
    val localName: String
)