package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.foxdog.strucalendar.data.entity.TagCustomField

@Dao
interface TagCustomFieldDao {

    @Query("""
        SELECT *
        FROM tag_custom_fields
        WHERE tagId IN (:tagIds)
        ORDER BY fieldId
    """)
    suspend fun getByTagIds(
        tagIds: List<Long>
    ): List<TagCustomField>

    @Query("""
        SELECT *
        FROM tag_custom_fields
        WHERE tagId = :tagId
        ORDER BY fieldId
    """)
    suspend fun getByTagId(
        tagId: Long
    ): List<TagCustomField>

    @Insert
    suspend fun insertAll(
        fields: List<TagCustomField>
    )

    @Delete
    suspend fun delete(
        field: TagCustomField
    )

    @Query("""
        DELETE FROM tag_custom_fields
        WHERE tagId = :tagId
    """)
    suspend fun deleteByTagId(
        tagId: Long
    )
}