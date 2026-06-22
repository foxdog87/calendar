package com.example.calendar.viewmodel

import android.content.Context
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
import com.example.calendar.notification.TaskAlarmScheduler
import com.example.calendar.state.TaskInputState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class TaskCreateViewModel(
    private val taskRepository: TaskRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // 画面の入力状態を管理するState
    var inputState by mutableStateOf(TaskInputState())
        private set

    // タイトルが空で保存しようとした際のエラー状態を管理するState
    var isTitleError by mutableStateOf(false)
        private set
    var isDateTimeError by mutableStateOf(false)
        private set

    // 永続化：選択肢として表示するマスタータグ一覧をデータベースから取得
    val allTags: StateFlow<List<Tag>> = tagRepository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateInput(transform: (TaskInputState) -> TaskInputState) {
        inputState = transform(inputState)
    }

    // 初期時刻を現在から最も近い「X時00分」にし、終了はその1時間後にする
    fun prepareCreateTask(selectedDate: LocalDate) {

        val startDateTime = java.time.LocalDateTime.now()
            .plusHours(1)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val adjustedStart =
            selectedDate.atTime(startDateTime.hour, 0)

        val adjustedEnd =
            adjustedStart.plusHours(1)

        isTitleError = false
        isDateTimeError = false

        inputState = TaskInputState(
            startTime = adjustedStart
                .atZone(ZoneId.systemDefault())
                .toEpochSecond(),

            endTime = adjustedEnd
                .atZone(ZoneId.systemDefault())
                .toEpochSecond(),

            selectedTags = emptyList(),
            isAllDay = false
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

    // 未入力バリデーション（赤枠化）と、非同期処理完了後に安全に画面を閉じるロジック
    fun saveTask(context: Context, onSuccess: () -> Unit) {

        if (inputState.title.trim().isBlank()) {
            isTitleError = true
            return
        }

        isTitleError = false

        if (
            !inputState.isAllDay &&
            inputState.endTime <= inputState.startTime
        ) {
            isDateTimeError = true
            return
        }

        isDateTimeError = false

        viewModelScope.launch {

            val finalStartTime: Long
            val finalEndTime: Long

            if (inputState.isAllDay) {

                // ★ 修正：システムローカルのタイムゾーンでミリ秒からLocalDateを復元
                val localDate =
                    Instant.ofEpochSecond(inputState.startTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                // ★ 修正：終日の始まり(00:00)と終わり(23:59:59)をローカルタイムゾーン準拠で秒に換算
                finalStartTime =
                    localDate.atTime(0, 0)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()

                finalEndTime =
                    localDate.atTime(23, 59, 59)
                        .atZone(ZoneId.systemDefault())
                        .toEpochSecond()

            } else {

                finalStartTime = inputState.startTime
                finalEndTime = inputState.endTime
            }

            val newTask = Task(
                title = inputState.title,
                startTime = finalStartTime,
                endTime = finalEndTime,
                memo = inputState.memo,
                checkList = inputState.checkList,
                color = inputState.color ?: 0,
                attachmentPath = inputState.attachmentPath,
                url = inputState.url,
                latitude = inputState.latitude,
                longitude = inputState.longitude,
                isAutoCompleted = inputState.isAutoCompleted,
                completeState = "INCOMPLETE",
                remindMinutes = inputState.remindMinutes,
                dayCountTarget = null,
                templateId = null,
                isAllDay = inputState.isAllDay
            )

            val generatedId =
                taskRepository.insertTaskWithTags(
                    newTask,
                    inputState.selectedTags
                )

            val savedTask =
                newTask.copy(taskId = generatedId)

            android.util.Log.d(
                "ALARM_TEST",
                """
            saveTask called
            taskId=$generatedId
            title=${savedTask.title}
            remindMinutes=${savedTask.remindMinutes}
            startTime=${savedTask.startTime}
            endTime=${savedTask.endTime}
            """.trimIndent()
            )

            val scheduler = TaskAlarmScheduler(context)
            scheduler.schedule(savedTask)

            withContext(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // 作成画面で新しいタグが作られたらデータベースへ永続保存する
    fun createTag(tag: Tag) {
        viewModelScope.launch {
            val newId = tagRepository.insertTag(tag)
            val savedTag = tag.copy(tagId = newId)
            toggleTagSelection(savedTag)
        }
    }

    // 作成画面でタグが長押し削除されたらデータベースから消去する
    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            if (inputState.selectedTags.contains(tag)) {
                toggleTagSelection(tag)
            }
        }
    }

    // テンプレートの適用ロジック
    fun applyTemplate(template: Template) {

        val (startEpoch, defaultEndEpoch) =
            createDefaultDateTimePair()

        val endEpoch =
            startEpoch + template.timeLength

        inputState = inputState.copy(
            title = template.title,
            startTime = startEpoch,
            endTime = endEpoch,
            memo = template.memo ?: "",
            checkList = template.checkList ?: "",
            color = template.color,
            latitude = template.latitude,
            longitude = template.longitude,
            url = template.url ?: "",
            attachmentPath = template.attachmentPath ?: "",
            isAutoCompleted = template.isAutoCompleted,
            remindMinutes = template.remindMinutes
        )
    }
}

private fun createDefaultDateTimePair(): Pair<Long, Long> {

    val startDateTime = java.time.LocalDateTime.now()
        .plusHours(1)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    val endDateTime = startDateTime.plusHours(1)

    // ★ 修正：システムローカルタイムゾーンを考慮してEpoch秒を生成
    return Pair(
        startDateTime
            .atZone(ZoneId.systemDefault())
            .toEpochSecond(),

        endDateTime
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
    )
}