package com.example.calendar.screens // 住所を screens に変更

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.components.DateCell
import com.example.calendar.components.buildMonthDates
import com.example.calendar.viewmodel.CalendarViewModel // ViewModelをインポート
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    // 以前解説した "by" を使うための import
    val currentMonth by viewModel.currentMonth
    val daysInMonth = remember(currentMonth) { buildMonthDates(currentMonth) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onPreviousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "前の月")
                }

                Text(
                    text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = { viewModel.onNextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "次の月")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

// 曜日のヘッダー
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("日", "月", "火", "水", "木", "金", "土")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = when (day) {
                            "日" -> Color.Red
                            "土" -> Color.Blue
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

// カレンダー本体
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.5.dp, Color.LightGray)
            ) {
                items(daysInMonth) { date ->
                    DateCell(
                        date = date,
                        isToday = date == LocalDate.now(),
                        onClick = { /* 詳細画面への遷移処理 */ }
                    )
                }
            }
        }
    }
}
