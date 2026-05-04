package com.example.calendar.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
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
}