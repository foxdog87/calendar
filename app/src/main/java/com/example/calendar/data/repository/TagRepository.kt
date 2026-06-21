package com.example.calendar.data.repository

import androidx.compose.ui.graphics.Color
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first

class TagRepository(private val taskDao: TaskDao) {

    // すべてのタグを取得する。もしデータが空なら初期タグを注入する
    val allTags: Flow<List<Tag>> = taskDao.getAllTags()
        .onStart {
            // 初回データ取得時に、テーブルが空っぽなら初期データを投入する
            val currentTags = taskDao.getAllTags().first()
            if (currentTags.isEmpty()) {
                val defaultTags = listOf(
                    Tag(name = "提出物", color = Color(0xFFFFD54F).value.toInt(), icon = "Book"),
                    Tag(name = "重要", color = Color(0xFFFF8A80).value.toInt(), icon = "ErrorOutline"),
                    Tag(name = "数学", color = Color(0xFF80D8FF).value.toInt(), icon = ""),
                    Tag(name = "レポート", color = Color(0xFFD1C4E9).value.toInt(), icon = "")
                )
                defaultTags.forEach { taskDao.insertTag(it) }
            }
        }

    // 新規タグをデータベースに保存する
    suspend fun insertTag(tag: Tag): Long {
        return taskDao.insertTag(tag)
    }

    // タグを削除する（中間テーブルのデータも安全に一緒に消す）
    suspend fun deleteTag(tag: Tag) {
        taskDao.deleteIntermediateTaskTag(tag.tagId)
        taskDao.deleteTag(tag)
    }
}