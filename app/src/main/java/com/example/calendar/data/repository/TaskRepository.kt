package com.example.calendar.data.repository

import com.example.calendar.data.dao.ChecklistItemDao
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.dao.TaskTagDao
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.TaskTag
import com.example.calendar.data.relation.TaskWithTags
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
}