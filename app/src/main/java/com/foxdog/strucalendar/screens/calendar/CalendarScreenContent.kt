package com.foxdog.strucalendar.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.MonthYearPickerDialog
import com.foxdog.strucalendar.components.TagCreateDialog
import com.foxdog.strucalendar.components.TagIconId
import com.foxdog.strucalendar.components.TagIconSource
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.ui.bounceClick
import com.foxdog.strucalendar.viewmodel.CalendarDisplayMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.foxdog.strucalendar.components.SpotlightOnboardingOverlay
import com.foxdog.strucalendar.components.SpotlightShape
import com.foxdog.strucalendar.components.SpotlightStep
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot



// Preview や テストのためにロジックを分離したUIコンポーネント
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreenContent(
    currentMonth: YearMonth,
    currentWeekStart: LocalDate = LocalDate.now(),
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate, List<TaskWithTags>>,
    holidayMap: Map<LocalDate, String> = emptyMap(),
    displayMode: CalendarDisplayMode = CalendarDisplayMode.MONTH,
    showDisplayModeMenu: Boolean = false,
    weekStartDay: java.time.DayOfWeek = java.time.DayOfWeek.SUNDAY,
    showTagColorOnCalendar: Boolean = true,
    showWeekNumber: Boolean = false,
    buildCalendarMatrix: (YearMonth) -> List<LocalDate?>,
    buildWeekCalendarMatrix: (LocalDate) -> List<LocalDate?> = { emptyList() },
    buildYearMatrix: (Int) -> List<YearMonth> = { emptyList() },
    weekNumberOf: (LocalDate) -> Int = { 0 },
    onDateSelected: (LocalDate) -> Unit,
    onDateSelectedFromYearView: (LocalDate) -> Unit = onDateSelected,
    onPreviousPeriod: () -> Unit = {},
    onNextPeriod: () -> Unit = {},
    onUpdateYearMonth: (Int, Int) -> Unit,
    onUpdateWeekStartFromYearMonth: (Int, Int) -> Unit = { _, _ -> },
    onToggleTaskCompletion: (TaskWithTags) -> Unit = {},
    onUpdateWeekStart: (LocalDate) -> Unit = {},
    onDisplayModeMenuButtonClick: () -> Unit = {},
    onDismissDisplayModeMenu: () -> Unit = {},
    onSelectDisplayMode: (CalendarDisplayMode) -> Unit = {},
    onNavigateToTaskCreate: (LocalDate) -> Unit,
    onNavigateToTaskList: () -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    showOnboarding: Boolean = false,
    onOnboardingFinished: () -> Unit = {},
    showAllTutorialsCompletedDialog: Boolean = false,
    onDismissAllTutorialsCompletedDialog: () -> Unit = {},
    allTags: List<Tag> = emptyList(),
    selectedFilterTagIds: Set<Long> = emptySet(),
    onToggleFilterTag: (Long) -> Unit = {},
    onResetTagFilter: () -> Unit = {},
    filterIsAndSearch: Boolean = false,
    onSetFilterIsAndSearch: (Boolean) -> Unit = {},
    onDeleteTag: (Tag) -> Unit = {},
    onCreateTag: (Tag, List<String>) -> Unit = { _, _ -> },
    onUpdateTag: (Tag, List<String>) -> Unit = { _, _ -> },
    onLoadCustomFieldsForTag: suspend (Long) -> List<String> = { emptyList() },
    onUpdateTagOrder: (List<Tag>) -> Unit = {},
    confirmDiscardChanges: Boolean = true,
    weekDayPreviewIsTimetable: Boolean = false,
    onToggleWeekDayPreviewMode: () -> Unit = {},
) {
    var showMonthYearDialog by remember { mutableStateOf(false) }
    var showTagFilterDialog by remember { mutableStateOf(false) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var showTagCreateDialog by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<Tag?>(null) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    // 週表示モードで年月確定後に開く「週選択」ダイアログの状態
    var weekSelectionMonth by remember { mutableStateOf<YearMonth?>(null) }

    val targetRects = remember { mutableStateMapOf<String, Rect>() }
    var onboardingDismissed by remember { mutableStateOf(false) } // ステップindexの管理は不要になった

    val onboardingSteps = remember {
        listOf(
            SpotlightStep(
                targetKey = "add_button",
                title = "予定を追加",
                description = "このボタンから新しい予定・タスクを作成できます。",
                shape = SpotlightShape.PILL,
                highlightPadding = 8.dp
            ),
            SpotlightStep(
                targetKey = "display_mode_button",
                title = "表示を切り替える",
                description = "ここをタップすると、カレンダーの表示を「年」「月」「週」に変更できます。"
            ),
            SpotlightStep(
                targetKey = "year_month_label",
                title = "年月をタップして移動",
                description = "ここをタップすると、好きな年月に一気にジャンプできます。"
            ),
            SpotlightStep(
                targetKey = "prev_next_buttons",
                title = "カレンダーを移動・絞り込む",
                description = "右上のタグボタンから、表示する予定をタグで絞り込めます。◀▶ボタンのほか、画面を左右にスワイプして月・週を移動することもできます。"
            )
        )
    }

    val today = LocalDate.now()
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned {
                                targetRects["display_mode_button"] = it.boundsInRoot()
                            }
                    ) {
                        IconButton(onClick = onDisplayModeMenuButtonClick) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "表示切り替え",
                                tint = colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showDisplayModeMenu,
                            onDismissRequest = onDismissDisplayModeMenu
                        ) {
                            DropdownMenuItem(
                                text = { Text("年") },
                                onClick = { onSelectDisplayMode(CalendarDisplayMode.YEAR) }
                            )
                            DropdownMenuItem(
                                text = { Text("月") },
                                onClick = { onSelectDisplayMode(CalendarDisplayMode.MONTH) }
                            )
                            DropdownMenuItem(
                                text = { Text("週") },
                                onClick = { onSelectDisplayMode(CalendarDisplayMode.WEEK) }
                            )
                        }
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
                                .weight(1f, fill = false)
                                .clickable { showMonthYearDialog = true }
                                .padding(vertical = 4.dp)
                                .onGloballyPositioned {
                                    targetRects["year_month_label"] = it.boundsInRoot()
                                }
                        ) {
                            val titleText = when (displayMode) {
                                CalendarDisplayMode.YEAR -> "${currentMonth.year}年"
                                CalendarDisplayMode.MONTH -> "${currentMonth.year}年 ${currentMonth.monthValue}月"
                                CalendarDisplayMode.WEEK -> {
                                    val weekEnd = currentWeekStart.plusDays(6)
                                    "${currentWeekStart.year}年 ${currentWeekStart.monthValue}/${currentWeekStart.dayOfMonth} 〜 ${weekEnd.monthValue}/${weekEnd.dayOfMonth}"
                                }
                            }

                            Text(
                                text = titleText,
                                fontSize = if (displayMode == CalendarDisplayMode.WEEK) 15.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.onGloballyPositioned {
                                targetRects["prev_next_buttons"] = it.boundsInRoot()
                            }
                        ) {
                            IconButton(onClick = { showTagFilterDialog = true }, modifier = Modifier.size(40.dp)) {
                                BadgedBox(
                                    badge = {
                                        if (selectedFilterTagIds.isNotEmpty()) {
                                            Badge {
                                                Text(selectedFilterTagIds.size.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Label,
                                        contentDescription = "タグで絞り込む",
                                        tint = if (selectedFilterTagIds.isNotEmpty()) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (displayMode == CalendarDisplayMode.WEEK) {
                                IconButton(onClick = onToggleWeekDayPreviewMode, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        imageVector = if (weekDayPreviewIsTimetable) Icons.Default.ViewList else Icons.Default.Schedule,
                                        contentDescription = if (weekDayPreviewIsTimetable) "リスト表示に切り替え" else "時刻表表示に切り替え",
                                        tint = if (weekDayPreviewIsTimetable) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(onClick = onPreviousPeriod, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "前へ",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            IconButton(onClick = onNextPeriod, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "次へ",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                shadowElevation = 8.dp,
                color = colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick { onNavigateToTaskList() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                            Text("一覧", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onNavigateToTaskCreate(selectedDate) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(42.dp)
                                .onGloballyPositioned { coordinates -> // ③ ボタン本体(楕円全体)の座標を記録
                                    targetRects["add_button"] = coordinates.boundsInRoot()
                                }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("追加", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .bounceClick { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                            Text("設定", fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        containerColor = colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            if (displayMode != CalendarDisplayMode.YEAR) {
                WeekdayHeaderRow(
                    weekStartDay = weekStartDay,
                    showWeekNumberColumn = showWeekNumber && displayMode == CalendarDisplayMode.MONTH,
                    leadingWidth = if (displayMode == CalendarDisplayMode.WEEK && weekDayPreviewIsTimetable) 40.dp else 0.dp
                )
            }

            when (displayMode) {
                CalendarDisplayMode.MONTH -> MonthPagerView(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    today = today,
                    tasksByDate = tasksByDate,
                    holidayMap = holidayMap,
                    showTagColorOnCalendar = showTagColorOnCalendar,
                    showWeekNumber = showWeekNumber,
                    weekNumberOf = weekNumberOf,
                    buildCalendarMatrix = buildCalendarMatrix,
                    onDateSelected = onDateSelected,
                    onNavigateToDateDetail = onNavigateToDateDetail,
                    onNavigateToTaskDetail = onNavigateToTaskDetail,
                    onUpdateYearMonth = onUpdateYearMonth
                )

                CalendarDisplayMode.WEEK -> WeekPagerView(
                    currentWeekStart = currentWeekStart,
                    selectedDate = selectedDate,
                    today = today,
                    tasksByDate = tasksByDate,
                    holidayMap = holidayMap,
                    showTagColorOnCalendar = showTagColorOnCalendar,
                    buildWeekCalendarMatrix = buildWeekCalendarMatrix,
                    onDateSelected = onDateSelected,
                    onNavigateToDateDetail = onNavigateToDateDetail,
                    onUpdateWeekStart = onUpdateWeekStart,
                    onNavigateToTaskDetail = onNavigateToTaskDetail,
                    onToggleTaskCompletion = onToggleTaskCompletion,
                    isTimetableMode = weekDayPreviewIsTimetable,
                    onTogglePreviewMode = onToggleWeekDayPreviewMode,
                )

                CalendarDisplayMode.YEAR -> YearGridView(
                    year = currentMonth.year,
                    focusMonth = currentMonth.monthValue,
                    selectedDate = selectedDate,
                    today = today,
                    tasksByDate = tasksByDate,
                    holidayMap = holidayMap,
                    weekStartDay = weekStartDay,
                    showTagColorOnCalendar = showTagColorOnCalendar,
                    buildYearMatrix = buildYearMatrix,
                    buildCalendarMatrix = buildCalendarMatrix,
                    onDateSelected = onDateSelectedFromYearView
                )
            }
        }

        if (showMonthYearDialog) {
            MonthYearPickerDialog(
                currentMonth = if (displayMode == CalendarDisplayMode.WEEK) YearMonth.from(
                    currentWeekStart
                ) else currentMonth,
                onDismiss = { showMonthYearDialog = false },
                onConfirm = { year, month ->
                    if (month in 1..12) {
                        when (displayMode) {
                            // 週表示のときは即座に週を確定せず、その月の週一覧を選ばせる2段階目を開く
                            CalendarDisplayMode.WEEK -> {
                                weekSelectionMonth = YearMonth.of(year, month)
                            }

                            else -> onUpdateYearMonth(year, month)
                        }
                    }
                    showMonthYearDialog = false
                }
            )
        }

        if (showTagFilterDialog) {
            AlertDialog(
                onDismissRequest = { showTagFilterDialog = false },
                title = { Text("タグで絞り込む", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            if (filterIsAndSearch) "選択したタグをすべて含む予定のみ表示します。"
                            else "選択したタグのいずれかを含む予定のみ表示します。",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (allTags.isEmpty()) {
                            Text("タグがまだありません", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
                        } else {
                            val textMeasurer = rememberTextMeasurer()
                            val density = LocalDensity.current

                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val usableWidthPx = with(density) { maxWidth.toPx() }

                                val displayTags = remember(
                                    allTags,
                                    selectedFilterTagIds.toList(),
                                    isTagFolderExpanded,
                                    usableWidthPx
                                ) {
                                    if (isTagFolderExpanded) {
                                        allTags
                                    } else {
                                        val defaultVisibleIds = LinkedHashSet<Long>()
                                        var usedWidthPx = 0f
                                        val spacingPx = with(density) { 8.dp.toPx() }

                                        allTags.forEach { tag ->
                                            val textWidthPx = textMeasurer.measure(
                                                text = tag.name,
                                                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            ).size.width.toFloat()
                                            val fixedWidthPx = with(density) {
                                                (16.dp + 20.dp +
                                                        if (TagIconId.fromId(tag.icon) != null) 6.dp else 0.dp).toPx()
                                            }
                                            val chipWidthPx = fixedWidthPx + textWidthPx
                                            val requiredWidthPx = chipWidthPx +
                                                    if (defaultVisibleIds.isEmpty()) 0f else spacingPx

                                            if (
                                                defaultVisibleIds.isEmpty() ||
                                                usedWidthPx + requiredWidthPx <= usableWidthPx
                                            ) {
                                                defaultVisibleIds.add(tag.tagId)
                                                usedWidthPx += requiredWidthPx
                                            }
                                        }

                                        selectedFilterTagIds.forEach { defaultVisibleIds.add(it) }
                                        allTags.filter { it.tagId in defaultVisibleIds }
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    com.foxdog.strucalendar.components.DraggableTagList(
                                        tags = displayTags,
                                        allTags = allTags,
                                        selectedTags = allTags.filter { it.tagId in selectedFilterTagIds },
                                        onTagClick = { tag -> onToggleFilterTag(tag.tagId) },
                                        onOrderChanged = onUpdateTagOrder,
                                        onDeleteTagRequest = { tagToDelete = it },
                                        onEditTagRequest = { tag ->
                                            tagToEdit = tag
                                            showTagCreateDialog = true
                                        }
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .border(BorderStroke(1.dp, colorScheme.outline), RoundedCornerShape(8.dp))
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colorScheme.surface)
                                                .clickable {
                                                    tagToEdit = null
                                                    showTagCreateDialog = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "新規タグ作成", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }

                                        if (displayTags.size < allTags.size || isTagFolderExpanded) {
                                            TextButton(
                                                onClick = { isTagFolderExpanded = !isTagFolderExpanded },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                            ) {
                                                Text(text = if (isTagFolderExpanded) "閉じる ▲" else "さらに表示 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("絞り込み条件:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)

                                        Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colorScheme.surfaceVariant).padding(2.dp)) {
                                            val activeColor = colorScheme.surface
                                            val inactiveColor = Color.Transparent
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (!filterIsAndSearch) activeColor else inactiveColor)
                                                    .clickable { onSetFilterIsAndSearch(false) }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("いずれか (OR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, color = if (!filterIsAndSearch) colorScheme.onSurface else colorScheme.onSurfaceVariant)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (filterIsAndSearch) activeColor else inactiveColor)
                                                    .clickable { onSetFilterIsAndSearch(true) }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("すべて含む (AND)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, color = if (filterIsAndSearch) colorScheme.onSurface else colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onResetTagFilter) { Text("リセット") }
                },
                confirmButton = {
                    TextButton(onClick = { showTagFilterDialog = false }) { Text("閉じる") }
                }
            )
        }

        if (tagToDelete != null) {
            AlertDialog(
                onDismissRequest = { tagToDelete = null },
                title = { Text("タグの削除", fontWeight = FontWeight.Bold) },
                text = { Text("タグ「${tagToDelete?.name}」を削除しますか？\n(この操作は取り消せません)") },
                dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("キャンセル") } },
                confirmButton = {
                    Button(
                        onClick = {
                            tagToDelete?.let { target ->
                                if (target.tagId in selectedFilterTagIds) {
                                    onToggleFilterTag(target.tagId)
                                }
                                onDeleteTag(target)
                            }
                            tagToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                    ) {
                        Text("削除する")
                    }
                }
            )
        }

        if (showTagCreateDialog) {
            var editCustomFields by remember(tagToEdit) { mutableStateOf<List<String>>(emptyList()) }
            LaunchedEffect(tagToEdit) {
                tagToEdit?.let { tag ->
                    editCustomFields = onLoadCustomFieldsForTag(tag.tagId)
                }
            }

            TagCreateDialog(
                confirmDiscardChanges = confirmDiscardChanges,
                existingTag = tagToEdit,
                existingCustomFields = editCustomFields,
                onDismissRequest = {
                    showTagCreateDialog = false
                    tagToEdit = null
                },
                onTagSave = { name, iconSource, color, customFieldNames ->
                    val iconString = when (iconSource) {
                        is TagIconSource.InitialText -> null
                        is TagIconSource.Vector -> iconSource.iconId.id
                    }

                    val editing = tagToEdit
                    if (editing != null) {
                        onUpdateTag(
                            editing.copy(name = name, color = color.toArgb(), icon = iconString),
                            customFieldNames
                        )
                    } else {
                        onCreateTag(
                            Tag(tagId = 0L, name = name, color = color.toArgb(), icon = iconString),
                            customFieldNames
                        )
                    }

                    showTagCreateDialog = false
                    tagToEdit = null
                }
            )
        }

        // 週表示専用「週選択」ダイアログ
        val monthForWeekSelection = weekSelectionMonth
        if (monthForWeekSelection != null) {
            WeekSelectionDialog(
                yearMonth = monthForWeekSelection,
                weekStartDay = weekStartDay,
                currentWeekStart = currentWeekStart,
                onSelectWeek = { weekStart ->
                    onUpdateWeekStart(weekStart)
                    weekSelectionMonth = null
                },
                onBackToMonthYear = {
                    weekSelectionMonth = null
                    showMonthYearDialog = true
                },
                onDismiss = { weekSelectionMonth = null }
            )
        }
    }
    // オンボーディング
    if (showOnboarding && !onboardingDismissed) {
        SpotlightOnboardingOverlay(
            steps = onboardingSteps,
            targetRects = targetRects,
            onSkip = {
                onboardingDismissed = true
                onOnboardingFinished()
            },
            onShowLater = {
                onboardingDismissed = true
                // 完了フラグは保存しない → 次回起動時にまた表示される
            },
            onFinish = {
                onboardingDismissed = true
                onOnboardingFinished()
            }
        )
    }
    if (showAllTutorialsCompletedDialog) {
        AlertDialog(
            onDismissRequest = onDismissAllTutorialsCompletedDialog,
            title = { Text("すべてのチュートリアルが完了しました", fontWeight = FontWeight.Bold) },
            text = { Text("チュートリアルを再度確認したい場合、設定画面から再度有効にすることができます。") },
            confirmButton = {
                TextButton(onClick = onDismissAllTutorialsCompletedDialog) { Text("OK") }
            }
        )
    }
}

// ============================================================
// 週表示モード用「その月に含まれる週」の選択ダイアログ
// ============================================================

private fun startOfWeekForPicker(date: LocalDate, weekStartDay: java.time.DayOfWeek): LocalDate {
    val diff = (date.dayOfWeek.value - weekStartDay.value + 7) % 7
    return date.minusDays(diff.toLong())
}

/**
 * [yearMonth] の初日を含む週から、末日を含む週までの各週の開始日一覧を返す
 * （月をまたぐ週も含む。境界の扱いはbuildWeekCalendarMatrixと揃えている）
 */
private fun weekStartsInMonth(yearMonth: YearMonth, weekStartDay: java.time.DayOfWeek): List<LocalDate> {
    val firstDay = yearMonth.atDay(1)
    val lastDay = yearMonth.atEndOfMonth()
    var cursor = startOfWeekForPicker(firstDay, weekStartDay)
    val result = mutableListOf<LocalDate>()
    while (!cursor.isAfter(lastDay)) {
        result.add(cursor)
        cursor = cursor.plusWeeks(1)
    }
    return result
}

private fun weekRangeLabel(weekStart: LocalDate): String {
    val weekEnd = weekStart.plusDays(6)
    return if (weekStart.monthValue == weekEnd.monthValue) {
        "${weekStart.dayOfMonth}日 〜 ${weekEnd.dayOfMonth}日"
    } else {
        "${weekStart.monthValue}/${weekStart.dayOfMonth} 〜 ${weekEnd.monthValue}/${weekEnd.dayOfMonth}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekSelectionDialog(
    yearMonth: YearMonth,
    weekStartDay: java.time.DayOfWeek,
    currentWeekStart: LocalDate,
    onSelectWeek: (LocalDate) -> Unit,
    onBackToMonthYear: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val weeks = remember(yearMonth, weekStartDay) { weekStartsInMonth(yearMonth, weekStartDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${yearMonth.year}年 ${yearMonth.monthValue}月の週を選択", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                weeks.forEach { weekStart ->
                    val isSelected = weekStart == currentWeekStart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectWeek(weekStart) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { onSelectWeek(weekStart) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(weekRangeLabel(weekStart), fontSize = 14.sp, color = colorScheme.onSurface)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onBackToMonthYear) { Text("年月を選び直す") }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    )
}

// --- Compose Preview (開発・デザイン確認用プレビュー) ---

@Preview(showBackground = true, name = "カレンダー画面（通常表示）")
@Composable
fun CalendarScreenPreview() {
    val today = LocalDate.now()
    val dummyTags = listOf(Tag(tagId = 1L, name = "会議", color = Color(0xFF1E88E5).toArgb(), icon = null))
    val dummyTasks = mapOf(
        today to listOf(
            TaskWithTags(
                task = Task(
                    taskId = 1L,
                    title = "全体定例ミーティング",
                    startTime = Instant.now().epochSecond,
                    endTime = Instant.now().plusSeconds(3600).epochSecond,
                    memo = "",  color = Color(0xFF4285F4).toArgb(),
                    attachmentPath = "", url = "", locationName = "筑波大学",
                    locationAddress = "茨城県つくば市天王台1-1-1",
                    isAutoCompleted = false, completeState = "INCOMPLETE",
                    reminderOffsetMinutes = 10, dayCountTarget = null, templateId = null, isAllDay = false
                ),
                tags = dummyTags
            )
        )
    )

    val firstDayOfMonth = today.withDayOfMonth(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val dummyMatrix = mutableListOf<LocalDate?>()
    for (i in 0 until startDayOfWeek) dummyMatrix.add(null)
    for (i in 1..today.lengthOfMonth()) dummyMatrix.add(today.withDayOfMonth(i))
    while (dummyMatrix.size < 42) dummyMatrix.add(null)

    CalendarScreenContent(
        currentMonth = YearMonth.now(),
        selectedDate = today,
        tasksByDate = dummyTasks,
        buildCalendarMatrix = { dummyMatrix },
        onDateSelected = {},
        onUpdateYearMonth = { _, _ -> },
        onNavigateToTaskCreate = {},
        onNavigateToTaskList = {},
        onNavigateToDateDetail = {},
        onNavigateToSettings = {},
        onNavigateToTaskDetail = {}
    )
}

@Preview(showBackground = true, widthDp = 840, heightDp = 480, name = "カレンダー画面（タブレット横画面）")
@Composable
fun CalendarScreenTabletPreview() {
    CalendarScreenPreview()
}