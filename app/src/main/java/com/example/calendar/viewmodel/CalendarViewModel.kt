package com.example.calendar.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.TagCustomField
import com.example.calendar.data.entity.Task
import com.example.calendar.data.entity.TaskTag       // ★理由2で追加
import com.example.calendar.state.TaskInputState
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    initialMonth: YearMonth = YearMonth.now(),
    initialDate: LocalDate = LocalDate.now()
) : ViewModel() {

    // === 月選択・日付選択ロジック（渡された初期値を使用） ===
    private val _currentMonth = mutableStateOf(initialMonth) // ★修正：実時間を直呼びせず初期値を使う
    val currentMonth: State<YearMonth> = _currentMonth

    private val _selectedDate = mutableStateOf(initialDate)   // ★修正：実時間を直呼びせず初期値を使う
    val selectedDate: State<LocalDate> = _selectedDate

    fun buildCalendarMatrix(yearMonth: YearMonth, mode: String): List<LocalDate?> {
        return when (mode) {
            "WEEK" -> buildWeekCalendarMatrix(yearMonth)
            "DAY" -> buildDayCalendarMatrix(yearMonth)
            else -> {
                // デフォルト（月単位）：元の buildMonthDates のロジックを100%継承
                val dates = mutableListOf<LocalDate?>()
                val firstDay = yearMonth.atDay(1)

                // 1日の曜日を基準に、前月分の余白を null で埋める
                val firstDayOfWeek = firstDay.dayOfWeek.value % 7
                repeat(firstDayOfWeek) { dates.add(null) }

                // 1日から末日までを追加
                for (day in 1..yearMonth.lengthOfMonth()) {
                    dates.add(yearMonth.atDay(day))
                }

                // 7の倍数になるように、翌月分の余白を null で埋める
                while (dates.size % 7 != 0) { dates.add(null) }

                dates
            }
        }
    }

    private fun buildWeekCalendarMatrix(yearMonth: YearMonth): List<LocalDate?> {
        val startOfWeek = _selectedDate.value.minusDays(_selectedDate.value.dayOfWeek.value % 7 .toLong())
        return (0 until 7).map { startOfWeek.plusDays(it.toLong()) }
    }

    private fun buildDayCalendarMatrix(yearMonth: YearMonth): List<LocalDate?> {
        return listOf(_selectedDate.value)
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }


    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    fun onMonthYearPickerClick() {
        _showDatePicker.value = true
    }

    fun dismissDatePicker() {
        _showDatePicker.value = false
    }

    fun updateYearMonth(year: Int, month: Int) {
        _currentMonth.value = YearMonth.of(year, month)
        _showDatePicker.value = false
    }

    // === [完全維持] 1. 土台：入力状態の管理 ===
    var inputState by mutableStateOf(TaskInputState())
        private set

    fun updateInput(onUpdate: (TaskInputState) -> TaskInputState) {
        inputState = onUpdate(inputState)
    }

    // === [完全維持] マスタデータ ===
    val sampleCustomFields = listOf(
        TagCustomField(fieldId = 1, tagId = 1, fieldName = "提出先"),
        TagCustomField(fieldId = 2, tagId = 1, fieldName = "点数"),
        TagCustomField(fieldId = 3, tagId = 2, fieldName = "部屋番号")
    )

    fun toggleTagSelection(tag: Tag) {
        updateInput { currentState ->
            val isSelected = currentState.selectedTags.contains(tag)
            val newTags = if (isSelected) currentState.selectedTags - tag else currentState.selectedTags + tag

            val newValues = currentState.customFieldValues.toMutableMap()
            if (isSelected) {
                val fieldsToRemove = sampleCustomFields.filter { it.tagId == tag.tagId }
                fieldsToRemove.forEach { newValues.remove(it) }
            }

            currentState.copy(
                selectedTags = newTags,
                customFieldValues = newValues
            )
        }
    }

    fun updateCustomFieldValue(field: TagCustomField, value: String) {
        updateInput { currentState ->
            val newValues = currentState.customFieldValues.toMutableMap()
            newValues[field] = value
            currentState.copy(customFieldValues = newValues)
        }
    }

    // === [完全維持] 2. 保存済みのリスト ===
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    // ==========================================
    // ★設計変更（中間テーブル対応）による必要最小限の変更・追加
    // ==========================================

    // 理由2: タスクとタグの結びつき（多対多）を保存する中間テーブル用のリストを新設
    private val _taskTags = mutableStateListOf<TaskTag>()
    val taskTags: List<TaskTag> = _taskTags

    // 理由2: 新しいタスクのIDを擬似的に自動採番するためのカウンター
    private var nextTaskId = 1

    // --- 3. 保存処理の実装（改名・拡張） ---
    /**
     * ★修正：手動入力 (Manual) でタスクとタグの連関データを同時に保存する
     */
    // 理由1: プログラム一覧表の変更に合わせ、関数名を `saveManualTask` から `saveTask` に改名
    fun saveTask() {
        val currentId = nextTaskId++ // 新しいTask用のIDを発行

        val newTask = Task(
            taskId = currentId, // ★理由2: 発行した固有IDをセット
            title = inputState.title,
            date = inputState.date,
            startTime = inputState.startTime,
            endTime = inputState.endTime,
            color = inputState.color ?: 0xFF000000.toInt(),
            memo = inputState.memo.takeIf { it.isNotBlank() },
            location = inputState.location.takeIf { it.isNotBlank() },
            url = inputState.url.takeIf { it.isNotBlank() },
            checkList = inputState.checkList.takeIf { it.isNotBlank() },
            attachmentPath = inputState.attachmentPath,
            dayCountTarget = inputState.dayCountTarget,
            completeState = "NOT_COMPLETED",
            autoCompleted = false
        )

        _tasks.add(newTask)

        // ★理由2: 画面で選択されていたタグを、中間テーブル（TaskTag）の形式に変換して一括保存
        inputState.selectedTags.forEach { tag ->
            val linkedData = TaskTag(taskId = currentId, tagId = tag.tagId)
            _taskTags.add(linkedData)
        }

        inputState = TaskInputState() // 下書きリセット
    }
}