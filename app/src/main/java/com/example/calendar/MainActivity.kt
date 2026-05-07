package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.calendar.screens.CalendarScreen // 新しく作った住所から呼ぶ
import com.example.calendar.ui.theme.CalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                // メイン画面を呼び出すだけ！
                CalendarScreen()
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun MainActivityPreview() {
    CalendarTheme {
        // MainActivityと同じようにCalendarScreenを呼び出す
        CalendarScreen()
    }
}
fun test(){}