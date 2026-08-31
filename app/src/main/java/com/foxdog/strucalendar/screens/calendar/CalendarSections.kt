package com.foxdog.strucalendar.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import java.util.Locale

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
    showWeekNumberColumn: Boolean = false,
    leadingWidth: Dp = 0.dp
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
            if (leadingWidth > 0.dp) {
                Box(modifier = Modifier.width(leadingWidth))
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
    onNavigateToTaskDetail: (Long) -> Unit = {},
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
            onNavigateToDateDetail = onNavigateToDateDetail,
            onNavigateToTaskDetail = onNavigateToTaskDetail
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
    onNavigateToTaskDetail: (Long) -> Unit,
    onUpdateWeekStart: (LocalDate) -> Unit,
    isTimetableMode: Boolean = false,
    onTogglePreviewMode: () -> Unit = {}
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

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val weekOffset = (page - basePage).toLong()
        val pageWeekStart = todayWeekStart.plusWeeks(weekOffset)
        val matrix = buildWeekCalendarMatrix(pageWeekStart)
        // ページ（週）ごとに実際のタスク内容から高さを計算する。
        // 表示中の週の値を全ページに使い回すと、日をまたぐタスクの本数が
        // 週によって異なる場合に、隣の週へスワイプした瞬間に必要な高さが
        // 足りず、バーが日付の数字に被って見えることがあったため修正。
        val gridHeight = remember(matrix, tasksByDate) {
            computeWeekGridHeight(matrix, tasksByDate, maxVisibleTasks = 20)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (isTimetableMode) {
                WeekTimetableView(
                    matrix = matrix,
                    tasksByDate = tasksByDate,
                    holidayMap = holidayMap,
                    today = today,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected,
                    onNavigateToTaskDetail = onNavigateToTaskDetail,
                    onNavigateToDateDetail = onNavigateToDateDetail,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
                    onNavigateToDateDetail = onNavigateToDateDetail,
                    onNavigateToTaskDetail = onNavigateToTaskDetail
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
}

@Composable
private fun DayCell(
    date: LocalDate,
    dayIndexInWeek: Int = 0,
    isSelected: Boolean,
    isToday: Boolean,
    holidayName: String? = null,
    dayTasks: List<TaskWithTags>,
    laneSlots: List<WeekTaskBar?> = emptyList(),
    laneOverflowCount: Int = 0,
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
            .drawBehind {
                val lineColor = colorScheme.outlineVariant.copy(alpha = 0.4f)
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(size.width - 0.5f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width - 0.5f, size.height),
                    strokeWidth = 1f
                )
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height - 0.5f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height - 0.5f),
                    strokeWidth = 1f
                )
            }
            .clickable { onClick() }
    ) {
        val topPadding = 4.dp
        val dateAreaHeight = 24.dp
        val spacerAfterDate = 2.dp
        val badgeHeight = 19.dp
        val badgeSpacing = 2.dp
        val overflowTextHeight = 13.dp
        val laneHeight = WEEK_BAR_LANE_HEIGHT
        val laneSpacing = WEEK_BAR_LANE_SPACING
        val laneAreaHeight = if (laneSlots.isEmpty()) 0.dp else {
            laneHeight * laneSlots.size + laneSpacing * (laneSlots.size - 1).coerceAtLeast(0)
        }
        val spacerAfterLanes = if (laneSlots.isEmpty()) 0.dp else spacerAfterDate

        val fixedHeaderHeight = topPadding + dateAreaHeight + spacerAfterDate + laneAreaHeight + spacerAfterLanes
        val availableForBadges = (maxHeight - fixedHeaderHeight).coerceAtLeast(0.dp)

        fun heightForCount(n: Int): Dp =
            if (n <= 0) 0.dp else badgeHeight * n + badgeSpacing * (n - 1)

        val holidaySlots = if (isHoliday) 1 else 0
        val totalItemsWanted = holidaySlots + dayTasks.size
        val totalOverflowWanted = laneOverflowCount

        var dynamicMaxSlots = 0
        while (
            dynamicMaxSlots < totalItemsWanted &&
            heightForCount(dynamicMaxSlots + 1) <= availableForBadges
        ) {
            dynamicMaxSlots++
        }

        if (dynamicMaxSlots < totalItemsWanted || totalOverflowWanted > 0) {
            while (
                dynamicMaxSlots > 0 &&
                heightForCount(dynamicMaxSlots) + badgeSpacing + overflowTextHeight > availableForBadges
            ) {
                dynamicMaxSlots--
            }
        }

        val cappedTotalSlots = dynamicMaxSlots.coerceAtMost(maxVisibleTasks + holidaySlots)
        val effectiveMaxVisibleTasks = (cappedTotalSlots - holidaySlots).coerceAtLeast(0)
        val totalOverflowCount = laneOverflowCount + (dayTasks.size - effectiveMaxVisibleTasks).coerceAtLeast(0)

        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                            fontSize = 13.sp,
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
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isHoliday || date.dayOfWeek.value % 7 == 0 -> calColors.sunday
                                date.dayOfWeek.value % 7 == 6 -> colorScheme.primary
                                else -> colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacerAfterDate))

            if (laneSlots.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(laneSpacing)
                ) {
                    laneSlots.forEach { bar ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(laneHeight)
                        ) {
                            if (bar != null) {
                                TaskBarChip(
                                    bar = bar,
                                    date = date,
                                    dayIndexInWeek = dayIndexInWeek,
                                    modifier = Modifier.fillMaxSize(),
                                    onClick = null
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(spacerAfterLanes))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(badgeSpacing)
            ) {
                if (holidayName != null && effectiveMaxVisibleTasks >= 0) {
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
                            fontSize = 9.sp,
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
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (hasIcon) colorScheme.onSurface else contentColor,
                            letterSpacing = (-0.3).sp,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 0.5.dp)
                        )
                    }
                }

                if (totalOverflowCount > 0) {
                    Text(
                        text = "+$totalOverflowCount",
                        fontSize = 9.sp,
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
// 複数日タスクの連結バー表示（Googleカレンダー方式）
// ============================================================

private data class WeekTaskBar(
    val item: TaskWithTags,
    val startCol: Int,
    val endCol: Int,
    val lane: Int,
    val continuesFromPrevWeek: Boolean,
    val continuesToNextWeek: Boolean,
    val realStart: LocalDate,
    val realEnd: LocalDate
)

/**
 * 週（7スロット、月表示の先頭・末尾週はnullを含む）に対して、
 * 実際のtask.startTime/endTimeから「開始列〜終了列」の区間を求め、
 * 区間が重ならないタスク同士を同じレーンに詰めて割り当てる。
 */
private fun computeWeekLanes(
    week: List<LocalDate?>,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>
): Pair<List<WeekTaskBar>, Int> {
    val weekDates = week.filterNotNull()
    if (weekDates.isEmpty()) return emptyList<WeekTaskBar>() to 0

    val weekStart = weekDates.first()
    val weekEnd = weekDates.last()
    val zone = ZoneId.systemDefault()

    val uniqueItems = weekDates
        .flatMap { tasksByDate[it].orEmpty() }
        .distinctBy { it.task.taskId }

    data class Raw(val item: TaskWithTags, val realStart: LocalDate, val realEnd: LocalDate)

    val raws = uniqueItems.mapNotNull { item ->
        val s = Instant.ofEpochSecond(item.task.startTime).atZone(zone).toLocalDate()
        val eRaw = Instant.ofEpochSecond(item.task.endTime).atZone(zone).toLocalDate()
        val e = if (eRaw.isBefore(s)) s else eRaw
        if (s == e) return@mapNotNull null
        if (e.isBefore(weekStart) || s.isAfter(weekEnd)) null else Raw(item, s, e)
    }

    val sorted = raws.sortedWith(
        compareBy(
            { week.indexOf(if (it.realStart.isBefore(weekStart)) weekStart else it.realStart) },
            { -ChronoUnit.DAYS.between(it.realStart, it.realEnd) }
        )
    )

    val laneEndCols = mutableListOf<Int>()
    val bars = mutableListOf<WeekTaskBar>()

    for (raw in sorted) {
        val clampedStart = if (raw.realStart.isBefore(weekStart)) weekStart else raw.realStart
        val clampedEnd = if (raw.realEnd.isAfter(weekEnd)) weekEnd else raw.realEnd
        val startCol = week.indexOf(clampedStart).coerceAtLeast(0)
        val endCol = week.indexOf(clampedEnd).let { if (it == -1) 6 else it }

        var laneIndex = laneEndCols.indexOfFirst { it < startCol }
        if (laneIndex == -1) {
            laneEndCols.add(endCol)
            laneIndex = laneEndCols.size - 1
        } else {
            laneEndCols[laneIndex] = endCol
        }

        bars.add(
            WeekTaskBar(
                item = raw.item,
                startCol = startCol,
                endCol = endCol,
                lane = laneIndex,
                continuesFromPrevWeek = raw.realStart.isBefore(weekStart),
                continuesToNextWeek = raw.realEnd.isAfter(weekEnd),
                realStart = raw.realStart,
                realEnd = raw.realEnd
            )
        )
    }

    return bars to laneEndCols.size
}

private fun barsForDay(
    bars: List<WeekTaskBar>,
    dayIndexInWeek: Int,
    maxLanesVisible: Int
): List<WeekTaskBar?> {
    if (maxLanesVisible <= 0) return emptyList()
    return (0 until maxLanesVisible).map { lane ->
        bars.firstOrNull { it.lane == lane && dayIndexInWeek in it.startCol..it.endCol }
    }
}

private fun laneOverflowCountForDay(
    bars: List<WeekTaskBar>,
    dayIndexInWeek: Int,
    maxLanesVisible: Int
): Int {
    if (maxLanesVisible <= 0) return bars.count { dayIndexInWeek in it.startCol..it.endCol }
    return bars.count { it.lane >= maxLanesVisible && dayIndexInWeek in it.startCol..it.endCol }
}

@Composable
private fun TaskBarChip(
    bar: WeekTaskBar,
    date: LocalDate,
    dayIndexInWeek: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val task = bar.item.task
    val isCompleted = task.completeState == "COMPLETED"
    val mainTag = bar.item.tags.firstOrNull()
    val baseColor = if (mainTag != null) Color(mainTag.color) else if (task.color == 0) colorScheme.primary else Color(task.color)
    val bg = if (isCompleted) colorScheme.surfaceVariant.copy(alpha = 0.4f) else baseColor.copy(alpha = 0.1f)
    val content = if (isCompleted) colorScheme.onSurfaceVariant else baseColor
    val hasIcon = !mainTag?.icon.isNullOrBlank()

    val continuesFromPrevDay = bar.realStart.isBefore(date)
    val continuesToNextDay = bar.realEnd.isAfter(date)
    val shape = RoundedCornerShape(
        topStart = if (continuesFromPrevDay) 0.dp else 6.dp,
        bottomStart = if (continuesFromPrevDay) 0.dp else 6.dp,
        topEnd = if (continuesToNextDay) 0.dp else 6.dp,
        bottomEnd = if (continuesToNextDay) 0.dp else 6.dp
    )

    Row(
        modifier = modifier
            .clip(shape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dayIndexInWeek == bar.startCol) {
            if (task.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = content,
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
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (hasIcon) colorScheme.onSurface else content,
                letterSpacing = (-0.3).sp,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 0.5.dp)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private val WEEK_BAR_LANE_HEIGHT = 19.dp
private val WEEK_BAR_LANE_SPACING = 2.dp

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
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit = {}
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
            val (weekBars, laneCount) = remember(week, tasksByDate) {
                computeWeekLanes(week, tasksByDate)
            }
            val maxLanes = laneCount.coerceAtMost(maxVisibleTasks)

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

                for ((dayIndex, date) in week.withIndex()) {
                    if (date != null) {
                        val isSelected = date == selectedDate
                        val isToday = date == today
                        val singleDayTasks = (tasksByDate[date] ?: emptyList())
                            .filter { task ->
                                val startDate = Instant.ofEpochSecond(task.task.startTime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                val endDate = Instant.ofEpochSecond(task.task.endTime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                startDate == endDate
                            }
                            .sortedForDayCell()

                        DayCell(
                            textSize = 10.sp,
                            date = date,
                            dayIndexInWeek = dayIndex,
                            isSelected = isSelected,
                            isToday = isToday,
                            holidayName = holidayMap[date],
                            dayTasks = singleDayTasks,
                            laneSlots = barsForDay(weekBars, dayIndex, maxLanes),
                            laneOverflowCount = laneOverflowCountForDay(weekBars, dayIndex, maxLanes),
                            maxVisibleTasks = maxVisibleTasks,
                            showTagColorOnCalendar = showTagColorOnCalendar,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = {
                                if (isSelected) onNavigateToDateDetail(date) else onDateSelected(date)
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(colorScheme.surface)
                        )
                    }
                }

                if (week.size < 7) {
                    repeat(7 - week.size) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(colorScheme.surface)
                        )
                    }
                }
            }
        }
    }
}

// 週表示：複数日タスクのレーン分だけDayCell内に表示領域を確保する。
private fun computeWeekGridHeight(
    matrix: List<LocalDate?>,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    maxVisibleTasks: Int
): Dp {
    val (_, laneCount) = computeWeekLanes(matrix, tasksByDate)
    val visibleLanes = laneCount.coerceAtMost(maxVisibleTasks)
    val laneHeight = if (visibleLanes <= 0) 0.dp else {
        WEEK_BAR_LANE_HEIGHT * visibleLanes + WEEK_BAR_LANE_SPACING * (visibleLanes - 1).coerceAtLeast(0)
    }

    val cellHeight = 40.dp
    val bottomPadding = 8.dp
    return (cellHeight + laneHeight + bottomPadding).coerceAtLeast(100.dp)
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
    modifier: Modifier = Modifier,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val (weekBars, laneCount) = remember(matrix, tasksByDate) {
        computeWeekLanes(matrix, tasksByDate)
    }
    val maxLanes = laneCount.coerceAtMost(maxVisibleTasks)

    Box(
        modifier = modifier
            .background(colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            for ((dayIndex, date) in matrix.withIndex()) {
                if (date != null) {
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val singleDayTasks = (tasksByDate[date] ?: emptyList())
                        .filter { task ->
                            val startDate = Instant.ofEpochSecond(task.task.startTime)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            val endDate = Instant.ofEpochSecond(task.task.endTime)
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            startDate == endDate
                        }
                        .sortedForDayCell()

                    DayCell(
                        textSize = 10.sp,
                        date = date,
                        dayIndexInWeek = dayIndex,
                        isSelected = isSelected,
                        isToday = isToday,
                        holidayName = holidayMap[date],
                        dayTasks = singleDayTasks,
                        laneSlots = barsForDay(weekBars, dayIndex, maxLanes),
                        laneOverflowCount = laneOverflowCountForDay(weekBars, dayIndex, maxLanes),
                        maxVisibleTasks = maxVisibleTasks,
                        showTagColorOnCalendar = showTagColorOnCalendar,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            if (isSelected) onNavigateToDateDetail(date) else onDateSelected(date)
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colorScheme.surface)
                    )
                }
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
    isTimetableMode: Boolean = false,
    onTogglePreviewMode: () -> Unit = {},
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
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToDateDetail(selectedDate) },
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty() && holidayName == null) {
            Text(
                text = "予定はありません",
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
        } else if (isTimetableMode) {
            SelectedDayTimetableView(
                tasks = tasks,
                holidayName = holidayName,
                onNavigateToTaskDetail = onNavigateToTaskDetail,
                onNavigateToDateDetail = { onNavigateToDateDetail(selectedDate) },
                modifier = Modifier.fillMaxSize()
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
                            shape = RoundedCornerShape(12.dp) // TaskCardに合わせて角丸を12.dpに変更
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

                // 共通化したTaskCardを使用
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

// ============================================================
// 週表示：選択日の時刻表レイアウト（0〜24時の時間軸にタスクを配置）
// 終日タスクは上部にまとめ、時間指定タスクのみ縦軸に配置する。
// ============================================================

private const val TIMETABLE_HOUR_HEIGHT_DP = 56
private const val TIMETABLE_START_HOUR = 0
private const val TIMETABLE_END_HOUR = 24

/**
 * 週表示・時刻表モード用：7列（曜日）×時間軸（縦）のグリッド。
 * 「上：ミニ週グリッド＋下：選択日プレビュー」の2段構成を丸ごと置き換える形で使う。
 * タスクブロックは簡略表示（タイトル一部＋タグの色/アイコンのみ、時刻テキストは軸で分かるため省略）。
 */
@Composable
private fun TimetableMultiDayBar(
    bar: WeekTaskBar,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val task = bar.item.task
    val mainTag = bar.item.tags.firstOrNull()
    val barColor = if (mainTag != null) Color(mainTag.color)
        else if (task.color != 0) Color(task.color)
        else colorScheme.primary

    val shape = RoundedCornerShape(
        topStart = if (bar.continuesFromPrevWeek) 0.dp else 6.dp,
        bottomStart = if (bar.continuesFromPrevWeek) 0.dp else 6.dp,
        topEnd = if (bar.continuesToNextWeek) 0.dp else 6.dp,
        bottomEnd = if (bar.continuesToNextWeek) 0.dp else 6.dp
    )

    Row(
        modifier = modifier
            .clip(shape)
            .background(barColor.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (task.isPinned) {
            Icon(
                imageVector = Icons.Default.PushPin,
                contentDescription = null,
                tint = colorScheme.onSurface,
                modifier = Modifier.size(9.dp)
            )
            Spacer(modifier = Modifier.width(1.dp))
        }
        if (mainTag != null && !mainTag.icon.isNullOrBlank()) {
            TagIconBadge(tag = mainTag, size = 14.dp, iconSize = 10.dp)
            Spacer(modifier = Modifier.width(2.dp))
        }
        Text(
            text = task.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colorScheme.onSurface,
            letterSpacing = (-0.3).sp
        )
    }
}

@Composable
private fun WeekTimetableView(
    matrix: List<LocalDate?>,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String>,
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // 終日欄には通常の終日タスクと日をまたぐタスクをまとめて表示する。
    // 日をまたぐタスクは週単位のレーンを使い、複数の日にまたがる1本のバーとして描画する。
    val allDayByDate = remember(matrix, tasksByDate) {
        matrix.filterNotNull().associateWith { date ->
            (tasksByDate[date] ?: emptyList())
                .filter { it.task.isAllDay }
                .sortedForDayCell()
        }
    }
    val (multiDayBars, multiDayLaneCount) = remember(matrix, tasksByDate) {
        computeWeekLanes(matrix, tasksByDate)
    }
    val maxVisibleAllDayTasks = 5
    val visibleMultiDayLanes = multiDayLaneCount.coerceAtMost(maxVisibleAllDayTasks)
    val visibleAllDaySlots = (maxVisibleAllDayTasks - visibleMultiDayLanes).coerceAtLeast(0)
    val allDayBarHeight = WEEK_BAR_LANE_HEIGHT
    val allDayBarSpacing = WEEK_BAR_LANE_SPACING
    val allDayRowHeight = allDayBarHeight + allDayBarSpacing
    val hasAllDayContent = allDayByDate.values.any { it.isNotEmpty() } || multiDayBars.isNotEmpty()
    val timetableScrollState = rememberScrollState()

    fun overflowCountForDate(date: LocalDate): Int {
        val dayIndex = matrix.indexOf(date)
        val multiDayOverflow = multiDayBars.count {
            it.lane >= maxVisibleAllDayTasks && dayIndex in it.startCol..it.endCol
        }
        val regularOverflow = (allDayByDate[date].orEmpty().size - visibleAllDaySlots).coerceAtLeast(0)
        return multiDayOverflow + regularOverflow
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ヘッダー：日付
        Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            Spacer(modifier = Modifier.width(40.dp))
            for (date in matrix) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (date == selectedDate) {
                                MaterialTheme.calendarColors.selectedDateBackground.copy(alpha = 0.4f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable {
                            if (date != null) {
                                if (date == selectedDate) onNavigateToDateDetail(date) else onDateSelected(date)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (date != null) {
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .then(
                                        if (isToday) Modifier.clip(CircleShape).background(colorScheme.primary)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) colorScheme.onPrimary else colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (hasAllDayContent) {
            HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.4f))
            val hasOverflow = matrix.filterNotNull().any { overflowCountForDate(it) > 0 }
            val requiredAllDayRows = matrix.filterNotNull().maxOfOrNull { date ->
                val regularVisibleCount = allDayByDate[date].orEmpty().size.coerceAtMost(visibleAllDaySlots)
                visibleMultiDayLanes + regularVisibleCount
            } ?: 0
            val allDayRows = requiredAllDayRows + if (hasOverflow) 1 else 0
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(allDayRowHeight * allDayRows + 6.dp)
            ) {
                val contentWidth = (maxWidth - 40.dp).coerceAtLeast(0.dp)
                val dayWidth = contentWidth / 7

                // 日をまたぐタスクは、カレンダー画面と同じ見た目のバーを
                // 該当する日付範囲に1本だけ配置する。
                multiDayBars
                    .filter { it.lane < maxVisibleAllDayTasks }
                    .forEach { bar ->
                        val barWidth = dayWidth * (bar.endCol - bar.startCol + 1)
                        val barX = 40.dp + dayWidth * bar.startCol
                        val barY = allDayRowHeight * bar.lane

                        TimetableMultiDayBar(
                            bar = bar,
                            modifier = Modifier
                                .offset(x = barX, y = barY)
                                .width(barWidth)
                                .height(allDayBarHeight),
                            onClick = { onNavigateToTaskDetail(bar.item.task.taskId) }
                        )
                    }

                // 通常の終日タスクは、日をまたぐバーの下に配置する。
                for ((dayIndex, date) in matrix.withIndex()) {
                    if (date == null) continue
                    val dayTasks = allDayByDate[date].orEmpty()
                    dayTasks
                        .take(visibleAllDaySlots)
                        .forEachIndexed { index, item ->
                            val row = visibleMultiDayLanes + index
                            val c = if (item.task.color != 0) Color(item.task.color) else colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = 40.dp + dayWidth * dayIndex + 1.dp,
                                        y = allDayRowHeight * row
                                    )
                                    .width((dayWidth - 2.dp).coerceAtLeast(0.dp))
                                    .height(allDayBarHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(c.copy(alpha = 0.25f))
                                    .clickable { onNavigateToTaskDetail(item.task.taskId) },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = item.task.title,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }

                    val overflow = overflowCountForDate(date)
                    if (overflow > 0) {
                        val row = allDayRows - 1
                        Text(
                            text = "+${overflow}件のタスク",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .offset(
                                    x = 40.dp + dayWidth * dayIndex + 3.dp,
                                    y = allDayRowHeight * row
                                )
                                .width((dayWidth - 6.dp).coerceAtLeast(0.dp))
                                .height(allDayBarHeight)
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))

        val hourHeight = TIMETABLE_HOUR_HEIGHT_DP.dp
        val totalHeight = hourHeight * (TIMETABLE_END_HOUR - TIMETABLE_START_HOUR)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val density = LocalDensity.current
            val viewportHeightPx = with(density) { maxHeight.roundToPx() }

            LaunchedEffect(viewportHeightPx) {
                if (viewportHeightPx > 0) {
                    val noonOffsetPx = with(density) { (hourHeight * (12 - TIMETABLE_START_HOUR)).roundToPx() }
                    val totalHeightPx = with(density) { totalHeight.roundToPx() }
                    val target = (noonOffsetPx - viewportHeightPx / 2)
                        .coerceIn(0, (totalHeightPx - viewportHeightPx).coerceAtLeast(0))
                    timetableScrollState.scrollTo(target)
                }
            }

            Box(modifier = Modifier.verticalScroll(timetableScrollState)) {
            Row(modifier = Modifier.fillMaxWidth().height(totalHeight)) {
                Column(modifier = Modifier.width(40.dp)) {
                    for (hour in TIMETABLE_START_HOUR until TIMETABLE_END_HOUR) {
                        Box(modifier = Modifier.height(hourHeight)) {
                            Text(
                                text = "%02d:00".format(hour),
                                fontSize = 9.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = (-6).dp)
                            )
                        }
                    }
                }

                for (date in matrix) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            for (hour in TIMETABLE_START_HOUR until TIMETABLE_END_HOUR) {
                                Box(modifier = Modifier.height(hourHeight).fillMaxWidth()) {
                                    HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.25f))
                                }
                            }
                        }

                        if (date != null) {
                            val timedTasks = (tasksByDate[date] ?: emptyList()).filter { item ->
                                if (item.task.isAllDay) return@filter false
                                val startDate = Instant.ofEpochSecond(item.task.startTime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                val endDate = Instant.ofEpochSecond(item.task.endTime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                startDate == endDate
                            }
                            timedTasks.forEach { item ->
                                val startLocalTime = LocalDateTimeOf(item.task.startTime)
                                val endLocalTime = LocalDateTimeOf(item.task.endTime)

                                val startMin = (startLocalTime.hour * 60 + startLocalTime.minute)
                                    .coerceIn(0, TIMETABLE_END_HOUR * 60)
                                var endMin = (endLocalTime.hour * 60 + endLocalTime.minute)
                                    .coerceIn(0, TIMETABLE_END_HOUR * 60)
                                if (endMin <= startMin) {
                                    endMin = (startMin + 30).coerceAtMost(TIMETABLE_END_HOUR * 60)
                                }

                                val topDp = hourHeight * startMin / 60
                                val heightDp = (hourHeight * (endMin - startMin) / 60).coerceAtLeast(18.dp)
                                val canWrapTitle = heightDp >= 40.dp

                                val mainTag = item.tags.firstOrNull()
                                val barColor = if (mainTag != null) Color(mainTag.color)
                                    else if (item.task.color != 0) Color(item.task.color)
                                    else colorScheme.primary

                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 1.dp)
                                        .offset(y = topDp)
                                        .height(heightDp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(barColor.copy(alpha = 0.22f))
                                        .clickable { onNavigateToTaskDetail(item.task.taskId) }
                                        .padding(horizontal = 3.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (mainTag != null && !mainTag.icon.isNullOrBlank()) {
                                        TagIconBadge(tag = mainTag, size = 10.dp, iconSize = 7.dp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = item.task.title,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = if (canWrapTitle) 2 else 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colorScheme.onSurface
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

@Composable
private fun SelectedDayTimetableView(
    tasks: List<TaskWithTags>,
    holidayName: String?,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToDateDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    val allDayTasks = remember(tasks) { tasks.filter { it.task.isAllDay } }
    val timedTasks = remember(tasks) {
        tasks.filter { !it.task.isAllDay }
            .sortedWith(compareBy({ LocalDateTimeOf(it.task.startTime) }, { LocalDateTimeOf(it.task.endTime) }))
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // 終日タスク・休日は上部に別枠でまとめる
        if (holidayName != null || allDayTasks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (holidayName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { onNavigateToDateDetail() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("終日", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = holidayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = calColors.sunday,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                allDayTasks.forEach { item ->
                    val taskColor = if (item.task.color != 0) Color(item.task.color) else colorScheme.primary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(taskColor.copy(alpha = 0.15f))
                            .clickable { onNavigateToTaskDetail(item.task.taskId) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(taskColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.task.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 時間指定タスクの時刻表本体
        val hourHeight = TIMETABLE_HOUR_HEIGHT_DP.dp
        val totalHeight = hourHeight * (TIMETABLE_END_HOUR - TIMETABLE_START_HOUR)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
        ) {
            // 時刻の目盛りと区切り線
            Column(modifier = Modifier.fillMaxSize()) {
                for (hour in TIMETABLE_START_HOUR until TIMETABLE_END_HOUR) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeight)
                    ) {
                        Box(modifier = Modifier.width(40.dp)) {
                            Text(
                                text = "%02d:00".format(hour),
                                fontSize = 10.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(y = (-6).dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            HorizontalDivider(thickness = 1.dp, color = colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            // タスクブロック（時間軸に沿って絶対配置）
            timedTasks.forEach { item ->
                val startLocalTime = LocalDateTimeOf(item.task.startTime)
                val endLocalTime = LocalDateTimeOf(item.task.endTime)

                val startMinutesFromZero = (startLocalTime.hour * 60 + startLocalTime.minute)
                    .coerceIn(0, TIMETABLE_END_HOUR * 60)
                var endMinutesFromZero = (endLocalTime.hour * 60 + endLocalTime.minute)
                    .coerceIn(0, TIMETABLE_END_HOUR * 60)
                // 終了時刻が開始時刻以前（日をまたぐ等）の場合は最低30分の見た目の高さを確保
                if (endMinutesFromZero <= startMinutesFromZero) {
                    endMinutesFromZero = (startMinutesFromZero + 30).coerceAtMost(TIMETABLE_END_HOUR * 60)
                }

                val topDp = hourHeight * startMinutesFromZero / 60
                val heightDp = (hourHeight * (endMinutesFromZero - startMinutesFromZero) / 60)
                    .coerceAtLeast(24.dp)

                val taskColor = if (item.task.color != 0) Color(item.task.color) else colorScheme.primary

                Column(
                    modifier = Modifier
                        .padding(start = 44.dp, end = 4.dp)
                        .offset(y = topDp)
                        .height(heightDp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(taskColor.copy(alpha = 0.18f))
                        .border(BorderStroke(1.dp, taskColor.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
                        .clickable { onNavigateToTaskDetail(item.task.taskId) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.task.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (heightDp > 36.dp) {
                        Text(
                            text = "${timetableTimeText(startLocalTime)} - ${timetableTimeText(endLocalTime)}",
                            fontSize = 10.sp,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (timedTasks.isEmpty()) {
                Text(
                    text = "時間指定の予定はありません",
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 44.dp, top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun timetableTimeText(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

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