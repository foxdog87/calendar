package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.foxdog.strucalendar.data.entity.TemplateDisplayOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDisplayOrderDao {
    @Query("SELECT * FROM template_display_orders ORDER BY position")
    fun getAll(): Flow<List<TemplateDisplayOrder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TemplateDisplayOrder>)

    @Query("DELETE FROM template_display_orders WHERE templateId = :templateId")
    suspend fun deleteByTemplateId(templateId: Long)

    @Query("SELECT MAX(position) FROM template_display_orders")
    suspend fun getMaxPosition(): Int?
}
