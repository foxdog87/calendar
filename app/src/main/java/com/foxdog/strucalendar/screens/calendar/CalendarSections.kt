package com.foxdog.strucalendar.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PushPin
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.TagIconBadge
import com.foxdog.strucalendar.components.TagLabel
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.ui.components.TaskCard
import com.foxdog.strucalendar.ui.theme.calendarColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// ============================================================
// 並び替え共通ヘルパー：ピン止め優先→開始時刻順
// ============================================================

private fun List<TaskWithTags>.sortedForDayCell(): List<TaskWithTags> {
    return sortedWith(compareByDescending<TaskWithTags> { it.task.isPinned }.thenBy { it.task.startTime })
}

// ============================================================
// 曜日ヘッダー（月・週表示で共通）
// ============================================================

@Composable
internal fun WeekdayHeaderRow(
    weekStartDay: java.time.DayOfWeek,
    showWeekNumberColumn: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            if (showWeekNumberColumn) {
                Box(modifier = Modifier.width(28.dp))
            }

            val orderedDays = (0..6).map { offset ->
                java.time.DayOfWeek.of(((weekStartDay.value - 1 + offset) % 7) + 1)
            }
            val dayLabels = mapOf(
                java.time.DayOfWeek.SUNDAY to "日",
                java.time.DayOfWeek.MONDAY to "月",
                java.time.DayOfWeek.TUESDAY to "火",
                java.time.DayOfWeek.WEDNESDAY to "水",
                java.time.DayOfWeek.THURSDAY to "木",
                java.time.DayOfWeek.FRIDAY to "金",
                java.time.DayOfWeek.SATURDAY to "土"
            )

            orderedDays.forEach { day ->
                Text(
                    text = dayLabels[day] ?: "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (day) {
                        java.time.DayOfWeek.SUNDAY -> calColors.sunday.copy(alpha = 0.8f)
                        java.time.DayOfWeek.SATURDAY -> colorScheme.primary.copy(alpha = 0.8f)
                        else -> colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

// ============================================================
// 月表示：既存のPagerロジックをそのまま維持
// ============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MonthPagerView(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    showTagColorOnCalendar: Boolean,
    showWeekNumber: Boolean,
    weekNumberOf: (LocalDate) -> Int,
    buildCalendarMatrix: (YearMonth) -> List<LocalDate?>,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onUpdateYearMonth: (Int, Int) -> Unit
) {
    val maxMonths = 2400
    val basePage = maxMonths / 2
    val todayMonth = remember { YearMonth.now() }

    fun pageForMonth(month: YearMonth): Int {
        val offset = ChronoUnit.MONTHS.between(todayMonth, month).toInt()
        return (basePage + offset).coerceIn(0, maxMonths - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = pageForMonth(currentMonth),
        pageCount = { maxMonths }
    )

    var isSyncingFromExternal by remember { mutableStateOf(false) }

    LaunchedEffect(currentMonth) {
        val expectedPage = pageForMonth(currentMonth)
        if (pagerState.currentPage != expectedPage) {
            isSyncingFromExternal = true
            pagerState.animateScrollToPage(expectedPage)
            isSyncingFromExternal = false
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isSyncingFromExternal) return@LaunchedEffect
        val monthOffset = (pagerState.currentPage - basePage).toLong()
        val targetMonth = todayMonth.plusMonths(monthOffset)
        if (targetMonth != currentMonth) {
            onUpdateYearMonth(targetMonth.year, targetMonth.monthValue)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) { page ->
        val monthOffset = (page - basePage).toLong()
        val pageMonth = todayMonth.plusMonths(monthOffset)
        val matrix = buildCalendarMatrix(pageMonth)

        CalendarGridBody(
            matrix = matrix,
            selectedDate = selectedDate,
            today = today,
            tasksByDate = tasksByDate,
            holidayMap = holidayMap,
            showTagColorOnCalendar = showTagColorOnCalendar,
            showWeekNumber = showWeekNumber,
            weekNumberOf = weekNumberOf,
            onDateSelected = onDateSelected,
            onNavigateToDateDetail = onNavigateToDateDetail
        )
    }
}

// ============================================================
// 週表示：月表示と同じ「ViewModelの状態→Pager追従」パターン
// ============================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WeekPagerView(
    currentWeekStart: LocalDate,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    showTagColorOnCalendar: Boolean,
    buildWeekCalendarMatrix: (LocalDate) -> List<LocalDate?>,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit, // ★ 追加
    onUpdateWeekStart: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val maxWeeks = 5200
    val basePage = maxWeeks / 2
    val todayWeekStart = remember { today.minusDays((today.dayOfWeek.value % 7).toLong()) }

    fun pageForWeekStart(weekStart: LocalDate): Int {
        val offset = ChronoUnit.WEEKS.between(todayWeekStart, weekStart).toInt()
        return (basePage + offset).coerceIn(0, maxWeeks - 1)
    }

    val pagerState = rememberPagerState(
        initialPage = pageForWeekStart(currentWeekStart),
        pageCount = { maxWeeks }
    )

    var isSyncingFromExternal by remember { mutableStateOf(false) }

    LaunchedEffect(currentWeekStart) {
        val expectedPage = pageForWeekStart(currentWeekStart)
        if (pagerState.currentPage != expectedPage) {
            isSyncingFromExternal = true
            pagerState.animateScrollToPage(expectedPage)
            isSyncingFromExternal = false
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isSyncingFromExternal) return@LaunchedEffect
        val weekOffset = (pagerState.currentPage - basePage).toLong()
        val targetWeekStart = todayWeekStart.plusWeeks(weekOffset)
        if (targetWeekStart != currentWeekStart) {
            onUpdateWeekStart(targetWeekStart)
        }
    }

    val currentWeekMatrix = remember(currentWeekStart, tasksByDate) {
        buildWeekCalendarMatrix(currentWeekStart)
    }
    val gridHeight = computeWeekGridHeight(currentWeekMatrix, tasksByDate, maxVisibleTasks = 20)

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val weekOffset = (page - basePage).toLong()
        val pageWeekStart = todayWeekStart.plusWeeks(weekOffset)
        val matrix = buildWeekCalendarMatrix(pageWeekStart)

        Column(modifier = Modifier.fillMaxSize()) {
            WeekGridBody(
                matrix = matrix,
                selectedDate = selectedDate,
                today = today,
                tasksByDate = tasksByDate,
                holidayMap = holidayMap,
                maxVisibleTasks = 20,
                showTagColorOnCalendar = showTagColorOnCalendar,
                modifier = Modifier.fillMaxWidth().height(gridHeight),
                onDateSelected = onDateSelected,
                onNavigateToDateDetail = onNavigateToDateDetail
            )

            HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))

            SelectedDayTaskPreview(
                selectedDate = selectedDate,
                tasks = (tasksByDate[selectedDate] ?: emptyList()).sortedForDayCell(),
                holidayName = holidayMap[selectedDate],
                onNavigateToDateDetail = onNavigateToDateDetail,
                onNavigateToTaskDetail = onNavigateToTaskDetail,
                onToggleTaskCompletion = onToggleTaskCompletion,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    holidayName: String? = null,
    dayTasks: List<TaskWithTags>,
    maxVisibleTasks: Int,
    showTagColorOnCalendar: Boolean = true,
    textSize: TextUnit = 12.sp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors
    val isHoliday = holidayName != null

    BoxWithConstraints(
        modifier = modifier
            .background(if (isSelected) calColors.selectedDateBackground.copy(alpha = 0.4f) else colorScheme.surface)
            .clickable { onClick() }
    ) {
        val topPadding = 4.dp
        val dateAreaHeight = 24.dp
        val spacerAfterDate = 2.dp
        val badgeHeight = 18.dp
        val badgeSpacing = 2.dp
        val overflowTextHeight = 12.dp

        val fixedHeaderHeight = topPadding + dateAreaHeight + spacerAfterDate
        val availableForBadges = (maxHeight - fixedHeaderHeight).coerceAtLeast(0.dp)

        fun heightForCount(n: Int): Dp =
            if (n <= 0) 0.dp else badgeHeight * n + badgeSpacing * (n - 1)

        val holidaySlots = if (isHoliday) 1 else 0
        val totalItemsWanted = holidaySlots + dayTasks.size

        var dynamicMaxSlots = 0
        while (dynamicMaxSlots < totalItemsWanted && heightForCount(dynamicMaxSlots + 1) <= availableForBadges) {
            dynamicMaxSlots++
        }

        if (dynamicMaxSlots < totalItemsWanted) {
            while (
                dynamicMaxSlots > 0 &&
                heightForCount(dynamicMaxSlots) + badgeSpacing + overflowTextHeight > availableForBadges
            ) {
                dynamicMaxSlots--
            }
        }

        val cappedTotalSlots = dynamicMaxSlots.coerceAtMost(maxVisibleTasks + holidaySlots)
        val effectiveMaxVisibleTasks = (cappedTotalSlots - holidaySlots).coerceAtLeast(0)

        Column(
            modifier = Modifier
                .padding(top = topPadding, start = 2.dp, end = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isToday) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onPrimary
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isHoliday || date.dayOfWeek.value % 7 == 0 -> calColors.sunday
                            date.dayOfWeek.value % 7 == 6 -> colorScheme.primary
                            else -> colorScheme.onSurface
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacerAfterDate))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(badgeSpacing)
            ) {
                if (holidayName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(badgeHeight)
                            .clip(RoundedCornerShape(6.dp))
                            .background(calColors.sunday.copy(alpha = 0.08f))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = holidayName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = calColors.sunday,
                            letterSpacing = (-0.4).sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 0.5.dp)
                        )
                    }
                }

                dayTasks.take(effectiveMaxVisibleTasks).forEach { item ->
                    val task = item.task
                    val isCompleted = task.completeState == "COMPLETED"
                    val mainTag = item.tags.firstOrNull()

                    val baseColor = if (showTagColorOnCalendar && mainTag != null) {
                        Color(mainTag.color)
                    } else {
                        if (task.color == 0) colorScheme.primary else Color(task.color)
                    }

                    val badgeBgColor = if (isCompleted) colorScheme.surfaceVariant.copy(alpha = 0.4f) else baseColor.copy(alpha = 0.1f)
                    val contentColor = if (isCompleted) colorScheme.onSurfaceVariant else baseColor
                    val hasIcon = showTagColorOnCalendar && !mainTag?.icon.isNullOrBlank()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(badgeHeight)
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBgColor)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (task.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (hasIcon && mainTag != null) {
                            TagIconBadge(tag = mainTag, size = 14.dp, iconSize = 10.dp)
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        Text(
                            text = task.title,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (hasIcon) colorScheme.onSurface else contentColor,
                            letterSpacing = (-0.4).sp,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 0.5.dp)
                        )
                    }
                }

                if (dayTasks.size > effectiveMaxVisibleTasks) {
                    Text(
                        text = "+${dayTasks.size - effectiveMaxVisibleTasks}",
                        fontSize = 8.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ============================================================
// 月・週表示で共通のグリッド本体
// ============================================================

@Composable
private fun CalendarGridBody(
    matrix: List<LocalDate?>,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    maxVisibleTasks: Int = 20,
    showTagColorOnCalendar: Boolean = true,
    showWeekNumber: Boolean = false,
    weekNumberOf: (LocalDate) -> Int = { 0 },
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val chunkedWeeks = matrix.chunked(7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.outlineVariant.copy(alpha = 0.4f)),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (week in chunkedWeeks) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                if (showWeekNumber) {
                    val firstDateInWeek = week.firstOrNull { it != null }
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .background(colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        if (firstDateInWeek != null) {
                            Text(
                                text = weekNumberOf(firstDateInWeek).toString(),
                                fontSize = 10.sp,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                for (date in week) {
                    if (date != null) {
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val dayTasks = (tasksByDate[date] ?: emptyList()).sortedForDayCell()

                        DayCell(
                            textSize = 10.sp,
                            date = date,
                            isSelected = isSelected,
                            isToday = isToday,
                            holidayName = holidayMap[date],
                            dayTasks = dayTasks,
                            maxVisibleTasks = maxVisibleTasks,
                            showTagColorOnCalendar = showTagColorOnCalendar,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                if (isSelected) onNavigateToDateDetail(date) else onDateSelected(date)
                            }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colorScheme.surface))
                    }
                }

                if (week.size < 7) {
                    for (i in 0 until (7 - week.size)) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colorScheme.surface))
                    }
                }
            }
        }
    }
}

// 週表示：グリッドの高さを計算する関数
private fun computeWeekGridHeight(
    matrix: List<LocalDate?>,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    maxVisibleTasks: Int
): Dp {
    var maxCount = 0
    var anyOverflow = false
    for (date in matrix) {
        if (date == null) continue
        val total = tasksByDate[date]?.size ?: 0
        val visible = minOf(total, maxVisibleTasks)
        if (visible > maxCount) maxCount = visible
        if (total > maxVisibleTasks) anyOverflow = true
    }

    val topPadding = 6.dp
    val dateCircleSize = 24.dp
    val spacerAfterDate = 4.dp
    val badgeHeight = 20.dp
    val badgeSpacing = 3.dp
    val overflowTextHeight = 14.dp
    val bottomPadding = 8.dp

    val badgesHeight = if (maxCount > 0) {
        badgeHeight * maxCount + badgeSpacing * (maxCount - 1).coerceAtLeast(0)
    } else 0.dp

    val overflowHeight = if (anyOverflow) overflowTextHeight else 0.dp

    return (topPadding + dateCircleSize + spacerAfterDate + badgesHeight + overflowHeight + bottomPadding)
        .coerceAtLeast(100.dp)
}

@Composable
private fun WeekGridBody(
    matrix: List<LocalDate?>,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    maxVisibleTasks: Int = 20,
    showTagColorOnCalendar: Boolean = true,
    modifier: Modifier = Modifier, // ★ 追加：呼び出し側から高さ等を指定できるように
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .background(colorScheme.outlineVariant.copy(alpha = 0.4f)),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        for (date in matrix) {
            if (date != null) {
                val isSelected = date == selectedDate
                val isToday = date == today
                val dayTasks = (tasksByDate[date] ?: emptyList()).sortedForDayCell()

                DayCell(
                    date = date,
                    isSelected = isSelected,
                    isToday = isToday,
                    holidayName = holidayMap[date],
                    dayTasks = dayTasks,
                    maxVisibleTasks = maxVisibleTasks,
                    showTagColorOnCalendar = showTagColorOnCalendar,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = {
                        if (isSelected) onNavigateToDateDetail(date) else onDateSelected(date)
                    }
                )
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(colorScheme.surface))
            }
        }
    }
}

@Composable
private fun SelectedDayTaskPreview(
    selectedDate: LocalDate,
    tasks: List<TaskWithTags>,
    holidayName: String? = null,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    // TaskCardに渡すフォーマッター
    val dateFormatter = remember { DateTimeFormatter.ofPattern("M/d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(
        modifier = modifier
            .background(colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // 見出し
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToDateDetail(selectedDate) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日の予定",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty() && holidayName == null) {
            Text(
                text = "予定はありません",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 休日の表示
                if (holidayName != null) {
                    item(key = "holiday") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDateDetail(selectedDate) },
                            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp) // ★ TaskCardに合わせて角丸を12.dpに変更
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(text = "終日", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = holidayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = calColors.sunday,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // ★ 共通化したTaskCardを使用！
                items(tasks, key = { it.task.taskId }) { item ->
                    TaskCard(
                        item = item,
                        dateFormatter = dateFormatter,
                        timeFormatter = timeFormatter,
                        onClick = { onNavigateToTaskDetail(item.task.taskId) },
                        onToggleTaskCompletion = onToggleTaskCompletion
                    )
                }
            }
        }
    }
}

// epoch秒 → LocalTime変換の簡易ヘルパー
private fun LocalDateTimeOf(epochSecond: Long): LocalTime {
    return java.time.LocalDateTime.ofInstant(
        Instant.ofEpochSecond(epochSecond),
        ZoneId.systemDefault()
    ).toLocalTime()
}

// ============================================================
// 年表示：12ヶ月分のミニカレンダーを縦スクロール表示
// ============================================================

@Composable
internal fun YearGridView(
    year: Int,
    focusMonth: Int,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    weekStartDay: java.time.DayOfWeek,
    showTagColorOnCalendar: Boolean,
    buildYearMatrix: (Int) -> List<YearMonth>,
    buildCalendarMatrix: (YearMonth) -> List<LocalDate?>,
    onDateSelected: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val months = buildYearMatrix(year)
    val listState = rememberLazyListState()

    LaunchedEffect(year, focusMonth) {
        val targetIndex = (focusMonth - 1).coerceIn(0, months.lastIndex)

        listState.scrollToItem(targetIndex)

        val layoutInfo = listState.layoutInfo
        val viewportHeight = layoutInfo.viewportSize.height
        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }

        if (itemInfo != null && viewportHeight > 0) {
            val centerOffset = (viewportHeight - itemInfo.size) / 2
            if (centerOffset > 0) {
                listState.scrollToItem(targetIndex, scrollOffset = -centerOffset)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(months) { yearMonth ->
            MiniMonthCalendar(
                yearMonth = yearMonth,
                selectedDate = selectedDate,
                today = today,
                tasksByDate = tasksByDate,
                holidayMap = holidayMap,
                weekStartDay = weekStartDay,
                showTagColorOnCalendar = showTagColorOnCalendar,
                matrix = buildCalendarMatrix(yearMonth),
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun MiniMonthCalendar(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    weekStartDay: java.time.DayOfWeek,
    showTagColorOnCalendar: Boolean,
    matrix: List<LocalDate?>,
    onDateSelected: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "${yearMonth.monthValue}月",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val weeks = matrix.chunked(7)

            val dayLabels = mapOf(
                java.time.DayOfWeek.SUNDAY to "日",
                java.time.DayOfWeek.MONDAY to "月",
                java.time.DayOfWeek.TUESDAY to "火",
                java.time.DayOfWeek.WEDNESDAY to "水",
                java.time.DayOfWeek.THURSDAY to "木",
                java.time.DayOfWeek.FRIDAY to "金",
                java.time.DayOfWeek.SATURDAY to "土"
            )
            val orderedDays = (0..6).map { offset ->
                java.time.DayOfWeek.of(((weekStartDay.value - 1 + offset) % 7) + 1)
            }

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surface)
                        .padding(vertical = 6.dp)
                ) {
                    orderedDays.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabels[day] ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (day) {
                                    java.time.DayOfWeek.SUNDAY -> calColors.sunday.copy(alpha = 0.8f)
                                    java.time.DayOfWeek.SATURDAY -> colorScheme.primary.copy(alpha = 0.8f)
                                    else -> colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.4f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (week in weeks) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            for (date in week) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .background(colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (date != null) {
                                        val isToday = date == today
                                        val isHoliday = holidayMap.containsKey(date)
                                        val dayTasks = (tasksByDate[date] ?: emptyList())
                                            .sortedForDayCell()
                                            .take(4)

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isToday -> colorScheme.primary
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .clickable { onDateSelected(date) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = date.dayOfMonth.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isToday -> colorScheme.onPrimary
                                                        isHoliday || date.dayOfWeek.value % 7 == 0 -> calColors.sunday
                                                        date.dayOfWeek.value % 7 == 6 -> colorScheme.primary
                                                        else -> colorScheme.onSurface
                                                    }
                                                )
                                            }

                                            if (dayTasks.isNotEmpty()) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    dayTasks.forEach { item ->
                                                        val mainTag = item.tags.firstOrNull()
                                                        val badgeColor = when {
                                                            showTagColorOnCalendar && mainTag != null -> Color(mainTag.color)
                                                            item.task.color != 0 -> Color(item.task.color)
                                                            else -> colorScheme.primary
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(RoundedCornerShape(1.5.dp))
                                                                .background(badgeColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}