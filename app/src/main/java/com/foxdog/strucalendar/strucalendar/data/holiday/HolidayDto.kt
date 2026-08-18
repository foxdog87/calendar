package com.foxdog.strucalendar.data.holiday

import kotlinx.serialization.Serializable

@Serializable
data class HolidayDto(
    val date: String,       // "2026-01-01" 形式
    val localName: String,  // 現地語の祝日名
    val name: String,       // 英語の祝日名
    val countryCode: String
)