package com.example.calendar.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.entity.Task

data class TaskWithChecklist(
    @Embedded
    val task: Task,

    @Relation(
        parentColumn = "taskId",
        entityColumn = "taskId"
    )
    val checklistItems: List<ChecklistItem>
)