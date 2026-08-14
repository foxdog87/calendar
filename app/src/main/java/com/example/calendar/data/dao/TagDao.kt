package com.example.calendar.data.dao

import androidx.room.*
import com.example.calendar.data.entity.Tag
import kotlinx.coroutines.flow.Flow


@Dao
interface TagDao {


    @Query(
        """
        SELECT *
        FROM tags
        ORDER BY tagId ASC
        """
    )
    fun getAllTags(): Flow<List<Tag>>


    @Insert
    suspend fun insertTag(
        tag: Tag
    ): Long


    @Update
    suspend fun updateTag(
        tag: Tag
    )


    @Delete
    suspend fun deleteTag(
        tag: Tag
    )

}