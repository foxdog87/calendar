package com.example.calendar.data.entity

/**
 * ER図の TaskTag 中間テーブルに対応するデータクラス
 * Task と Tag の多対多を実現するための「結びつき」を保持する
 */
data class TaskTag(
    val taskId: Long, // PK, FK (TaskテーブルのtaskIdを指す)
    val tagId: Long   // PK, FK (TagテーブルのtagIdを指す)
)