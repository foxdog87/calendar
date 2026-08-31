package com.foxdog.strucalendar.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.DraggableTagList
import com.foxdog.strucalendar.components.SpotlightOnboardingOverlay
import com.foxdog.strucalendar.components.SpotlightShape
import com.foxdog.strucalendar.components.SpotlightStep
import com.foxdog.strucalendar.components.TagCreateDialog
import com.foxdog.strucalendar.components.TagIconId
import com.foxdog.strucalendar.components.TagIconSource
import com.foxdog.strucalendar.components.TagLabel
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.ui.theme.calendarColors
import com.foxdog.strucalendar.viewmodel.TaskListViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import com.foxdog.strucalendar.ui.bounceClick
import androidx.compose.material.icons.filled.Repeat
import com.foxdog.strucalendar.data.entity.RecurrenceSeriesSummary
import com.foxdog.strucalendar.data.recurrence.RecurrenceSummaryFormatter
import com.foxdog.strucalendar.ui.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit
) {
    val tasksWithTags by viewModel.allTasksWithTags.collectAsState()
    val dynamicTags by viewModel.allTags.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val recurrenceSeries = viewModel.recurrenceSeries
    val coroutineScope = rememberCoroutineScope()

    TaskListContent(
        tasksWithTags = tasksWithTags,
        dynamicTags = dynamicTags,
        defaultShowCompleted = settings.showCompletedTasks,
        taskListOnboardingCompleted = settings.taskListOnboardingCompleted,
        bulkDeletePreviewCount = viewModel.bulkDeletePreviewCount,
        isBulkDeleting = viewModel.isBulkDeleting,
        recurrenceSeries = recurrenceSeries,
        onLoadRecurrenceSeries = { viewModel.loadRecurrenceSeries() },
        onDeleteRecurrenceSeries = { groupIds, onComplete -> viewModel.deleteRecurrenceSeries(groupIds, onComplete) },
        onNavigateBack = onNavigateBack,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onToggleTaskCompletion = { item ->
            coroutineScope.launch { viewModel.toggleTaskCompletion(item) }
        },
        onDeleteTag = { tag -> viewModel.deleteTag(tag) },
        onCreateTag = { tag, customFieldNames ->
            viewModel.createTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
        },
        onUpdateTag = { tag, customFieldNames ->
            viewModel.updateTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
        },
        onLoadCustomFieldsForTag = { tagId -> viewModel.getCustomFieldNamesForTag(tagId) },
        onUpdateTagOrder = { tags -> viewModel.updateTagOrder(tags) },
        onPreviewBulkDelete = { cutoffEpoch, includeCompleted, includeUncompleted ->
            viewModel.previewBulkDelete(cutoffEpoch, includeCompleted, includeUncompleted)
        },
        onClearBulkDeletePreview = { viewModel.clearBulkDeletePreview() },
        onExecuteBulkDelete = { cutoffEpoch, includeCompleted, includeUncompleted, onComplete ->
            viewModel.executeBulkDelete(cutoffEpoch, includeCompleted, includeUncompleted, onComplete)
        },
        onOnboardingFinished = { viewModel.setTaskListOnboardingCompleted(true) },
        showAllTutorialsCompletedDialog = viewModel.showAllTutorialsCompletedDialog,
        onDismissAllTutorialsCompletedDialog = { viewModel.dismissAllTutorialsCompletedDialog() },
        confirmDiscardChanges = settings.confirmDiscardChanges,
    )
}

// Preview や テスト用に分離した UI コンポーネント
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListContent(
    tasksWithTags: List<TaskWithTags>,
    dynamicTags: List<Tag>,
    defaultShowCompleted: Boolean = false,
    taskListOnboardingCompleted: Boolean = true, // （Previewとの互換のためデフォルトtrue＝出さない）
    bulkDeletePreviewCount: Int? = null,
    isBulkDeleting: Boolean = false,
    recurrenceSeries: List<RecurrenceSeriesSummary> = emptyList(),
    onLoadRecurrenceSeries: () -> Unit = {},
    onDeleteRecurrenceSeries: (groupIds: List<String>, onComplete: () -> Unit) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag, List<String>) -> Unit,
    onUpdateTag: (Tag, List<String>) -> Unit = { _, _ -> },
    onLoadCustomFieldsForTag: suspend (Long) -> List<String> = { emptyList() },
    onUpdateTagOrder: (List<Tag>) -> Unit,
    onPreviewBulkDelete: (cutoffEpoch: Long?, includeCompleted: Boolean, includeUncompleted: Boolean) -> Unit = { _, _, _ -> },
    onClearBulkDeletePreview: () -> Unit = {},
    onExecuteBulkDelete: (cutoffEpoch: Long?, includeCompleted: Boolean, includeUncompleted: Boolean, onComplete: () -> Unit) -> Unit = { _, _, _, _ -> },
    onOnboardingFinished: () -> Unit = {},
    showAllTutorialsCompletedDialog: Boolean = false,
    onDismissAllTutorialsCompletedDialog: () -> Unit = {},
    confirmDiscardChanges: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    val selectedTagIds = remember { mutableStateListOf<Long>() }
    var isAndSearch by remember { mutableStateOf(false) }
    var taskNameQuery by remember { mutableStateOf("") }

    var showCompleted by remember(defaultShowCompleted) { mutableStateOf(defaultShowCompleted) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var tagToEdit by remember { mutableStateOf<Tag?>(null) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    // 絞り込み条件全体の表示/非表示。初期状態は展開。
    var isFilterPanelExpanded by remember { mutableStateOf(true) }
    val now = Instant.now().epochSecond
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showRecurrenceDeleteDialog by remember { mutableStateOf(false) }

    // オンボーディング用のターゲット座標
    var showOnboarding by remember(taskListOnboardingCompleted) { mutableStateOf(!taskListOnboardingCompleted) }
    val onboardingTargetRects = remember { mutableStateMapOf<String, Rect>() }

    val selectedTagsList by remember(dynamicTags, selectedTagIds.toList()) {
        derivedStateOf {
            dynamicTags.filter { selectedTagIds.contains(it.tagId) }
        }
    }

    val filteredTasks by remember(tasksWithTags, isAndSearch, selectedTagIds.toList(), showCompleted, taskNameQuery) {
        derivedStateOf {
            val baseList = if (showCompleted) tasksWithTags
            else tasksWithTags.filter { it.task.completeState != "COMPLETED" }

            val tagFiltered = if (selectedTagIds.isEmpty()) {
                baseList
            } else {
                baseList.filter { taskWithTags ->
                    val taskTagIds = taskWithTags.tags.map { it.tagId }.toSet()
                    if (isAndSearch) {
                        selectedTagIds.all { it in taskTagIds }
                    } else {
                        selectedTagIds.any { it in taskTagIds }
                    }
                }
            }

            val trimmedQuery = taskNameQuery.trim()
            if (trimmedQuery.isEmpty()) {
                tagFiltered
            } else {
                tagFiltered.filter { taskWithTags ->
                    taskWithTags.task.title.contains(trimmedQuery, ignoreCase = true)
                }
            }
        }
    }

    val pastTasks = remember(filteredTasks) { filteredTasks.filter { it.task.endTime < now } }
    val upcomingTasks = remember(filteredTasks) { filteredTasks.filter { it.task.endTime >= now } }
    val listState = rememberLazyListState()

    var hasAutoScrolledForFilter by remember(selectedTagIds.toList(), isAndSearch, showCompleted, taskNameQuery) {
        mutableStateOf(false)
    }

    LaunchedEffect(selectedTagIds.toList(), isAndSearch, showCompleted, taskNameQuery, tasksWithTags) {
        if (!hasAutoScrolledForFilter && pastTasks.isNotEmpty()) {
            listState.scrollToItem(pastTasks.size)
            hasAutoScrolledForFilter = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("予定・タスク一覧", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, tint = colorScheme.onSurface, contentDescription = "戻る")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                onboardingTargetRects["bulk_delete_button"] = coordinates.boundsInRoot()
                            }
                        ) {
                            Icon(Icons.Default.DeleteSweep, tint = colorScheme.onSurface, contentDescription = "過去のタスクを一括削除")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
                )
            },
            containerColor = colorScheme.surface
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                // --- 絞り込み条件 ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.background)
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                onboardingTargetRects["search_filter_area"] = coordinates.boundsInRoot()
                            }
                            .clickable { isFilterPanelExpanded = !isFilterPanelExpanded }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "絞り込み条件",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isFilterPanelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isFilterPanelExpanded) "絞り込み条件を閉じる" else "絞り込み条件を開く",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }

                    if (isFilterPanelExpanded) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

                        OutlinedTextField(
                            value = taskNameQuery,
                            onValueChange = { taskNameQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            placeholder = { Text("タスク名で検索", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                            },
                            trailingIcon = {
                                if (taskNameQuery.isNotEmpty()) {
                                    IconButton(onClick = { taskNameQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "検索文字をクリア", tint = colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("タグで絞り込む", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                            Text(
                                "(タップで選択・ドラッグで並替/編集/削除)",
                                fontSize = 10.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }

                        val textMeasurer = rememberTextMeasurer()
                        val density = LocalDensity.current

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val usableWidthPx = with(density) { (maxWidth - 32.dp).toPx() }

                            val displayTags = remember(
                                dynamicTags,
                                selectedTagIds.toList(),
                                isTagFolderExpanded,
                                usableWidthPx
                            ) {
                                if (isTagFolderExpanded) {
                                    dynamicTags
                                } else {
                                    val defaultVisibleIds = LinkedHashSet<Long>()
                                    var usedWidthPx = 0f
                                    val spacingPx = with(density) { 8.dp.toPx() }

                                    dynamicTags.forEach { tag ->
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

                                    selectedTagIds.forEach { defaultVisibleIds.add(it) }
                                    dynamicTags.filter { it.tagId in defaultVisibleIds }
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DraggableTagList(
                                    tags = displayTags,
                                    allTags = dynamicTags,
                                    selectedTags = selectedTagsList,
                                    onTagClick = { tag ->
                                        if (selectedTagIds.contains(tag.tagId)) {
                                            selectedTagIds.remove(tag.tagId)
                                        } else {
                                            selectedTagIds.add(tag.tagId)
                                        }
                                    },
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

                                    if (displayTags.size < dynamicTags.size || isTagFolderExpanded) {
                                        TextButton(
                                            onClick = { isTagFolderExpanded = !isTagFolderExpanded },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                        ) {
                                            Text(text = if (isTagFolderExpanded) "閉じる ▲" else "さらに表示 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Text("フィルター条件:", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                        }

                        Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colorScheme.surfaceVariant).padding(2.dp)) {
                            val activeColor = colorScheme.surface
                            val inactiveColor = Color.Transparent
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (!isAndSearch) activeColor else inactiveColor)
                                    .clickable { isAndSearch = false }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("いずれか (OR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, color = if (!isAndSearch) colorScheme.onSurface else colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isAndSearch) activeColor else inactiveColor)
                                    .clickable { isAndSearch = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("すべて含む (AND)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, color = if (isAndSearch) colorScheme.onSurface else colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showCompleted = !showCompleted }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showCompleted,
                            onCheckedChange = { showCompleted = it },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("完了済み予定を含める", fontSize = 12.sp, color = colorScheme.onSurface)
                    }

                        }
                    }

                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorScheme.outline
                )

                // --- タスクリスト本体表示 ---
                if (filteredTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (selectedTagIds.isEmpty()) "予定はありません" else "該当するタスクが見つかりません",
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .onGloballyPositioned { coordinates -> // タスクカード説明の対象エリア
                                onboardingTargetRects["task_card_area"] = coordinates.boundsInRoot()
                            },
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                    ) {
                        items(pastTasks, key = { it.task.taskId }) { item ->
                            TaskCard(
                                item = item,
                                dateFormatter = dateFormatter,
                                timeFormatter = timeFormatter,
                                onToggleTaskCompletion = onToggleTaskCompletion,
                                onClick = { onNavigateToTaskDetail(item.task.taskId) }
                            )
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                                HorizontalDivider(modifier = Modifier.weight(1f))
                                Text("過去のタスク", color = colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                HorizontalDivider(modifier = Modifier.weight(1f))
                            }
                        }

                        items(upcomingTasks, key = { it.task.taskId }) { item ->
                            TaskCard(
                                item = item,
                                dateFormatter = dateFormatter,
                                timeFormatter = timeFormatter,
                                onToggleTaskCompletion = onToggleTaskCompletion,
                                onClick = { onNavigateToTaskDetail(item.task.taskId) }
                            )
                        }
                    }
                }
            }
        }

        // 4ステップのオンボーディング
        if (showOnboarding) {
            val onboardingSteps = remember {
                listOf(
                    SpotlightStep(
                        targetKey = "bulk_delete_button",
                        title = "一括削除",
                        description = "期間や完了状況を指定して過去のタスクをまとめて削除したり、繰り返しタスクをシリーズ単位で削除できます。",
                        shape = SpotlightShape.OVAL,
                        highlightPadding = 8.dp
                    ),
                    SpotlightStep(
                        targetKey = "search_filter_area",
                        title = "検索条件",
                        description = "タグをタップして予定を絞り込めます。「いずれか(OR)」「すべて含む(AND)」の切り替えや、完了済みタスクの表示有無もここで設定できます。",
                        shape = SpotlightShape.ROUNDED_RECT
                    ),
                    SpotlightStep(
                        targetKey = "task_card_area",
                        title = "タスクカード",
                        description = "左の丸いアイコンをタップすると完了/未完了を切り替えられます。カードをタップすると詳細画面に移動します。",
                        shape = SpotlightShape.ROUNDED_RECT
                    )
                )
            }

            SpotlightOnboardingOverlay(
                steps = onboardingSteps,
                targetRects = onboardingTargetRects,
                introTitle = "予定一覧画面へようこそ",
                introDescription = "登録した予定をタグや条件で絞り込んで管理できます。簡単にご紹介します。",
                onSkip = {
                    showOnboarding = false
                    onOnboardingFinished()
                },
                onShowLater = {
                    showOnboarding = false
                    // 完了扱いにはせず、次回また表示されるようにする
                },
                onFinish = {
                    showOnboarding = false
                    onOnboardingFinished()
                }
            )
        }
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
                            selectedTagIds.remove(target.tagId)
                            onDeleteTag(target)
                        }
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
                    val newTag = Tag(
                        tagId = 0L,
                        name = name,
                        color = color.toArgb(),
                        icon = iconString
                    )
                    onCreateTag(newTag, customFieldNames)
                }

                showTagCreateDialog = false
                tagToEdit = null
            }
        )
    }


    if (showBulkDeleteDialog) {
        BulkDeleteDialogContent(
            previewCount = bulkDeletePreviewCount,
            isDeleting = isBulkDeleting,
            onPreview = onPreviewBulkDelete,
            onClearPreview = onClearBulkDeletePreview,
            onExecute = onExecuteBulkDelete,
            onOpenRecurrenceDelete = {
                showBulkDeleteDialog = false
                onClearBulkDeletePreview()
                onLoadRecurrenceSeries()
                showRecurrenceDeleteDialog = true
            },
            onDismiss = {
                showBulkDeleteDialog = false
                onClearBulkDeletePreview()
            }
        )
    }

    if (showRecurrenceDeleteDialog) {
        RecurrenceSeriesDeleteDialogContent(
            series = recurrenceSeries,
            onExecute = { groupIds, onComplete -> onDeleteRecurrenceSeries(groupIds, onComplete) },
            onDismiss = { showRecurrenceDeleteDialog = false }
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
// 過去タスク一括削除
// ============================================================

enum class BulkDeleteCutoff(val label: String, val shortLabel: String, val monthsAgo: Long?) {
    ONE_MONTH("1ヶ月以上前", "1ヶ月", 1),
    THREE_MONTHS("3ヶ月以上前", "3ヶ月", 3),
    SIX_MONTHS("6ヶ月以上前", "6ヶ月", 6),
    ONE_YEAR("1年以上前", "1年", 12),
    ALL("全期間", "すべて", null);

    fun toEpochSecond(): Long? {
        val months = monthsAgo ?: return null
        return LocalDateTime.now()
            .minusMonths(months)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkDeleteDialogContent(
    previewCount: Int?,
    isDeleting: Boolean,
    onPreview: (cutoffEpoch: Long?, includeCompleted: Boolean, includeUncompleted: Boolean) -> Unit,
    onClearPreview: () -> Unit,
    onExecute: (cutoffEpoch: Long?, includeCompleted: Boolean, includeUncompleted: Boolean, onComplete: () -> Unit) -> Unit,
    onOpenRecurrenceDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedCutoff by remember { mutableStateOf(BulkDeleteCutoff.ONE_YEAR) }
    var includeCompleted by remember { mutableStateOf(true) }
    var includeUncompleted by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCutoff, includeCompleted, includeUncompleted) {
        onPreview(selectedCutoff.toEpochSecond(), includeCompleted, includeUncompleted)
    }

    AlertDialog(
        onDismissRequest = {
            onClearPreview()
            onDismiss()
        },
        title = { Text("過去のタスクを一括削除", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("対象期間", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BulkDeleteCutoff.entries.forEachIndexed { index, cutoff ->
                        SegmentedButton(
                            selected = selectedCutoff == cutoff,
                            onClick = { selectedCutoff = cutoff },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = BulkDeleteCutoff.entries.size
                            ),
                            icon = {},
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Text(
                                cutoff.shortLabel,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("削除対象", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeUncompleted = !includeUncompleted },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeUncompleted, onCheckedChange = { includeUncompleted = it })
                    Text("範囲内の未完了タスクを削除する", fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { includeCompleted = !includeCompleted },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = includeCompleted, onCheckedChange = { includeCompleted = it })
                    Text("範囲内の完了タスクを削除する", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                val badgeText = when {
                    includeCompleted && includeUncompleted -> "範囲内の未完了・完了タスクがすべて削除されます"
                    includeCompleted -> "範囲内の完了タスクのみが削除されます"
                    includeUncompleted -> "範囲内の未完了タスクのみが削除されます"
                    else -> "削除対象が選択されていません"
                }
                val badgeColor = when {
                    includeCompleted && includeUncompleted -> colorScheme.errorContainer
                    includeCompleted || includeUncompleted -> colorScheme.surfaceVariant
                    else -> colorScheme.surfaceVariant
                }
                val badgeContentColor = when {
                    includeCompleted && includeUncompleted -> colorScheme.onErrorContainer
                    includeCompleted || includeUncompleted -> colorScheme.onSurfaceVariant
                    else -> colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = badgeContentColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenRecurrenceDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("繰り返しタスクをまとめて削除", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colorScheme.outline)
                Spacer(modifier = Modifier.height(12.dp))

                if (previewCount == null) {
                    Text("件数を確認中...", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        "${previewCount}件のタスクが削除されます。この操作は取り消せません。",
                        fontSize = 12.sp,
                        color = colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onClearPreview()
                onDismiss()
            }) { Text("キャンセル") }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExecute(selectedCutoff.toEpochSecond(), includeCompleted, includeUncompleted) { onDismiss() }
                },
                enabled = previewCount?.let { it > 0 } == true && !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
            ) {
                Text(if (isDeleting) "削除中..." else "削除する")
            }
        }
    )
}



// ============================================================
// 繰り返しシリーズ単位の一括削除
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceSeriesDeleteDialogContent(
    series: List<RecurrenceSeriesSummary>,
    onExecute: (groupIds: List<String>, onComplete: () -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedGroupIds = remember { mutableStateListOf<String>() }
    var isDeleting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("繰り返しタスクを削除", fontWeight = FontWeight.Bold) },
        text = {
            if (series.isEmpty()) {
                Text("繰り返し設定されたタスクはありません", fontSize = 13.sp, color = colorScheme.onSurfaceVariant)
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "削除したいシリーズを選択してください。選択したシリーズは、過去・未来を含む全ての回が削除されます。",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    series.forEach { summary ->
                        val isSelected = selectedGroupIds.contains(summary.recurrenceGroupId)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isSelected) selectedGroupIds.remove(summary.recurrenceGroupId)
                                    else selectedGroupIds.add(summary.recurrenceGroupId)
                                }
                                .background(if (isSelected) colorScheme.errorContainer.copy(alpha = 0.3f) else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isSelected) selectedGroupIds.remove(summary.recurrenceGroupId)
                                    else selectedGroupIds.add(summary.recurrenceGroupId)
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${RecurrenceSummaryFormatter.ruleLabel(summary)} ・ ${RecurrenceSummaryFormatter.endDateLabel(summary)} ・ 全${summary.occurrenceCount}件",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (summary != series.last()) {
                            HorizontalDivider(color = colorScheme.outline)
                        }
                    }

                    if (selectedGroupIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "選択した${selectedGroupIds.size}件のシリーズが削除されます。この操作は取り消せません。",
                            fontSize = 12.sp,
                            color = colorScheme.error
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
        confirmButton = {
            Button(
                onClick = {
                    isDeleting = true
                    onExecute(selectedGroupIds.toList()) {
                        isDeleting = false
                        onDismiss()
                    }
                },
                enabled = selectedGroupIds.isNotEmpty() && !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
            ) {
                Text(if (isDeleting) "削除中..." else "削除する")
            }
        }
    )
}

// --- Compose Preview（Onboardingはデフォルトtrueなので出ません） ---

@Preview(showBackground = true, name = "タスク一覧画面（通常表示）")
@Composable
fun TaskListContentPreview() {
    val dummyTags = listOf(
        Tag(tagId = 1L, name = "重要", color = Color(0xFFE53935).toArgb(), icon = null),
        Tag(tagId = 2L, name = "仕事", color = Color(0xFF1E88E5).toArgb(), icon = "Book"),
        Tag(tagId = 3L, name = "プライベート", color = Color(0xFF43A047).toArgb(), icon = null)
    )

    val dummyTasks = listOf(
        TaskWithTags(
            task = Task(
                taskId = 101L,
                title = "週次進捗報告ミーティング",
                startTime = Instant.now().epochSecond,
                endTime = Instant.now().plusSeconds(3600).epochSecond,
                memo = "", color = Color(0xFF1E88E5).toArgb(),
                attachmentPath = "", url = "", locationName = "筑波大学",
                locationAddress = "茨城県つくば市天王台1-1-1",
                isAutoCompleted = false, completeState = "INCOMPLETE",
                reminderType = "BEFORE",
                reminderOffsetMinutes = 10,
                reminderDayOffset = null,
                reminderHour = null,
                reminderMinute = null,
                dayCountTarget = null, templateId = null, isAllDay = false
            ),
            tags = listOf(dummyTags[0], dummyTags[1])
        ),
        TaskWithTags(
            task = Task(
                taskId = 102L,
                title = "日用品の買い物",
                startTime = Instant.now().minusSeconds(7200).epochSecond,
                endTime = Instant.now().minusSeconds(3600).epochSecond,
                memo = "", color = Color(0xFF43A047).toArgb(),
                attachmentPath = "", url = "", locationName = "筑波大学",
                locationAddress = "茨城県つくば市天王台1-1-1",
                isAutoCompleted = false, completeState = "COMPLETED",
                reminderType = null,
                reminderOffsetMinutes = null,
                reminderDayOffset = null,
                reminderHour = null,
                reminderMinute = null,
                dayCountTarget = null, templateId = null, isAllDay = false
            ),
            tags = listOf(dummyTags[2])
        )
    )

    TaskListContent(
        tasksWithTags = dummyTasks,
        dynamicTags = dummyTags,
        onNavigateBack = {},
        onNavigateToTaskDetail = {},
        onToggleTaskCompletion = {},
        onDeleteTag = {},
        onCreateTag = { _, _ -> },
        onUpdateTagOrder = {}
    )
}

@Preview(showBackground = true, name = "タスク一覧画面（該当なし表示）")
@Composable
fun TaskListContentEmptyPreview() {
    TaskListContent(
        tasksWithTags = emptyList(),
        dynamicTags = emptyList(),
        onNavigateBack = {},
        onNavigateToTaskDetail = {},
        onToggleTaskCompletion = {},
        onDeleteTag = {},
        onCreateTag = { _, _ -> },
        onUpdateTagOrder = {}
    )
}