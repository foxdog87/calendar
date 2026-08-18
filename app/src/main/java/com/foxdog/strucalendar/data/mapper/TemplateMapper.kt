package com.foxdog.strucalendar.data.mapper

import com.foxdog.strucalendar.data.entity.ChecklistItem

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