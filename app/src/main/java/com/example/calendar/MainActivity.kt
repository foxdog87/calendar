package com.example.calendar

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.screens.CalendarScreen
import com.example.calendar.ui.theme.CalendarTheme
import com.example.calendar.viewmodel.CalendarViewModel // ★追加：ViewModelのインポート
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                // 1. ★ここでアプリ唯一の共通ViewModelを生成する
                val mainViewModel: CalendarViewModel = viewModel()

                // 2. ★生成したインスタンスを明示的に引数で渡す！
                CalendarScreen(viewModel = mainViewModel)
            }
        }
    }
}

private val previewViewModelInstance = CalendarViewModel(
    initialMonth = YearMonth.of(2026, 5),
    initialDate = LocalDate.of(2026, 5, 1)
)

@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    CalendarTheme {

        CalendarScreen(viewModel = previewViewModelInstance)
    }
}