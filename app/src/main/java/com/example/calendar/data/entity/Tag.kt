package com.example.calendar.data.entity

/**
 * ER図の Tag テーブルに対応するデータクラス
 */
data class Tag(
    val tagId: Int = 0,         // PK
    val name: String,           // タグ名（例：「プロジェクトA」）
    val icon: String? = null,
    val color: Int,
)