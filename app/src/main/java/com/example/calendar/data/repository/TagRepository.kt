package com.example.calendar.data.repository

import com.example.calendar.data.dao.TagDao
import com.example.calendar.data.dao.TagDisplayOrderDao
import com.example.calendar.data.dao.TaskTagDao
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.TagDisplayOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TagRepository(
    private val tagDao: TagDao,
    private val tagDisplayOrderDao: TagDisplayOrderDao,
    private val taskTagDao: TaskTagDao
) {


    /**
     * 表示順付きタグ一覧取得
     */
    fun getAllTags(): Flow<List<Tag>> {

        return combine(
            tagDao.getAllTags(),
            tagDisplayOrderDao.getAllOrders()
        ) { tags, orders ->


            val orderMap =
                orders.associateBy {
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
        tag: Tag
    ): Long {


        val tagId =
            tagDao.insertTag(tag)


        val maxPosition =
            tagDisplayOrderDao
                .getMaxPosition()
                ?: -1


        tagDisplayOrderDao.insert(
            TagDisplayOrder(
                tagId = tagId,
                position = maxPosition + 1
            )
        )


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
    suspend fun deleteTag(tag: Tag) {

        taskTagDao.deleteForTag(tag.tagId)

        tagDisplayOrderDao.deleteByTagId(tag.tagId)

        tagDao.deleteTag(tag)
    }



    /**
     * 並び替え保存
     */
    // TagRepository.kt
    suspend fun updateTagOrder(tags: List<Tag>) {
        val orders = tags.mapIndexed { index, tag ->
            TagDisplayOrder(tagId = tag.tagId, position = index)
        }
        tagDisplayOrderDao.insertAll(orders) // ← updateAll から insertAll に変更
    }


}