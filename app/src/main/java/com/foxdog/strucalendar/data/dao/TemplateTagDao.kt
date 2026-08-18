package com.foxdog.strucalendar.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.foxdog.strucalendar.data.entity.TemplateTag

@Dao
interface TemplateTagDao {

    @Insert
    suspend fun insertAll(templateTags: List<TemplateTag>)

    @Query("DELETE FROM template_tag WHERE templateId = :templateId")
    suspend fun deleteForTemplate(templateId: Long)

    @Query("SELECT tagId FROM template_tag WHERE templateId = :templateId")
    suspend fun getTagIdsForTemplate(templateId: Long): List<Long>
}