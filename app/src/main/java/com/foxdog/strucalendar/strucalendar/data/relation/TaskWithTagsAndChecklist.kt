package com.foxdog.strucalendar.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.entity.TaskTag

data class TaskWithTagsAndChecklist(
    @Embedded
    val task: Task,

    @Relation(
        parentColumn = "taskId",
        entityColumn = "tagId",
        associateBy = Junction(TaskTag::class)
    )
    val tags: List<Tag>,

    @Relation(
        parentColumn = "taskId",
        entityColumn = "taskId"
    )
    val checklistItems: List<ChecklistItem>
)