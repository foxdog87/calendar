package com.example.calendar.screens // 住所を screens に変更

import MonthYearPickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calendar.components.DateCell
import com.example.calendar.components.DayOfWeekRow
import com.example.calendar.viewmodel.CalendarViewModel // ViewModelをインポート
import java.time.LocalDate

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    var currentScreenMode by remember { mutableStateOf("CALENDAR") }

    // ログ1: 今どちらのモードで画面が描画されようとしているかを追跡
    println("現在の画面モード: $currentScreenMode")

    if (currentScreenMode == "CREATE") {
        TaskCreateScreen(
            viewModel = viewModel,
            onNavigateBack = {
                println("カレンダー画面に戻ります")
                currentScreenMode = "CALENDAR"
            }
        )
    } else {
        val currentMonth by viewModel.currentMonth
        val selectedDate by viewModel.selectedDate
        val daysInMonth =
            remember(currentMonth) { viewModel.buildCalendarMatrix(currentMonth, "MONTH") }
        val showDatePicker by viewModel.showDatePicker

        // ★ ここでダイアログを「設置」する
        if (showDatePicker) {
            MonthYearPickerDialog(
                currentMonth = currentMonth,
                onDismiss = { viewModel.dismissDatePicker() }, // キャンセル時
                onConfirm = { year, month ->
                    viewModel.updateYearMonth(year, month) // 確定時：年月を更新
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // --- トップバー部分（年月表示のすぐ右に上下ボタンを一直線に配置） ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. メニューアイコン（左端）
                IconButton(onClick = { /* TODO: メニュー */ }) {
                    Icon(Icons.Default.Menu, contentDescription = "メニュー")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 2. 年月表示とボタンのセット
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 年月
                    TextButton(
                        onClick = { viewModel.onMonthYearPickerClick() },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    // ★ ここ：年月表示のすぐ右に、上下ボタンを垂直に配置
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.onNextMonth() }, // Upで翌月
                            modifier = Modifier.size(28.dp) // 少しサイズ調整
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "翌月")
                        }
                        IconButton(
                            onClick = { viewModel.onPreviousMonth() }, // Downで前月
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "前月")
                        }
                    }
                }
            }

            // --- 曜日ヘッダー ---
            DayOfWeekRow()

            // --- カレンダー本体 ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .border(1.dp, Color.LightGray)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(daysInMonth) { date ->
                        DateCell(
                            date = date,
                            isToday = date == LocalDate.now(),
                            isSelected = date == selectedDate,
                            onClick = { if (date != null) viewModel.onDateSelected(date) }
                        )
                    }
                }
            }

            // --- ボトムボタン（画面最下部まで白背景を伸ばす） ---
            Surface(
                color = Color.White, // 背景を白に
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp // 境目を少しはっきりさせる
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // ★重要：ボタンの中身だけを上に持ち上げ、背景（Surface）は下まで伸ばす
                        .height(64.dp)
                        .border(0.5.dp, Color.LightGray)
                ) {
                    TextButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.List, null, tint = Color.DarkGray)
                            Text("リスト", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                    TextButton(
                        onClick = {
                            // ログ2: ボタンが物理的にタップされた瞬間に走るか確認
                            println("★追加ボタンがタップされました！モードをCREATEに書き換えます。")
                            currentScreenMode = "CREATE"
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, null, tint = Color.DarkGray)
                            Text("追加", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}