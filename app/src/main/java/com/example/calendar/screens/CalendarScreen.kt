package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag // ★ 追加
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
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
            NavigationBar(
                containerColor = softGrayBackground,
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
                    onClick = { onNavigateToTaskCreate(selectedDate) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("追加", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                )
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

            val totalGridItems = viewModel.buildCalendarMatrix(currentMonth, "MONTH")
            val chunkedWeeks = totalGridItems.chunked(7)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFEFEF)),
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

                                            // カラーロジック
                                            val baseColor = if (mainTag != null) Color(mainTag.color) else (if (task.color == 0) Color(0xFF1A73E8) else Color(task.color))
                                            val badgeBgColor = if (isCompleted) Color(0xFFE8EAED) else baseColor.copy(alpha = 0.12f)
                                            val contentColor = if (isCompleted) Color(0xFF9AA0A6) else baseColor

                                            // 白を足したようなワントーン明るい色（アルファをさらに薄くするか、白のオーバーレイにする）
                                            // ここではベース背景より明るい、白に近い不透明度高めの同系色をブレンド表現
                                            val innerCircleColor = if (isCompleted) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.45f)

                                            val hasIcon = !mainTag?.icon.isNullOrBlank()

                                            // ★ 修正：全体を1本に繋がった「1つの背景帯」にする
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(20.dp) // 高さを20dpに固定して無駄をなくす
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeBgColor) // ここで全体に背景を敷く
                                                    .padding(horizontal = 3.dp), // 帯の内側の余白
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                if (hasIcon) {
                                                    val iconEnum: TagIconId = TagIconId.fromId(mainTag?.icon)
                                                    val tagIcon = TagIconMapper.getVector(iconEnum)

                                                    // ★ 修正：背景とつながったまま、アイコンの裏に「白を足した色の丸」を重ねる
                                                    Box(
                                                        modifier = Modifier
                                                            .size(16.dp) // 帯に綺麗に収まるサイズ
                                                            .background(innerCircleColor, CircleShape), // ここで後ろに丸を配置
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = tagIcon,
                                                            contentDescription = null,
                                                            tint = contentColor,
                                                            modifier = Modifier.requiredSize(11.dp) // 丸の中に綺麗に収まるように調整
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }

                                                // テキスト部分（同じ背景帯の中にそのまま並べる）
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
                                                        .padding(bottom = 0.5.dp) // 上下中央に微調整
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

// ====================================================================================
// マスターマッピング構造定義群
// ====================================================================================

enum class TagIconId(val id: String) {
    FIRE("fire"),
    ERROR("error"),
    WARNING("warning"),
    FLAG("flag"),
    NOTIFICATION("notification"),
    OUTBOX("outbox"),
    ASSIGNMENT("assignment"),
    DONE("done"),
    BOOK("book"),
    BAG("bag"), // ★ 追加：バッグ
    SCHOOL("school"),
    PIN("pin"),
    LOCK("lock"),
    CALENDAR("calendar"),
    EDIT("edit"),
    HOURGLASS("hourglass"),
    STAR("star"),
    BULB("bulb"),
    BOOKMARK("bookmark"),
    SPARKLES("sparkles"),
    CANCEL("cancel");

    companion object {
        private val map = entries.associateBy { it.id }

        fun fromId(id: String?): TagIconId {
            if (id.isNullOrBlank()) return BOOKMARK
            val cleanId = id.substringAfter("Filled.")
                .replace("Outline", "")
                .lowercase()

            // ★ 修正：長い文字列の中に "bag" が含まれていたらカバン、"book" だけでかつ "bookmark" じゃないなら本にする
            return when {
                cleanId.contains("bag") -> BAG
                cleanId == "book" -> BOOK
                cleanId == "bookmark" -> BOOKMARK
                else -> map[cleanId] ?: BOOKMARK
            }
        }
    }
}

object TagIconMapper {
    fun getVector(id: TagIconId): ImageVector {
        return when (id) {
            TagIconId.FIRE -> Icons.Filled.LocalFireDepartment
            TagIconId.ERROR -> Icons.Filled.Error
            TagIconId.WARNING -> Icons.Filled.Warning
            TagIconId.FLAG -> Icons.Filled.Flag
            TagIconId.NOTIFICATION -> Icons.Filled.NotificationsActive
            TagIconId.OUTBOX -> Icons.Filled.Outbox
            TagIconId.ASSIGNMENT -> Icons.Filled.Assignment
            TagIconId.DONE -> Icons.Filled.AssignmentTurnedIn
            TagIconId.BOOK -> Icons.Filled.MenuBook
            TagIconId.BAG -> Icons.Filled.ShoppingBag // ★ カバンのベクターを割り当て
            TagIconId.SCHOOL -> Icons.Filled.School
            TagIconId.PIN -> Icons.Filled.PushPin
            TagIconId.LOCK -> Icons.Filled.Lock
            TagIconId.CALENDAR -> Icons.Filled.CalendarMonth
            TagIconId.EDIT -> Icons.Filled.EditCalendar
            TagIconId.HOURGLASS -> Icons.Filled.HourglassTop
            TagIconId.STAR -> Icons.Filled.Star
            TagIconId.BULB -> Icons.Filled.Lightbulb
            TagIconId.BOOKMARK -> Icons.Filled.Bookmark
            TagIconId.SPARKLES -> Icons.Filled.AutoAwesome
            TagIconId.CANCEL -> Icons.Filled.Cancel
        }
    }
}