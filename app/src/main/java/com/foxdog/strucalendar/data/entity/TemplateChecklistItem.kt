package com.foxdog.strucalendar.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("templateId")
    ]
)
data class TemplateChecklistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val templateId: Long,

    val text: String,

    val isChecked: Boolean = false, // チェック状態を保存できるようにする

    val position: Int
)