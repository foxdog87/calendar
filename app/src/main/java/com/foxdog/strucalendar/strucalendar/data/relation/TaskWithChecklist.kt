package com.foxdog.strucalendar.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Task

data class TaskWithChecklist(
    @Embedded
    val task: Task,

    @Relation(
        parentColumn = "taskId",
        entityColumn = "taskId"
    )
    val checklistItems: List<ChecklistItem>
)