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
        // 現在の inputState (下書き) を Task エンティティに変換
        val newTask = Task(
            title = inputState.title,
            date = inputState.date,
            startTime = inputState.startTime,
            endTime = inputState.endTime,
            color = inputState.color ?: 0xFF000000.toInt(), // nullなら黒(0xFF000000)
            memo = inputState.memo.takeIf { it.isNotBlank() }, // 空文字ならnullとして扱う
            dayCountTarget = inputState.dayCountTarget,
            isAutoCompleted = inputState.isAutoCompleted,
            completeState = "NOT_COMPLETED"
        )
        
        // リストに追加（将来的にここがDB保存に変わる）
        _tasks.add(newTask)
        
        // 保存が終わったら下書きをリセットして次の入力に備える
        inputState = TaskInputState()
    fun toggleTagSelection(tag: Tag) {
        updateInput { currentState ->
            val isSelected = currentState.selectedTags.contains(tag)
            val newTags = if (isSelected) {
                currentState.selectedTags - tag // 選択解除
            } else {
                currentState.selectedTags + tag // 新規選択
            }

            // タグが解除されたら、そのタグが持っていたカスタム入力欄のデータもMapから消去する
            val newValues = currentState.customFieldValues.toMutableMap()
            if (isSelected && tag.customFieldLabel != null) {
                newValues.remove(tag.customFieldLabel)
            }

            currentState.copy(
                selectedTags = newTags,
                customFieldValues = newValues
            )
        }
    }

    /**
     * 動的入力欄の文字が書き換わったときに呼び出される関数
     * @param label 入力欄のタイトル（例：「責任者」）
     * @param value 入力された文字（例：「山田太郎」）
     */
    fun updateCustomFieldValue(label: String, value: String) {
        updateInput { currentState ->
            val newValues = currentState.customFieldValues.toMutableMap()
            newValues[label] = value // Mapの中身を更新、または新規追加
            currentState.copy(customFieldValues = newValues)
        }
    }
    }

}

