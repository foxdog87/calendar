package com.foxdog.strucalendar.viewmodel

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.dao.ChecklistItemDao
import com.foxdog.strucalendar.data.dao.TagCustomFieldDao
import com.foxdog.strucalendar.data.dao.TaskCustomFieldValueDao
import com.foxdog.strucalendar.data.dao.TaskDao
import com.foxdog.strucalendar.data.dao.TaskTagDao
import com.foxdog.strucalendar.data.dao.TemplateDao
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.TagCustomField
import com.foxdog.strucalendar.data.entity.Template
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.data.settings.AppSettings
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.notification.TaskAlarmScheduler
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class TaskDetailViewModel(
    private val taskDao: TaskDao,
    private val checklistItemDao: ChecklistItemDao,
    private val taskTagDao: TaskTagDao,
    private val taskCustomFieldValueDao: TaskCustomFieldValueDao,
    private val tagCustomFieldDao: TagCustomFieldDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ★ 追加：削除確認ダイアログの表示可否を画面側で参照するため
    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    var currentTaskWithTags by mutableStateOf<TaskWithTags?>(null)
        private set

    var currentCustomFields by mutableStateOf<List<TagCustomField>>(emptyList())
        private set

    var currentCustomFieldValues by mutableStateOf<Map<Long, String>>(emptyMap())
        private set

    var currentTemplate by mutableStateOf<Template?>(null)
        private set


    val checklistState = mutableStateListOf<ChecklistItem>()


    fun loadTaskDetail(taskId: Long) {

        AnalyticsLogger.logTaskDetailOpened()

        viewModelScope.launch {

            val taskWithTags =
                taskDao.getTaskWithTagsById(taskId)

            currentTaskWithTags = taskWithTags

            checklistState.clear()

            currentCustomFields = emptyList()
            currentCustomFieldValues = emptyMap()

            if (taskWithTags != null) {

                val items =
                    checklistItemDao.getChecklistItemsByTaskId(taskId)

                checklistState.addAll(
                    items.sortedBy { it.position }
                )

                // タスクに付いているタグからCustomField定義を取得
                val tagIds =
                    taskWithTags.tags.map { it.tagId }

                if (tagIds.isNotEmpty()) {

                    currentCustomFields =
                        tagCustomFieldDao.getByTagIds(tagIds)
                }

                // このTaskに保存されているCustomField値を取得
                val values =
                    taskCustomFieldValueDao.getByTaskId(taskId)

                currentCustomFieldValues =
                    values.associate { it.fieldId to it.value }
            }
        }
    }


    fun toggleTaskCompletion(
        context: Context
    ) {

        val current =
            currentTaskWithTags ?: return


        val currentTask =
            current.task


        val newStatus =
            if (currentTask.completeState == "COMPLETED") {
                "UNCOMPLETED" // ★ 修正：INCOMPLETE → UNCOMPLETED（他画面と統一）
            } else {
                "COMPLETED"
            }


        val updatedTask =
            currentTask.copy(
                completeState = newStatus
            )


        viewModelScope.launch {

            taskDao.updateTask(updatedTask)

            AnalyticsLogger.logTaskCompletionToggled()


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


        checklistState[index] = updatedItem


        viewModelScope.launch {

            checklistItemDao.updateChecklistItem(
                updatedItem
            )

            AnalyticsLogger.logChecklistItemToggled()
        }
    }


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

            AnalyticsLogger.logTaskDeleted()


            withContext(Dispatchers.Main) {

                onSuccess()
            }
        }
    }

    fun deleteRecurrenceGroup(
        context: Context,
        onSuccess: () -> Unit
    ) {

        val current = currentTaskWithTags ?: return
        val groupId = current.task.recurrenceGroupId ?: return

        viewModelScope.launch {

            val groupTasks = taskDao.getTasksByRecurrenceGroupId(groupId)
            if (groupTasks.isEmpty()) {
                withContext(Dispatchers.Main) { onSuccess() }
                return@launch
            }

            val scheduler = TaskAlarmScheduler(context)
            groupTasks.forEach { scheduler.cancel(it) }

            val taskIds = groupTasks.map { it.taskId }

            taskTagDao.deleteForTaskIds(taskIds)
            checklistItemDao.deleteForTaskIds(taskIds)
            taskDao.deleteTasksByIds(taskIds)

            AnalyticsLogger.logTaskDeleted()

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }
}