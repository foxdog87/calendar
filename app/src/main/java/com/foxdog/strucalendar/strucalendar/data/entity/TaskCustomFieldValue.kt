package com.foxdog.strucalendar.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_custom_field_values",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagCustomField::class,
            parentColumns = ["fieldId"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["fieldId"]),
        Index(value = ["taskId", "fieldId"], unique = true)
    ]
)
data class TaskCustomFieldValue(
    @PrimaryKey(autoGenerate = true)
    val valueId: Long = 0L,

    val taskId: Long,

    val fieldId: Long,

    val value: String
)