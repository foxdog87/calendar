package com.example.calendar.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.TaskTag
import com.example.calendar.data.relation.TaskWithTags
import com.example.calendar.data.relation.TaskWithTagsAndChecklist
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {


    @Transaction
    @Query(
        "SELECT * FROM tasks ORDER BY startTime ASC"
    )
    fun getAllTasksWithTags(): Flow<List<TaskWithTags>>


    @Transaction
    @Query("""
        SELECT *
        FROM tasks
        WHERE taskId IN (
            SELECT taskId 
            FROM task_tag
            WHERE tagId IN (:tagIds)
        )
    """)
    fun getTasksByTagIds(
        tagIds: List<Long>
    ): List<TaskWithTags>


    @Transaction
    @Query("""
        SELECT *
        FROM tasks
        WHERE taskId = :taskId
    """)
    suspend fun getTaskWithTagsById(
        taskId: Long
    ): TaskWithTags?


    @Transaction
    @Query("""
        SELECT *
        FROM tasks
        WHERE taskId = :taskId
    """)
    suspend fun getTaskWithTagsAndChecklistById(
        taskId: Long
    ): TaskWithTagsAndChecklist?


    @Query("""
        SELECT *
        FROM tasks
        WHERE taskId = :taskId
    """)
    suspend fun getTaskById(
        taskId: Long
    ): Task?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(
        task: Task
    ): Long


    @Update
    suspend fun updateTask(
        task: Task
    )


    @Delete
    suspend fun deleteTask(
        task: Task
    )


    @Query("""
        SELECT *
        FROM tasks
        WHERE startTime > :currentEpoch
        AND (
            reminderType IS NOT NULL
            OR reminderOffsetMinutes IS NOT NULL
        )
        AND completeState != 'COMPLETED'
    """)
    suspend fun getFutureTasksWithReminderDirect(
        currentEpoch: Long
    ): List<Task>
}