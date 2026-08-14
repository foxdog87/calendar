package com.example.calendar.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.data.dao.TaskDao
import com.example.calendar.data.relation.TaskWithTags
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId // ★ 追加
import java.time.ZoneOffset

class CalendarViewModel(
    private val taskDao: TaskDao, // Roomからデータを引っ張るために注入
    initialMonth: YearMonth = YearMonth.now(),
    initialDateTime: LocalDateTime = LocalDateTime.now()
) : ViewModel() {

    // =================================================================
    // 1. データベース（Room）連携ロジック
    // =================================================================

    // データベースからすべてのタスクを取得し、カレンダー描画用に「日付（LocalDate）」ごとのリストにリアルタイム集計
    val tasksByDate: StateFlow<Map<LocalDate, List<TaskWithTags>>> = taskDao.getAllTasksWithTags()
        .map { totalList ->
            // ★ 修正（33行目付近）：EpochSecond の Long型をシステムローカルタイムゾーン基準で LocalDateTime にパースし、LocalDateでグループ化
            totalList.groupBy { item ->
                LocalDateTime.ofInstant(Instant.ofEpochSecond(item.task.startTime), ZoneId.systemDefault()).toLocalDate()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // =================================================================
    // 2. カレンダー・月選択・日付選択ロジック（完全維持＆最適化）
    // =================================================================

    private val _currentMonth = mutableStateOf(initialMonth)
    val currentMonth: State<YearMonth> = _currentMonth

    private val _selectedDate = mutableStateOf(initialDateTime)
    val selectedDate: State<LocalDateTime> = _selectedDate

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    /**
     * 指定された表示モード（月・週・日）に応じてカレンダーの行列（マトリクス）をビルド
     */
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
        val localDate = _selectedDate.value.toLocalDate()
        val startOfWeek = localDate.minusDays((localDate.dayOfWeek.value % 7).toLong())
        return (0 until 7).map { startOfWeek.plusDays(it.toLong()) }
    }

    private fun buildDayCalendarMatrix(yearMonth: YearMonth): List<LocalDate?> {
        return listOf(_selectedDate.value.toLocalDate())
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    /**
     * 日付がタップされた時の処理。時間情報を壊さずに日付だけを更新
     */
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
}