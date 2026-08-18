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

    // ▼ 追加

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

    // ★ 追加：繰り返しシリーズをまとめて挿入
    @Insert
    suspend fun insertAll(tasks: List<Task>)

    // ★ 追加（将来の「この回以降削除」用に先に用意）：
    // 指定した groupId のうち、fromStartTime 以降（その回を含む）を削除する
    @Query("DELETE FROM tasks WHERE recurrenceGroupId = :groupId AND startTime >= :fromStartTime")
    suspend fun deleteRecurrenceFromDate(groupId: String, fromStartTime: Long)

    // ★ 追加（将来の「シリーズ全体削除」用に先に用意）
    @Query("DELETE FROM tasks WHERE recurrenceGroupId = :groupId")
    suspend fun deleteRecurrenceGroup(groupId: String)
}