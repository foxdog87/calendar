package com.example.calendar.state

import java.time.LocalDate
import java.time.LocalTime
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.TagCustomField

data class TaskInputState(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val color: Int? = null,
    val memo: String = "",
    val location: String = "",
    val url: String = "",
    val checkList: String = "",
    val dayCountTarget: LocalDate? = null,
    val attachmentPath: String? = null,

    // 選択されたタグのリスト
    val selectedTags: List<Tag> = emptyList(),

    // ★修正：[ カスタム項目エンティティ (定義) -> ユーザーの入力値 ] のペアで管理する
    val customFieldValues: Map<TagCustomField, String> = emptyMap()
)