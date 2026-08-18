package com.foxdog.strucalendar.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.entity.RecurrenceSeriesSummary
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.data.repository.TagRepository
import com.foxdog.strucalendar.data.repository.TaskRepository
import com.foxdog.strucalendar.data.settings.AppSettings
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allTasksWithTags: StateFlow<List<TaskWithTags>> = taskRepository.allTasksWithTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTags: StateFlow<List<Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun toggleTaskCompletion(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            val currentTask = taskWithTags.task
            val nextState = if (currentTask.completeState == "COMPLETED") "UNCOMPLETED" else "COMPLETED"
            taskRepository.updateTask(currentTask.copy(completeState = nextState))
            AnalyticsLogger.logTaskCompletionToggled()
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            AnalyticsLogger.logTagDeleted()
        }
    }

    fun createTag(
        tag: Tag,
        customFieldNames: List<String>
    ) {
        viewModelScope.launch {
            tagRepository.createTag(
                tag = tag,
                customFieldNames = customFieldNames
            )

            AnalyticsLogger.logTagCreated()
        }
    }

    fun updateTagOrder(tags: List<Tag>) {
        viewModelScope.launch {
            tagRepository.updateTagOrder(tags)
            AnalyticsLogger.logTagOrderChanged()
        }
    }

    // ============================================================
    // 過去タスク一括削除
    // ============================================================

    var bulkDeletePreviewCount by mutableStateOf<Int?>(null)
        private set

    var isBulkDeleting by mutableStateOf(false)
        private set

    // ★ 変更：deleteAllStatuses: Boolean（Switch）から、
    // includeCompleted / includeUncompleted の2つの独立したBoolean（Checkbox）に変更
    fun previewBulkDelete(cutoffEpoch: Long?, includeCompleted: Boolean, includeUncompleted: Boolean) {
        val states = buildTargetStates(includeCompleted, includeUncompleted)

        if (states.isEmpty()) {
            // 削除対象が何も選択されていない場合はDBに問い合わせず0件扱いにする
            bulkDeletePreviewCount = 0
            return
        }

        viewModelScope.launch {
            bulkDeletePreviewCount = taskRepository.countOldTasks(cutoffEpoch, states)
        }
    }

    fun clearBulkDeletePreview() {
        bulkDeletePreviewCount = null
    }

    fun executeBulkDelete(
        cutoffEpoch: Long?,
        includeCompleted: Boolean,
        includeUncompleted: Boolean,
        onComplete: () -> Unit
    ) {
        val states = buildTargetStates(includeCompleted, includeUncompleted)

        if (states.isEmpty()) {
            onComplete()
            return
        }

        viewModelScope.launch {
            isBulkDeleting = true
            taskRepository.deleteOldTasks(cutoffEpoch, states)
            isBulkDeleting = false
            bulkDeletePreviewCount = null
            onComplete()
        }
    }

    // ★ 変更：未完了・完了それぞれの独立したチェック状態から対象ステータスのリストを組み立てる
    private fun buildTargetStates(includeCompleted: Boolean, includeUncompleted: Boolean): List<String> {
        val states = mutableListOf<String>()
        if (includeCompleted) states.add("COMPLETED")
        if (includeUncompleted) states.add("UNCOMPLETED")
        return states
    }

    // ============================================================
    // 繰り返しタスクの一括削除
    // ============================================================

    var recurrenceSeries by mutableStateOf<List<RecurrenceSeriesSummary>>(emptyList())
        private set

    fun loadRecurrenceSeries() {
        viewModelScope.launch {
            recurrenceSeries = taskRepository.getRecurrenceSeriesSummaries()
        }
    }

    fun deleteRecurrenceSeries(groupIds: List<String>, onComplete: () -> Unit) {
        viewModelScope.launch {
            taskRepository.deleteRecurrenceSeries(groupIds)
            recurrenceSeries = emptyList()
            onComplete()
        }
    }
    // ============================================================
    // オンボーディング
    // ============================================================

    // ★ 追加
    fun setTaskListOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setTaskListOnboardingCompleted(completed)
        }
    }
}