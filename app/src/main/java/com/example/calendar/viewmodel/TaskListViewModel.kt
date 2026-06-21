package com.example.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.TaskWithTags
import com.example.calendar.data.repository.TagRepository
import com.example.calendar.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskListViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // ★ 永続化：タスク一覧をRepositoryからリアルタイム取得（StateFlowに変換）
    val allTasksWithTags: StateFlow<List<TaskWithTags>> = taskRepository.allTasksWithTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ★ 永続化：タグ一覧もRepositoryからリアルタイム取得
    val allTags: StateFlow<List<Tag>> = tagRepository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // タスクの完了状態をトグルする
    fun toggleTaskCompletion(taskWithTags: TaskWithTags) {
        viewModelScope.launch {
            val currentTask = taskWithTags.task
            val nextState = if (currentTask.completeState == "COMPLETED") "INCOMPLETE" else "COMPLETED"
            taskRepository.updateTask(currentTask.copy(completeState = nextState))
        }
    }

    // ★ 新設：タグをデータベースから永続的に削除する
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
        }
    }

    // ★ 新設：新しいタグをデータベースに直接保存する（一覧画面のクイック作成用）
    fun createTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.insertTag(tag)
        }
    }
}