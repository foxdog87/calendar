package com.example.calendar.data.entity


data class TagCustomField(
    val fieldId: Int = 0,    // PK (Primary Key)
    val tagId: Long,          // FK (Foreign Key): TagテーブルのtagIdを指す
    val fieldName: String    // ユーザーが入力した項目名（例：「提出先」「点数」）
)