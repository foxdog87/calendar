package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.foxdog.strucalendar.data.entity.TemplateChecklistItem

@Dao
interface TemplateChecklistItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TemplateChecklistItem>)

    @Query("DELETE FROM template_checklist_items WHERE templateId = :templateId")
    suspend fun deleteForTemplate(templateId: Long)

    @Query("""
        SELECT *
        FROM template_checklist_items
        WHERE templateId = :templateId
        ORDER BY position ASC
    """)
    suspend fun getByTemplateId(
        templateId: Long
    ): List<TemplateChecklistItem>
}
