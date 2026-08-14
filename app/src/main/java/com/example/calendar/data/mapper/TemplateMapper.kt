package com.example.calendar.data.mapper

import com.example.calendar.data.entity.ChecklistItem

fun String.toChecklistItemsFromText(): List<ChecklistItem> {
    return this
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { index, text ->
            ChecklistItem(
                id = 0L,
                taskId = 0L,
                text = text.removePrefix("・").trim(),
                isChecked = false,
                position = index
            )
        }
}