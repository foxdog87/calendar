package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.foxdog.strucalendar.data.entity.TemplateCustomFieldValue

@Dao
interface TemplateCustomFieldValueDao {

    @Query("""
        SELECT *
        FROM template_custom_field_values
        WHERE templateId = :templateId
    """)
    suspend fun getByTemplateId(
        templateId: Long
    ): List<TemplateCustomFieldValue>

    @Upsert
    suspend fun upsert(
        value: TemplateCustomFieldValue
    )

    @Upsert
    suspend fun upsertAll(
        values: List<TemplateCustomFieldValue>
    )

    @Query("""
        DELETE FROM template_custom_field_values
        WHERE templateId = :templateId
    """)
    suspend fun deleteByTemplateId(
        templateId: Long
    )
}