package com.example.calendar.data.entity

data class Tag(
    val tagId: Int = 0,         // PK
    val name: String,           // タグ名（例：「プロジェクトA」）
    val icon: String? = null,
    val color: Int,
    val customFieldLabel: String? = null // ★ statusから変更：追加したい入力欄の名前が入る
)