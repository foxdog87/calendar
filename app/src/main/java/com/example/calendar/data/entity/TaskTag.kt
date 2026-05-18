package com.example.calendar.data.entity

data class TaskTag(
    val taskId: Int, // PK, FK (TaskテーブルのtaskIdを指す)
    val tagId: Int   // PK, FK (TagテーブルのtagIdを指す)
)