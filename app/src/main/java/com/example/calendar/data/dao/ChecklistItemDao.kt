package com.example.calendar.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.calendar.data.entity.ChecklistItem

@Dao
interface ChecklistItemDao {

    @Query("""
        SELECT * FROM checklist_items
        WHERE taskId = :taskId
        ORDER BY position ASC
    """)
    suspend fun getChecklistItemsByTaskId(
        taskId: Long
    ): List<ChecklistItem>

    @Insert
    suspend fun insertAll(
        items: List<ChecklistItem>
    )

    @Update
    suspend fun updateChecklistItem(
        item: ChecklistItem
    )

    @Delete
    suspend fun deleteChecklistItem(
        item: ChecklistItem
    )

    @Query("""
        DELETE FROM checklist_items
        WHERE taskId = :taskId
    """)
    suspend fun deleteForTask(
        taskId: Long
    )
}