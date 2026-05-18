package com.example.calendar.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import com.example.calendar.data.entity.Task
import com.example.calendar.domain.TaskManager
import com.example.calendar.state.TaskInputState
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel : ViewModel() {
    private val _currentMonth = mutableStateOf(YearMonth.now())
    val currentMonth: State<YearMonth> = _currentMonth

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

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

    // --- 1. 土台：入力状態の管理 ---
    // 外部からは読み取れるが、書き換えはViewModel内のみに制限 (private set)
    var inputState by mutableStateOf(TaskInputState())
        private set

    // 外部（UI）から状態を安全に更新するための窓口
    // .copy() を使って特定の項目だけを書き換えた新しい状態を生成する
    fun updateInput(onUpdate: (TaskInputState) -> TaskInputState) {
        inputState = onUpdate(inputState)
    }

    // --- 2. 保存済みのリストとロジックの準備 ---
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    // --- 3. 保存処理の実装 ---
    /**
     * 手動入力 (Manual) でタスクを保存する
     */
    fun saveManualTask() {
        val newTask = Task(
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
            autoCompleted = false // ER図にある boolean 項目
        )

        _tasks.add(newTask)
        inputState = TaskInputState() // 下書きリセット
    }

}

