package com.foxdog.strucalendar.data.repository

import com.foxdog.strucalendar.data.dao.TagDao
import com.foxdog.strucalendar.data.dao.TagDisplayOrderDao
import com.foxdog.strucalendar.data.dao.TagCustomFieldDao
import com.foxdog.strucalendar.data.dao.TaskTagDao
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.TagCustomField
import com.foxdog.strucalendar.data.entity.TagDisplayOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TagRepository(
    private val tagDao: TagDao,
    private val tagDisplayOrderDao: TagDisplayOrderDao,
    private val taskTagDao: TaskTagDao,
    private val tagCustomFieldDao: TagCustomFieldDao
) {

    /**
     * 表示順付きタグ一覧取得
     */
    fun getAllTags(): Flow<List<Tag>> {

        return combine(
            tagDao.getAllTags(),
            tagDisplayOrderDao.getAllOrders()
        ) { tags, orders ->

            val orderMap = orders.associateBy {
                it.tagId
            }

            tags.sortedBy { tag ->
                orderMap[tag.tagId]?.position
                    ?: Int.MAX_VALUE
            }
        }
    }

    /**
     * タグ作成
     */
    suspend fun createTag(
        tag: Tag,
        customFieldNames: List<String>
    ): Long {

        // 1. タグ本体を作成
        val tagId = tagDao.insertTag(tag)

        // 2. 表示順を作成
        val maxPosition =
            tagDisplayOrderDao.getMaxPosition() ?: -1

        tagDisplayOrderDao.insert(
            TagDisplayOrder(
                tagId = tagId,
                position = maxPosition + 1
            )
        )

        // 3. カスタム項目を作成
        val customFields = customFieldNames
            .filter { it.isNotBlank() }
            .map { fieldName ->
                TagCustomField(
                    tagId = tagId,
                    fieldName = fieldName
                )
            }

        if (customFields.isNotEmpty()) {
            tagCustomFieldDao.insertAll(customFields)
        }

        return tagId
    }

    suspend fun updateTag(
        tag: Tag
    ) {
        tagDao.updateTag(tag)
    }

    /**
     * タグ削除
     */
    suspend fun deleteTag(
        tag: Tag
    ) {

        taskTagDao.deleteForTag(tag.tagId)

        tagCustomFieldDao.deleteByTagId(tag.tagId)

        tagDisplayOrderDao.deleteByTagId(tag.tagId)

        tagDao.deleteTag(tag)
    }

    /**
     * 並び替え保存
     */
    suspend fun updateTagOrder(
        tags: List<Tag>
    ) {
        val orders = tags.mapIndexed { index, tag ->
            TagDisplayOrder(
                tagId = tag.tagId,
                position = index
            )
        }

        tagDisplayOrderDao.insertAll(orders)
    }
}