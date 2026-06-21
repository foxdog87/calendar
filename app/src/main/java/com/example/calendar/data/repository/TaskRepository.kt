package com.example.calendar.data.repository

import android.util.Log
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.TaskTag
import com.example.calendar.data.entity.TaskWithTags
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {

    // すべてのタスク（タグ付き）をリアルタイムで取得
    val allTasksWithTags: Flow<List<TaskWithTags>> = taskDao.getAllTasksWithTags()

    // ★ 修正: 戻り値の型に : Long を追加
    suspend fun insertTaskWithTags(
        task: Task,
        tags: List<Tag>
    ): Long {
        val taskId = taskDao.insertTask(task)

        tags.forEach { tag ->
            Log.d(
                "TagSave",
                "taskId=$taskId tagId=${tag.tagId} name=${tag.name}"
            )

            taskDao.insertTaskTag(
                TaskTag(
                    taskId = taskId,
                    tagId = tag.tagId
                )
            )
        }

        // ★ 追加: 新しく生成されたtaskIdをViewModelへ返却する
        return taskId
    }

    // タスクの更新（完了状態のトグルなど）
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    // タスクの削除
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
}