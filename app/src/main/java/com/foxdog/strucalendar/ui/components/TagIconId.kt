package com.foxdog.strucalendar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TagIconId(
    val id: String,
    val vector: ImageVector
) {
    // 既存のアイコン
    BOOK("book", Icons.Default.MenuBook),
    WARNING("warning", Icons.Default.Warning),
    FIRE("fire", Icons.Default.LocalFireDepartment),
    SCHOOL("school", Icons.Default.School),
    WORK("work", Icons.Default.Work),
    STAR("star", Icons.Default.Star),
    FAVORITE("favorite", Icons.Default.Favorite),

    // ★ レポートの要件に合わせて追加した5つのアイコン
    ASSIGNMENT("assignment", Icons.Default.Assignment), // 課題・書類提出用
    EVENT("event", Icons.Default.Event),               // ミーティング・試験用
    PERSON("person", Icons.Default.Person),             // 私用・個人用
    FLAG("flag", Icons.Default.Flag),                   // 重要・マイルストーン用
    NOTIFICATIONS("notifications", Icons.Default.Notifications); // 期限厳守・リマインダー用

    companion object {
        fun fromId(id: String?): TagIconId? {
            if (id.isNullOrBlank()) return null
            return entries.firstOrNull { it.id == id }
        }
    }
}