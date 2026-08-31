package com.foxdog.strucalendar.data.repository

import com.foxdog.strucalendar.data.dao.ChecklistItemDao
import com.foxdog.strucalendar.data.dao.TaskDao
import com.foxdog.strucalendar.data.dao.TaskTagDao
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.entity.TaskTag
import com.foxdog.strucalendar.data.relation.TaskWithTags
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val taskTagDao: TaskTagDao,
    private val checklistItemDao: ChecklistItemDao
) {

    val allTasksWithTags: Flow<List<TaskWithTags>> =
        taskDao.getAllTasksWithTags()


    suspend fun getTaskWithTagsById(
        taskId: Long
    ): TaskWithTags? {
        return taskDao.getTaskWithTagsById(taskId)
    }


    suspend fun insertTaskWithTagsAndChecklist(
        task: Task,
        tags: List<Tag>,
        checklistItems: List<ChecklistItem>
    ): Long {


        val taskId =
            taskDao.insertTask(task)


        val items =
            checklistItems.mapIndexed { index, item ->

                item.copy(
                    id = 0L,
                    taskId = taskId,
                    position = index
                )
            }


        if (items.isNotEmpty()) {
            checklistItemDao.insertAll(items)
        }


        tags.forEach { tag ->

            taskTagDao.insert(
                TaskTag(
                    taskId = taskId,
                    tagId = tag.tagId
                )
            )
        }


        return taskId
    }



    suspend fun updateTaskWithTagsAndChecklist(
        task: Task,
        tags: List<Tag>,
        checklistItems: List<ChecklistItem>
    ) {


        taskDao.updateTask(task)


        checklistItemDao.deleteForTask(
            task.taskId
        )


        val items =
            checklistItems.mapIndexed { index, item ->

                item.copy(
                    id = 0L,
                    taskId = task.taskId,
                    position = index
                )
            }


        if (items.isNotEmpty()) {
            checklistItemDao.insertAll(items)
        }


        taskTagDao.deleteForTask(
            task.taskId
        )


        tags.forEach { tag ->

            taskTagDao.insert(
                TaskTag(
                    taskId = task.taskId,
                    tagId = tag.tagId
                )
            )
        }
    }



    suspend fun updateTask(
        task: Task
    ) {
        taskDao.updateTask(task)
    }



    suspend fun deleteTask(
        task: Task
    ) {
        taskDao.deleteTask(task)
    }



    suspend fun getChecklistItems(
        taskId: Long
    ): List<ChecklistItem> {

        return checklistItemDao
            .getChecklistItemsByTaskId(taskId)
    }

    suspend fun countOldTasks(beforeEpoch: Long?, targetStates: List<String>): Int {
        return taskDao.getTaskIdsBefore(beforeEpoch, targetStates).size
    }

    suspend fun autoCompleteExpiredTasks() {
        taskDao.autoCompleteExpiredTasks(System.currentTimeMillis() / 1000)
    }

    suspend fun deleteOldTasks(beforeEpoch: Long?, targetStates: List<String>): Int {
        val taskIds = taskDao.getTaskIdsBefore(beforeEpoch, targetStates)
        if (taskIds.isEmpty()) return 0

        taskTagDao.deleteForTaskIds(taskIds)
        checklistItemDao.deleteForTaskIds(taskIds)
        taskDao.deleteTasksByIds(taskIds)

        return taskIds.size
    }

    suspend fun setPinned(taskId: Long, isPinned: Boolean) {
        taskDao.setPinned(taskId, isPinned)
    }

    // 繰り返しシリーズ一覧の取得
    suspend fun getRecurrenceSeriesSummaries() = taskDao.getRecurrenceSeriesSummaries()

    // 選択した繰り返しシリーズをまとめて削除（タグ・チェックリストも連動削除）
    suspend fun deleteRecurrenceSeries(groupIds: List<String>): Int {
        if (groupIds.isEmpty()) return 0

        val taskIds = taskDao.getTaskIdsByRecurrenceGroupIds(groupIds)
        if (taskIds.isEmpty()) return 0

        taskTagDao.deleteForTaskIds(taskIds)
        checklistItemDao.deleteForTaskIds(taskIds)
        taskDao.deleteTasksByIds(taskIds)

        return taskIds.size
    }

}