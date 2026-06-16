package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.ui.theme.CalendarTheme
import com.example.calendar.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                // MainActivityでアプリ唯一のインスタンスを生成
                val mainViewModel: CalendarViewModel = viewModel()

                // ナビゲーションにインスタンスを託す
                AppNavigation(viewModel = mainViewModel)
            }
        }
    }
}

// (Preview部分は以前のままでOKです)