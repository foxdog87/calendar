package com.example.calendar.data.entity

data class Template(
    val templateId: Int,
    val title: String,
    val icon: String?,
    val timeLength: Int, // 分単位
    val color: Int,
    val description: String,
    val additionalOption: String? = null
)