package com.example.calendar.state

import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.TagCustomField
import java.time.LocalDateTime

data class TaskInputState(
    val title: String = "",
    // ★修正：日付と時刻を完全に統合した LocalDateTime に変更（初期値は現在日時）
    val startTime: LocalDateTime = LocalDateTime.now(),
    val endTime: LocalDateTime = LocalDateTime.now().plusHours(1), // 初期値は1時間後に設定
    val color: Int? = null,
    val memo: String = "",
    val location: String = "",
    val url: String = "",
    val checkList: String = "",
    val attachmentPath: String? = null,
    val dayCountTarget: Boolean = false,
    val selectedTags: List<Tag> = emptyList(),
    val customFieldValues: Map<TagCustomField, String> = emptyMap()
)