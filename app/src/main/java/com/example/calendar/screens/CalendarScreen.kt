package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateToTaskCreate: () -> Unit,
    onNavigateToTaskList: () -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showMonthYearDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Menu, contentDescription = "設定", tint = Color(0xFF1C1B1F))
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 年月表示：クリックで洗練されたダイアログへ
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showMonthYearDialog = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${currentMonth.year}年 ${currentMonth.monthValue}月",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1B1F)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF5F6368),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 右端：細いラインのスタイリッシュな月切り替え
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "前月", tint = Color(0xFF5F6368))
                            }
                            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "次月", tint = Color(0xFF5F6368))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF8F9FA),
                tonalElevation = 0.dp,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTaskList,
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    label = { Text("一覧", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToTaskCreate,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("追加", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 曜日ヘッダー
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                val daysOfWeek = listOf("日", "月", "火", "水", "木", "金", "土")
                daysOfWeek.forEachIndexed { index, day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (index) {
                            0 -> Color(0xFFD93025) // 日曜：モダンレッド
                            6 -> Color(0xFF1A73E8) // 土曜：モダンブルー
                            else -> Color(0xFF70757A) // 平日：落ち着いたグレー
                        }
                    )
                }
            }

            // 日付データの算出
            val firstDayOfMonth = currentMonth.atDay(1)
            val dayOfWeekValue = firstDayOfMonth.dayOfWeek.value % 7
            val daysInMonth = currentMonth.lengthOfMonth()

            val totalGridItems = mutableListOf<LocalDate?>()
            for (i in 0 until dayOfWeekValue) totalGridItems.add(null)
            for (i in 1..daysInMonth) totalGridItems.add(currentMonth.atDay(i))

            val chunkedWeeks = totalGridItems.chunked(7)

            // ★熟考：極薄グレーを背景にし、マスの隙間(0.5dp)で完璧なグリッド線を表現
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFEFEF)), // グリッド線の色（ノイズのない極細ライトグレー）
                verticalArrangement = Arrangement.spacedBy(0.5.dp) // 縦のグリッド線
            ) {
                for (week in chunkedWeeks) {
                    Row(
                        modifier = Modifier.weight(1f), // 下部まで均等に100%引き伸ばす
                        horizontalArrangement = Arrangement.spacedBy(0.5.dp) // 横のグリッド線
                    ) {
                        for (date in week) {
                            if (date != null) {
                                val isSelected = date == selectedDate
                                val isToday = date == today

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            // ★熟考：選択日は優しいライトブルーの「面」で表現。普段は完全な白。
                                            if (isSelected) Color(0xFFE8F0FE) else Color.White
                                        )
                                        .clickable {
                                            if (isSelected) onNavigateToDateDetail(date)
                                            else selectedDate = date
                                        }
                                        .padding(top = 6.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (isToday) {
                                        // ★熟考：今日は「絶対的視認性」を誇るソリッドな青丸＋白文字
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(0xFF1A73E8).copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    } else {
                                        // 通常日・選択日
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when (date.dayOfWeek.value % 7) {
                                                0 -> Color(0xFFD93025)
                                                6 -> Color(0xFF1A73E8)
                                                else -> if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C1B1F)
                                            }
                                        )
                                    }
                                }
                            } else {
                                // 月の枠外の空白マス（白背景にして線を綺麗に通す）
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.White)
                                )
                            }
                        }

                        // 最終週の端数埋め
                        if (week.size < 7) {
                            for (i in 0 until (7 - week.size)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 年月クイック指定ダイアログ
        if (showMonthYearDialog) {
            var yearInput by remember { mutableStateOf(currentMonth.year.toString()) }
            var monthInput by remember { mutableStateOf(currentMonth.monthValue.toString()) }

            AlertDialog(
                onDismissRequest = { showMonthYearDialog = false },
                title = { Text("年月を指定", fontWeight = FontWeight.Bold) },
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = yearInput,
                            onValueChange = { yearInput = it },
                            label = { Text("年") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = monthInput,
                            onValueChange = { monthInput = it },
                            label = { Text("月") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val y = yearInput.toIntOrNull() ?: currentMonth.year
                        val m = monthInput.toIntOrNull() ?: currentMonth.monthValue
                        if (m in 1..12) currentMonth = YearMonth.of(y, m)
                        showMonthYearDialog = false
                    }) { Text("決定", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showMonthYearDialog = false }) { Text("キャンセル", color = Color.Gray) }
                }
            )
        }
    }
}