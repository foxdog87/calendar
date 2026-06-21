package com.example.calendar.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.entity.TaskWithTags
import com.example.calendar.screens.ChecklistItem
import kotlinx.coroutines.flow.first // ★ Flowの初回値を取得するために追加
import kotlinx.coroutines.launch

class TaskDetailViewModel(private val taskDao: TaskDao) : ViewModel() {

    // 現在この画面で表示・編集しているタスクデータ
    var currentTaskWithTags by mutableStateOf<TaskWithTags?>(null)
        private set

    // チェックリストの状態
    val checklistState = mutableStateListOf<ChecklistItem>()

    /**
     * ★ 既存の getAllTasksWithTags() から本物のデータを安全にロードする
     */
    fun loadTaskDetail(taskId: Long) {
        viewModelScope.launch {
            // .first() を使うことで、Flowのストリームから現在の最新リストを1回だけ取り出します
            val allTasks = taskDao.getAllTasksWithTags().first()
            val found = allTasks.find { it.task.taskId == taskId }
            currentTaskWithTags = found

            checklistState.clear()
            if (found != null) {
                // UIが正常に動くようにモック構造と同期
                checklistState.addAll(
                    listOf(
                        ChecklistItem(1, "第1章の復習", false),
                        ChecklistItem(2, "第2章の問題演習", false),
                        ChecklistItem(3, "第3章の演習", found.task.completeState == "COMPLETED")
                    )
                )
            }
        }
    }

    /**
     * データベースと同期する、完了/未完了トグル処理
     */
    fun toggleTaskCompletion() {
        val currentItem = currentTaskWithTags ?: return
        val task = currentItem.task

        viewModelScope.launch {
            val newStatus = if (task.completeState == "COMPLETED") "UNCOMPLETED" else "COMPLETED"
            val updatedTask = task.copy(completeState = newStatus)

            // 1. データベースを更新
            taskDao.updateTask(updatedTask)

            // 2. 画面の内部Stateも最新状態に更新して再描画
            currentTaskWithTags = currentItem.copy(task = updatedTask)
        }
    }

    /**
     * チェックリストアイテム個別のチェック状態変更
     */
    fun toggleChecklistItem(index: Int, isChecked: Boolean) {
        if (index in checklistState.indices) {
            checklistState[index] = checklistState[index].copy(isChecked = isChecked)
        }
    }

    /**
     * データベースから予定を物理削除する
     */
    fun deleteTask(onSuccess: () -> Unit) {
        val currentItem = currentTaskWithTags ?: return

        viewModelScope.launch {
            // 1. データベースから削除を実行
            taskDao.deleteTask(currentItem.task)

            currentTaskWithTags = null
            // 2. 削除成功後の画面遷移コールバックを実行
            onSuccess()
        }
    }
}