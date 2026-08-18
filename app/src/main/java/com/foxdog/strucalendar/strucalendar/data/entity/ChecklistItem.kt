package com.foxdog.strucalendar.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId")
    ]
)
data class ChecklistItem(
    @androidx.room.PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val taskId: Long,

    val text: String,

    val isChecked: Boolean = false,

    val position: Int
)