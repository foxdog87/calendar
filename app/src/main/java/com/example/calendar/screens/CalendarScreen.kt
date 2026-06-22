package com.example.calendar.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.components.TagIconBadge // ★ 先ほど作った丸背景の共通コンポーネントをインポート
import com.example.calendar.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToTaskCreate: (LocalDate) -> Unit,
    onNavigateToTaskList: () -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentMonth by viewModel.currentMonth
    val selectedDateTime by viewModel.selectedDate
    val tasksByDate by viewModel.tasksByDate.collectAsState()

    val selectedDate = selectedDateTime.toLocalDate()
    var showMonthYearDialog by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val softGrayBackground = Color(0xFFF8F9FA)

    // 左右スワイプ用の PagerState 構築
    val maxMonths = 2400
    val initialPage = maxMonths / 2
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { maxMonths }
    )

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = (pagerState.currentPage - initialPage).toLong()
        val targetMonth = YearMonth.now().plusMonths(monthOffset)
        if (targetMonth != currentMonth) {
            viewModel.updateYearMonth(targetMonth.year, targetMonth.monthValue)
        }
    }

    LaunchedEffect(currentMonth) {
        val currentYearMonth = YearMonth.now()
        val expectedPage = initialPage + (
                (currentMonth.year - currentYearMonth.year) * 12 +
                        (currentMonth.monthValue - currentYearMonth.monthValue)
                )
        if (pagerState.currentPage != expectedPage && expectedPage in 0 until maxMonths) {
            pagerState.scrollToPage(expectedPage)
        }
    }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showMonthYearDialog = true }
                                .padding(top = 4.dp, bottom = 4.dp)
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.onPreviousMonth() }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "前月", tint = Color(0xFF5F6368))
                            }
                            IconButton(onClick = { viewModel.onNextMonth() }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "次月", tint = Color(0xFF5F6368))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = softGrayBackground)
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 4.dp,
                color = softGrayBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToTaskList,
                        icon = { Icon(Icons.Default.FormatListBulleted, null) },
                        text = { Text("一覧") }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToTaskCreate(selectedDate) },
                        icon = { Icon(Icons.Default.Add, null) },
                        text = { Text("追加") }
                    )
                }
            }
        },
        containerColor = softGrayBackground
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
                    .padding(top = 10.dp, bottom = 10.dp)
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
                            0 -> Color(0xFFD93025)
                            6 -> Color(0xFF1A73E8)
                            else -> Color(0xFF70757A)
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFEFEFEF))
            ) { page ->
                val monthOffset = (page - initialPage).toLong()
                val pageMonth = YearMonth.now().plusMonths(monthOffset)

                val totalGridItems = viewModel.buildCalendarMatrix(pageMonth, "MONTH")
                val chunkedWeeks = totalGridItems.chunked(7)

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.5.dp)
                ) {
                    for (week in chunkedWeeks) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(0.5.dp)
                        ) {
                            for (date in week) {
                                if (date != null) {
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val dayTasks = tasksByDate[date] ?: emptyList()

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (isSelected) Color(0xFFE8F0FE) else Color.White)
                                            .clickable {
                                                if (isSelected) {
                                                    onNavigateToDateDetail(date)
                                                } else {
                                                    viewModel.onDateSelected(date)
                                                }
                                            }
                                            .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // 日付の数字部分
                                        if (isToday) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color(0xFF1A73E8), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = date.dayOfMonth.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = when (date.dayOfWeek.value % 7) {
                                                    0 -> Color(0xFFD93025)
                                                    6 -> Color(0xFF1A73E8)
                                                    else -> if (isSelected) Color(0xFF1A73E8) else Color(0xFF1C1B1F)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // タスクの簡易一覧表示
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            dayTasks.take(3).forEach { item ->
                                                val task = item.task
                                                val isCompleted = task.completeState == "COMPLETED"
                                                val mainTag = item.tags.firstOrNull()

                                                val baseColor = if (mainTag != null) Color(mainTag.color) else (if (task.color == 0) Color(0xFF1A73E8) else Color(task.color))
                                                val badgeBgColor = if (isCompleted) Color(0xFFE8EAED) else baseColor.copy(alpha = 0.12f)
                                                val contentColor = if (isCompleted) Color(0xFF9AA0A6) else baseColor
                                                val hasIcon = !mainTag?.icon.isNullOrBlank()

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(20.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(badgeBgColor)
                                                        .padding(horizontal = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Start
                                                ) {
                                                    if (hasIcon && mainTag != null) {
                                                        // ★ 修正優先度③: 共通化した `TagIcon` を直接呼び出し、不要なwhenや独自の変換処理を全削除！
                                                        TagIconBadge(
                                                            tag = mainTag,
                                                            size = 14.dp,    // マスに収まるように極小サイズ
                                                            iconSize = 10.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                    }

                                                    Text(
                                                        text = task.title,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = if (hasIcon) Color(0xFF1C1B1F) else contentColor,
                                                        letterSpacing = (-0.4).sp,
                                                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(bottom = 0.5.dp)
                                                    )
                                                }
                                            }

                                            if (dayTasks.size > 3) {
                                                Text(
                                                    text = "+${dayTasks.size - 3}",
                                                    fontSize = 8.sp,
                                                    color = Color(0xFF70757A),
                                                    modifier = Modifier.padding(start = 2.dp),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(Color.White)
                                    )
                                }
                            }

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
        }

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
                        if (m in 1..12) viewModel.updateYearMonth(y, m)
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