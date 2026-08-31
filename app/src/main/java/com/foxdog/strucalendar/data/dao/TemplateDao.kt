package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.entity.Template
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Insert
    suspend fun insertTemplate(template: Template): Long

    @Update
    suspend fun updateTemplate(template: Template)

    @Update
    suspend fun updateAll(templates: List<Template>)

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("SELECT * FROM templates ORDER BY position ASC")
    fun getAllTemplates(): Flow<List<Template>>

    @Query("SELECT MAX(position) FROM templates")
    suspend fun getMaxPosition(): Int?

    @Query("SELECT * FROM templates WHERE templateId = :templateId")
    suspend fun getTemplateById(templateId: Long): Template?


    @Query("""
        SELECT *
        FROM templates
        WHERE lastUsedAt IS NOT NULL
        ORDER BY lastUsedAt DESC
        LIMIT :limit
    """)
    fun getRecentTemplates(limit: Int): Flow<List<Template>>

    @Query("UPDATE templates SET lastUsedAt = :lastUsedAt WHERE templateId = :templateId")
    suspend fun updateLastUsedAt(templateId: Long, lastUsedAt: Long)

    @Insert
    suspend fun insertAll(tasks: List<Task>)


    @Query("DELETE FROM tasks WHERE recurrenceGroupId = :groupId AND startTime >= :fromStartTime")
    suspend fun deleteRecurrenceFromDate(groupId: String, fromStartTime: Long)


    @Query("DELETE FROM tasks WHERE recurrenceGroupId = :groupId")
    suspend fun deleteRecurrenceGroup(groupId: String)
}