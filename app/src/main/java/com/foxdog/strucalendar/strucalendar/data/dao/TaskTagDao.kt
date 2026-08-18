package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.foxdog.strucalendar.data.entity.TaskTag

@Dao
interface TaskTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(taskTag: TaskTag)


    @Query("""
        DELETE FROM task_tag
        WHERE taskId = :taskId
    """)
    suspend fun deleteForTask(taskId: Long)


    @Query("""
        DELETE FROM task_tag
        WHERE tagId = :tagId
    """)
    suspend fun deleteForTag(tagId: Long)

    @Query("""
    DELETE FROM task_tag
    WHERE taskId IN (:taskIds)
""")
    suspend fun deleteForTaskIds(taskIds: List<Long>)
}