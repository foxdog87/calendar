package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.foxdog.strucalendar.data.entity.RecurrenceSeriesSummary
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.data.relation.TaskWithTagsAndChecklist
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

    @Query("""
    SELECT taskId
    FROM tasks
    WHERE (:beforeEpoch IS NULL OR endTime < :beforeEpoch)
    AND completeState IN (:targetStates)
""")
    suspend fun getTaskIdsBefore(beforeEpoch: Long?, targetStates: List<String>): List<Long>

    @Query("""
    DELETE FROM tasks
    WHERE taskId IN (:taskIds)
""")
    suspend fun deleteTasksByIds(taskIds: List<Long>)

    @Query("""
    UPDATE tasks
    SET isPinned = :isPinned
    WHERE taskId = :taskId
""")
    suspend fun setPinned(taskId: Long, isPinned: Boolean)

    // 繰り返しシリーズの一覧をグループ化して取得
    @Query("""
        SELECT
            recurrenceGroupId AS recurrenceGroupId,
            MIN(title) AS title,
            MIN(recurrenceType) AS recurrenceType,
            MIN(recurrenceIntervalDays) AS recurrenceIntervalDays,
            MIN(recurrenceNth) AS recurrenceNth,
            MIN(recurrenceWeekday) AS recurrenceWeekday,
            MIN(recurrenceEndDate) AS recurrenceEndDate,
            COUNT(*) AS occurrenceCount,
            MIN(startTime) AS firstStartTime
        FROM tasks
        WHERE recurrenceGroupId IS NOT NULL
        GROUP BY recurrenceGroupId
        ORDER BY firstStartTime DESC
    """)
    suspend fun getRecurrenceSeriesSummaries(): List<RecurrenceSeriesSummary>

    // 指定した複数の recurrenceGroupId に属するタスクIDを全部取得
    @Query("""
        SELECT taskId
        FROM tasks
        WHERE recurrenceGroupId IN (:groupIds)
    """)
    suspend fun getTaskIdsByRecurrenceGroupIds(groupIds: List<String>): List<Long>

    // ★ 追加：指定した単一の recurrenceGroupId に属する全タスク（フルエンティティ）を取得。
    // TaskDetailScreenで「この繰り返し予定をすべて削除する」を選んだ際、
    // アラーム解除のためにタスクの全カラム（reminder関連など）が必要になるため。
    @Query("""
        SELECT *
        FROM tasks
        WHERE recurrenceGroupId = :groupId
    """)
    suspend fun getTasksByRecurrenceGroupId(groupId: String): List<Task>
}