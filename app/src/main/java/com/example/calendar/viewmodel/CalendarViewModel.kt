package com.example.calendar.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel : ViewModel() {
    // 状態（データ）を保持する変数を、カプセル化（private）して定義
    private val _currentMonth = mutableStateOf(YearMonth.now())

    // UI側が読み取るための公開プロパティ
    val currentMonth: State<YearMonth> = _currentMonth

    // 「前の月」ボタンが押された時の処理
    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    // 「次の月」ボタンが押された時の処理
    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }
    private val _selectedDate = mutableStateOf(LocalDate.now())
    val selectedDate: State<LocalDate> = _selectedDate

    // ★追加：日付がクリックされた時の処理
    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date
    }

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    // 年月表示が押されたとき
    fun onMonthYearPickerClick() {
        _showDatePicker.value = true
    }

    // ダイアログを閉じる
    fun dismissDatePicker() {
        _showDatePicker.value = false
    }

    // 年月を更新する
    fun updateYearMonth(year: Int, month: Int) {
        _currentMonth.value = YearMonth.of(year, month)
        _showDatePicker.value = false
    }
}
