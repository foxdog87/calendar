package com.example.calendar.data.entity

data class Tag(
    val tagId: Int = 0,         // PK
    val name: String,          
    val icon: String? = null,
    val color: Int,
)