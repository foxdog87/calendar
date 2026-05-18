package com.example.calendar.data.entity

data class Tag(
    val tagId: Int = 0,      // PK (Primary Key)
    val name: String,        // タグの名前（例：「仕事」「プライベート」）
    val icon: String? = null, // ER図にあるアイコン（Stringでパスや名前を保持）
    val color: Int,          // ER図にあるタグの色
    val status: String? = null // ER図にあるステータス
)