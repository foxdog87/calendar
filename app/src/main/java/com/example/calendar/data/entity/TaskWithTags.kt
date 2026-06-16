package com.example.calendar.data.entity

// ★ここを完全にこの通りに修正してください
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TaskWithTags(
    @Embedded val task: Task,
    @Relation(
        parentColumn = "taskId",
        entityColumn = "tagId",
        associateBy = Junction(TaskTag::class)
    )
    val tags: List<Tag>
)