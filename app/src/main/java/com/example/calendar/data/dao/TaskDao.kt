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
import com.example.calendar.data.entity.TaskWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY startTime ASC")
    fun getAllTasksWithTags(): Flow<List<TaskWithTags>>

    // ★ 追加：データベースに保存されているすべてのタグをリアルタイム（Flow）で取得する
    @Query("SELECT * FROM tags ORDER BY tagId ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Transaction
    @Query("""
        SELECT * FROM tasks 
        WHERE taskId IN (SELECT taskId FROM task_tag WHERE tagId IN (:tagIds))
    """)
    fun getTasksByTagIds(tagIds: List<Long>): List<TaskWithTags>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTag(taskTag: TaskTag)

    // ★ 追加：タグそのものを削除する（長押し削除用）
    @Delete
    suspend fun deleteTag(tag: Tag)

    // ★ 追加：タグが削除された際、タスクとタグの中間テーブルからもデータを削除する
    @Query("DELETE FROM task_tag WHERE tagId = :tagId")
    suspend fun deleteIntermediateTaskTag(tagId: Long)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE startTime > :currentEpoch AND remindMinutes IS NOT NULL")
    suspend fun getFutureTasksWithReminderDirect(currentEpoch: Long): List<Task>
}