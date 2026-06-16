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
import com.example.calendar.data.entity.TaskTag
import com.example.calendar.state.TaskInputState
import java.time.LocalDate
import java.time.LocalDateTime // ★LocalDateTime を追加
import java.time.YearMonth

// ★余計な @JvmOverloads constructor は完全削除。
// 「開始日時・終了日時」への設計変更に伴い、initialDate から initialDateTime (LocalDateTime型) に変更
class CalendarViewModel(
    initialMonth: YearMonth = YearMonth.now(),
    initialDateTime: LocalDateTime = LocalDateTime.now()
) : ViewModel() {

    // === 月選択・日付選択ロジック（渡された初期値を使用） ===
    private val _currentMonth = mutableStateOf(initialMonth)
    val currentMonth: State<YearMonth> = _currentMonth

    // ★修正：下部に散らばっていた定義を最上部に集約。型を LocalDateTime に変更
    private val _selectedDate = mutableStateOf(initialDateTime)
    val selectedDate: State<LocalDateTime> = _selectedDate

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    fun buildCalendarMatrix(yearMonth: YearMonth, mode: String): List<LocalDate?> {
        return when (mode) {
            "WEEK" -> buildWeekCalendarMatrix(yearMonth)
            "DAY" -> buildDayCalendarMatrix(yearMonth)
            else -> {
                val dates = mutableListOf<LocalDate?>()
                val firstDay = yearMonth.atDay(1)

                val firstDayOfWeek = firstDay.dayOfWeek.value % 7
                repeat(firstDayOfWeek) { dates.add(null) }

                for (day in 1..yearMonth.lengthOfMonth()) {
                    dates.add(yearMonth.atDay(day))
                }

                while (dates.size % 7 != 0) { dates.add(null) }

                dates
            }
        }
    }

    private fun buildWeekCalendarMatrix(yearMonth: YearMonth): List<LocalDate?> {
        // ★修正：_selectedDate から .toLocalDate() で日付部分だけを取り出して計算
        val localDate = _selectedDate.value.toLocalDate()
        val startOfWeek = localDate.minusDays(localDate.dayOfWeek.value % 7 .toLong())
        return (0 until 7).map { startOfWeek.plusDays(it.toLong()) }
    }

    private fun buildDayCalendarMatrix(yearMonth: YearMonth): List<LocalDate?> {
        // ★修正：_selectedDate から .toLocalDate() で日付部分だけを取り出す
        return listOf(_selectedDate.value.toLocalDate())
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    // ★修正：日付が選択された時、現在の「時間情報」を壊さないように結合して保持
    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date.atTime(_selectedDate.value.toLocalTime())
    }

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
        TagCustomField(fieldId = 1, tagId = 1L, fieldName = "提出先"),
        TagCustomField(fieldId = 2, tagId = 1L, fieldName = "点数"),
        TagCustomField(fieldId = 3, tagId = 2L, fieldName = "部屋番号")
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
    // === 2. 保存済みのリスト（すべてのタスクデータはここに集約されます） ===
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks // ★画面側からは、この tasks を直接見にいきます

    private val _taskTags = mutableStateListOf<TaskTag>()
    val taskTags: List<TaskTag> = _taskTags

    private var nextTaskId = 1L

    // --- 3. 保存処理の実装 ---
    fun saveTask() {
        val currentId = nextTaskId++

        val newTask = Task(
            taskId = currentId,
            title = inputState.title,
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

        _tasks.add(newTask) // ViewModel内のリストに直接追加！

        inputState.selectedTags.forEach { tag ->
            // taskId に渡す currentId を、確実に Long 型にして TaskTag を作成します
            val linkedData = TaskTag(
                taskId = currentId.toLong(), // ★ .toLong() を追加して型を確実に合わせる
                tagId = tag.tagId           // ※ もしここでもエラーが出る場合は tag.tagId.toLong() も検討
            )
            _taskTags.add(linkedData)
        }

        inputState = TaskInputState() // 下書きリセット
    }
}
