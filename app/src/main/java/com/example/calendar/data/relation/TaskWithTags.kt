package com.example.calendar.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.TaskTag

data class TaskWithTags(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "taskId",
        entityColumn = "tagId",
        associateBy = Junction(TaskTag::class)
    )
    val tags: List<Tag>
)