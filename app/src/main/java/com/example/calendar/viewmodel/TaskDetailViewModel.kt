package com.example.calendar.viewmodel

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.dao.ChecklistItemDao
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.data.relation.TaskWithTags
import com.example.calendar.notification.TaskAlarmScheduler
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class TaskDetailViewModel(
    private val taskDao: TaskDao,
    private val checklistItemDao: ChecklistItemDao
) : ViewModel() {


    var currentTaskWithTags by mutableStateOf<TaskWithTags?>(null)
        private set


    val checklistState = mutableStateListOf<ChecklistItem>()


    /**
     * タスク詳細取得
     */
    fun loadTaskDetail(taskId: Long) {

        viewModelScope.launch {

            val taskWithTags =
                taskDao.getTaskWithTagsById(taskId)

            currentTaskWithTags = taskWithTags


            checklistState.clear()


            if (taskWithTags != null) {

                val items =
                    checklistItemDao.getChecklistItemsByTaskId(taskId)


                checklistState.addAll(
                    items.sortedBy { it.position }
                )
            }
        }
    }


    /**
     * タスク完了状態変更
     */
    fun toggleTaskCompletion(
        context: Context
    ) {

        val current =
            currentTaskWithTags ?: return


        val currentTask =
            current.task


        val newStatus =
            if (currentTask.completeState == "COMPLETED") {
                "INCOMPLETE"
            } else {
                "COMPLETED"
            }


        val updatedTask =
            currentTask.copy(
                completeState = newStatus
            )


        viewModelScope.launch {

            taskDao.updateTask(updatedTask)


            if (newStatus == "COMPLETED") {

                val notificationManager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager


                notificationManager.cancel(
                    updatedTask.taskId.toInt()
                )
            }


            currentTaskWithTags =
                current.copy(
                    task = updatedTask
                )
        }
    }


    /**
     * チェックリスト変更
     */
    fun toggleChecklistItem(
        index: Int,
        checked: Boolean
    ) {

        if (index !in checklistState.indices) {
            return
        }


        val item =
            checklistState[index]


        val updatedItem =
            item.copy(
                isChecked = checked
            )


        // UI即時反映
        checklistState[index] = updatedItem


        // DB保存
        viewModelScope.launch {

            checklistItemDao.updateChecklistItem(
                updatedItem
            )
        }
    }


    /**
     * タスク削除
     *
     * checklist_items は ForeignKey CASCADE
     * のためTask削除時に自動削除される
     */
    fun deleteTask(
        context: Context,
        onSuccess: () -> Unit
    ) {

        val current =
            currentTaskWithTags ?: return


        viewModelScope.launch {


            TaskAlarmScheduler(context)
                .cancel(current.task)


            taskDao.deleteTask(
                current.task
            )


            withContext(Dispatchers.Main) {

                onSuccess()
            }
        }
    }
}