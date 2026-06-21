package com.example.calendar.viewmodel

import android.content.Context // ★ 追加
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.Template
import com.example.calendar.data.repository.TagRepository
import com.example.calendar.data.repository.TaskRepository
import com.example.calendar.notification.TaskAlarmScheduler // ★ 追加
import com.example.calendar.state.TaskInputState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class TaskCreateViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // 画面の入力状態を管理するState
    var inputState by mutableStateOf(TaskInputState())
        private set

    // ★ 永続化：選択肢として表示するマスタータグ一覧をデータベースから取得
    val allTags: StateFlow<List<Tag>> = tagRepository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateInput(transform: (TaskInputState) -> TaskInputState) {
        inputState = transform(inputState)
    }

    // ★ 新設（問題1の解決）：カレンダー画面で選択された日付を初期値として自動設定する
    fun prepareCreateTask(selectedDate: LocalDate) {
        // 選択された日付の「朝9時」から「朝10時」をデフォルトの初期値にする
        val startDateTime = selectedDate.atTime(9, 0)
        val endDateTime = selectedDate.atTime(10, 0)

        // RoomやRepositoryがUnixタイムスタンプ（Long型秒数）で扱っている形に変換してセット
        val startEpoch = startDateTime.toEpochSecond(ZoneOffset.UTC)
        val endEpoch = endDateTime.toEpochSecond(ZoneOffset.UTC)

        // 画面の初期状態へ反映
        inputState = TaskInputState(
            startTime = startEpoch,
            endTime = endEpoch,
            selectedTags = emptyList() // 開くたびに選択タグは一回クリアする
        )
    }

    // タグの選択状態を切り替える
    fun toggleTagSelection(tag: Tag) {
        val currentSelected = inputState.selectedTags.toMutableList()
        if (currentSelected.contains(tag)) {
            currentSelected.remove(tag)
        } else {
            currentSelected.add(tag)
        }
        inputState = inputState.copy(selectedTags = currentSelected)
    }

    // ★ 修正：引数に context を追加し、保存成功時にアラーム予約を走らせる
    fun saveTask(context: Context, onSuccess: () -> Unit) {
        if (inputState.title.isBlank()) return // タイトル空っぽならガード

        viewModelScope.launch {
            val newTask = Task(
                title = inputState.title,
                startTime = inputState.startTime,
                endTime = inputState.endTime,
                memo = inputState.memo,
                checkList = inputState.checkList,
                color = inputState.color ?: 0, // ★ エラー回避: Nullならデフォルト色(0)にする
                attachmentPath = inputState.attachmentPath,
                url = inputState.url,
                latitude = inputState.latitude,
                longitude = inputState.longitude,
                isAutoCompleted = inputState.isAutoCompleted,
                completeState = "INCOMPLETE", // 初期は未完了

                // ★ 追加：UIから入力された通知設定（何分前か）を保存
                remindMinutes = inputState.remindMinutes,

                dayCountTarget = null,
                templateId = null
            )

            // 1. Repository経由でタスク本体と中間テーブルへ同時保存し、発行された taskId を取得
            val generatedId = taskRepository.insertTaskWithTags(newTask, inputState.selectedTags)

            // 2. 正しいIDを持った状態の Task オブジェクトを作成
            val savedTask = newTask.copy(taskId = generatedId)

            // 3. アラームスケジューラーを呼び出してOSに通知予約を入れる
            val scheduler = TaskAlarmScheduler(context)
            scheduler.schedule(savedTask)

            onSuccess()
        }
    }

    // ★ 新設：作成画面で新しいタグが作られたらデータベースへ永続保存する
    fun createTag(tag: Tag) {
        viewModelScope.launch {
            val newId = tagRepository.insertTag(tag)
            val savedTag = tag.copy(tagId = newId)
            toggleTagSelection(savedTag)
        }
    }


    // ★ 新設：作成画面でタグが長押し削除されたらデータベースから消去する
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            if (inputState.selectedTags.contains(tag)) {
                toggleTagSelection(tag)
            }
        }
    }

    // テンプレートの適用ロジック（既存）
    fun applyTemplate(template: Template) {
        val now = Instant.now().epochSecond
        inputState = inputState.copy(
            title = template.title,
            startTime = now,
            endTime = now + template.timeLength,
            memo = template.memo ?: "",
            checkList = template.checkList ?: "",
            color = template.color,
            latitude = template.latitude,
            longitude = template.longitude,
            url = template.url ?: "",
            attachmentPath = template.attachmentPath ?: "",
            isAutoCompleted = template.isAutoCompleted,
            remindMinutes = template.remindMinutes // ★ テンプレート側の通知設定も引き継ぐ
        )
    }
}