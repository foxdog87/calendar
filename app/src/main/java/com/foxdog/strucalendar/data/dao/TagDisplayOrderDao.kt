package com.foxdog.strucalendar.data.dao

import androidx.room.*
import com.foxdog.strucalendar.data.entity.TagDisplayOrder
import kotlinx.coroutines.flow.Flow


@Dao
interface TagDisplayOrderDao {


    @Query("""
        SELECT *
        FROM tag_display_orders
        ORDER BY position ASC
    """)
    fun getAllOrders(): Flow<List<TagDisplayOrder>>


    @Query("""
        SELECT *
        FROM tag_display_orders
        WHERE tagId = :tagId
    """)
    suspend fun getByTagId(
        tagId: Long
    ): TagDisplayOrder?


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insert(
        order: TagDisplayOrder
    )


    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertAll(
        orders: List<TagDisplayOrder>
    )


    @Update
    suspend fun updateAll(
        orders: List<TagDisplayOrder>
    )


    @Query("""
        DELETE FROM tag_display_orders
        WHERE tagId = :tagId
    """)
    suspend fun deleteByTagId(
        tagId: Long
    )


    @Query("""
        SELECT MAX(position)
        FROM tag_display_orders
    """)
    suspend fun getMaxPosition(): Int?
}