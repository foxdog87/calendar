package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.foxdog.strucalendar.data.entity.TaskCustomFieldValue

@Dao
interface TaskCustomFieldValueDao {

    @Query("""
        SELECT *
        FROM task_custom_field_values
        WHERE taskId = :taskId
    """)
    suspend fun getByTaskId(taskId: Long): List<TaskCustomFieldValue>

    @Upsert
    suspend fun upsert(value: TaskCustomFieldValue)

    @Upsert
    suspend fun upsertAll(values: List<TaskCustomFieldValue>)

    @Query("""
        DELETE FROM task_custom_field_values
        WHERE taskId = :taskId
    """)
    suspend fun deleteByTaskId(taskId: Long)
}