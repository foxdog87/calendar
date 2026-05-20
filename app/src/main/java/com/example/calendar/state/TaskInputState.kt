package com.example.calendar.state

import java.time.LocalDate
import java.time.LocalTime
import com.example.calendar.data.entity.Tag

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
    // 複数選択を視野に入れて、選択されたタグをリスト保持に変更
    val selectedTags: List<Tag> = emptyList(), 
    // ★修正：[ カスタム項目の名前 (String) -> ユーザーの入力値 (String) ] のペアで保持
    val customFieldValues: Map<String, String> = emptyMap() 
)