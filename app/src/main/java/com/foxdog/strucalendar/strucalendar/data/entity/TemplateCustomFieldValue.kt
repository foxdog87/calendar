package com.foxdog.strucalendar.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_custom_field_values",
    foreignKeys = [
        ForeignKey(
            entity = Template::class,
            parentColumns = ["templateId"],
            childColumns = ["templateId"],
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
        Index(value = ["templateId"]),
        Index(value = ["fieldId"]),
        Index(value = ["templateId", "fieldId"], unique = true)
    ]
)
data class TemplateCustomFieldValue(
    @PrimaryKey(autoGenerate = true)
    val valueId: Long = 0L,

    val templateId: Long,

    val fieldId: Long,

    val value: String
)