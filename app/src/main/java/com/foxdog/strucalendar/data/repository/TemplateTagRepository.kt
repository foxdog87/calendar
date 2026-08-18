package com.foxdog.strucalendar.data.repository

import com.foxdog.strucalendar.data.dao.TemplateTagDao

class TemplateTagRepository(
    private val templateTagDao: TemplateTagDao
) {
    // templateId に紐づくタグのIDリストを取得する (Suspend関数版)
    suspend fun getTagIds(templateId: Long): List<Long> {
        return templateTagDao.getTagIdsForTemplate(templateId)
    }

    // 必要に応じて、Flowでリアルタイム取得したい場合や、追加・削除のメソッドもここに整備できます
    // suspend fun insertTemplateTag(templateTag: TemplateTag) = templateTagDao.insert(templateTag)
    // suspend fun deleteTemplateTag(templateId: Long, tagId: Long) = templateTagDao.delete(templateId, tagId)
}