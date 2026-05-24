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

@OptIn(ExperimentalMaterial3Api::class) // ★CenterAlignedTopAppBar の採用に伴い公式に付与
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
            // ==========================================
            // ★変更：Rowの手動配置から CenterAlignedTopAppBar へアップグレード！
            // ==========================================
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 年月選択テキストボタン
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

                        // 年月のすぐ右の上下ボタン
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.onNextMonth() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "翌月")
                            }
                            IconButton(
                                onClick = { viewModel.onPreviousMonth() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "前月")
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: メニュー */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "メニュー")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent // 元のRowと同じく背景に溶け込ませる
                )
            )

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
                        // ==========================================
                        // ★新設：この日付マス目に合致するタスクがあるかチェック
                        // ==========================================
                        val dayTasks = if (date != null) {
                            viewModel.tasks.filter { task -> task.startTime.toLocalDate() == date }
                        } else {
                            emptyList()
                        }

                        DateCell(
                            date = date,
                            isToday = date == LocalDate.now(),
                            // ★修正：selectedDate が LocalDateTime型 に昇格したため、.toLocalDate() で比較
                            isSelected = date == selectedDate.toLocalDate(),
                            onClick = { if (date != null) viewModel.onDateSelected(date) }
                            // ※注意：もし今後 DateCell 側でタスクをアスタリスクや点、文字で表示したい場合は、
                            // 引数に「tasks = dayTasks」のようにデータを渡せるように拡張してください。
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