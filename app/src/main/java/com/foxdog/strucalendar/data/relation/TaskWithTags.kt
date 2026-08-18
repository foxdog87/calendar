package com.foxdog.strucalendar.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.entity.TaskTag

data class TaskWithTags(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "taskId",
        entityColumn = "tagId",
        associateBy = Junction(TaskTag::class)
    )
    val tags: List<Tag>
)