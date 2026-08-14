package com.example.calendar.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.components.DraggableTagList
import com.example.calendar.components.TagCreateDialog
import com.example.calendar.components.TagIconId
import com.example.calendar.components.TagIconSource
import com.example.calendar.components.TagLabel
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.relation.TaskWithTags
import com.example.calendar.viewmodel.TaskListViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit
) {
    val tasksWithTags by viewModel.allTasksWithTags.collectAsState()
    val dynamicTags by viewModel.allTags.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    TaskListContent(
        tasksWithTags = tasksWithTags,
        dynamicTags = dynamicTags,
        onNavigateBack = onNavigateBack,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onToggleTaskCompletion = { item ->
            coroutineScope.launch { viewModel.toggleTaskCompletion(item) }
        },
        onDeleteTag = { tag -> viewModel.deleteTag(tag) },
        onCreateTag = { tag -> viewModel.createTag(tag) },
        onUpdateTagOrder = { tags ->
            // ★注意: TaskListViewModel に updateTagOrder(tags: List<Tag>) 関数を追加してください
            viewModel.updateTagOrder(tags)
        }
    )
}

// Preview や テスト用に分離した UI コンポーネント
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListContent(
    tasksWithTags: List<TaskWithTags>,
    dynamicTags: List<Tag>,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTag: (Tag) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    val selectedTagIds = remember { mutableStateListOf<Long>() }
    var isAndSearch by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }

    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    val now = Instant.now().epochSecond

    val selectedTagsList by remember(dynamicTags, selectedTagIds.toList()) {
        derivedStateOf {
            dynamicTags.filter { selectedTagIds.contains(it.tagId) }
        }
    }

    // 完了済みフィルターとタグフィルター(AND/OR)を統合
    val filteredTasks by remember(tasksWithTags, isAndSearch, selectedTagIds.toList(), showCompleted) {
        derivedStateOf {
            val baseList = if (showCompleted) tasksWithTags
            else tasksWithTags.filter { it.task.completeState != "COMPLETED" }

            if (selectedTagIds.isEmpty()) {
                baseList
            } else {
                baseList.filter { taskWithTags ->
                    val taskTagIds = taskWithTags.tags.map { it.tagId }.toSet()
                    if (isAndSearch) {
                        // 【AND検索】選択中の "すべて" のタグが、タスクに含まれているか
                        selectedTagIds.all { it in taskTagIds }
                    } else {
                        // 【OR検索】選択中の "いずれか" のタグが、タスクに含まれているか
                        selectedTagIds.any { it in taskTagIds }
                    }
                }
            }
        }
    }

    val pastTasks = remember(filteredTasks) { filteredTasks.filter { it.task.endTime < now } }
    val upcomingTasks = remember(filteredTasks) { filteredTasks.filter { it.task.endTime >= now } }
    val listState = rememberLazyListState()

    // 初期状態で「過去/現在ライン」付近へスクロール
    LaunchedEffect(filteredTasks) {
        if (pastTasks.isNotEmpty()) listState.scrollToItem(pastTasks.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("予定・タスク一覧", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, tint = Color(0xFF1C1B1F), contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // --- タグフィルター ＆ AND/OR 切り替え ＆ 完了済み表示バー ---
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA)).padding(vertical = 10.dp)) {

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                    Text("タグで絞り込む", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    val textMeasurer = rememberTextMeasurer()
                    val density = LocalDensity.current

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // チップの左右に16dpずつ余白を確保する。
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
                                    // TagLabelと同じ、横8dpずつの余白 + 20dpアイコン +
                                    // （アイコンがあれば）6dpの間隔 + 太字13spのタグ名で幅を計算する。
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

                                // 選択中のタグは、1行に入り切らない場合でも必ず追加表示する。
                                selectedTagIds.forEach { defaultVisibleIds.add(it) }
                                dynamicTags.filter { it.tagId in defaultVisibleIds }
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                onDeleteTagRequest = { tagToDelete = it }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 新規作成プラスボタン
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .border(BorderStroke(1.dp, Color(0xFFE0E0E0)), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .clickable { showTagCreateDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "新規タグ作成", tint = Color(0xFF3C4043), modifier = Modifier.size(18.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showCompleted, onCheckedChange = { showCompleted = it })
                                    Text("完了済みを含める", fontSize = 12.sp)
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

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text("フィルター条件:", fontSize = 12.sp, color = Color.Gray)
                    }

                    Row(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFE0E0E0)).padding(2.dp)) {
                        val activeColor = Color.White
                        val inactiveColor = Color.Transparent
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(if (!isAndSearch) activeColor else inactiveColor)
                                .clickable { isAndSearch = false }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("いずれか (OR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isAndSearch) Color.Black else Color.Gray)
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(if (isAndSearch) activeColor else inactiveColor)
                                .clickable { isAndSearch = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("すべて含む (AND)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isAndSearch) Color.Black else Color.Gray)
                        }
                    }
                }
            }

            // --- タスクリスト本体表示 ---
            if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedTagIds.isEmpty()) "予定はありません" else "該当するタスクが見つかりません",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
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

                    // --- 過去/未来ライン ---
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text("過去のタスク", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
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
        TagCreateDialog(
            onDismissRequest = { showTagCreateDialog = false },
            onTagSave = { name, iconSource, color, _ ->
                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id
                }

                val newTag = Tag(tagId = 0L, name = name, color = color.toArgb(), icon = iconString)
                onCreateTag(newTag)

                showTagCreateDialog = false
            }
        )
    }
}

@Composable
private fun TaskCard(
    item: TaskWithTags,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onClick: () -> Unit
) {
    val task = item.task
    val isCompleted = task.completeState == "COMPLETED"

    val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
    val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())
    val baseColor = if (task.color == 0) Color(0xFF1A73E8) else Color(task.color)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF1F3F4) else Color(0xFFF8F9FA))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onToggleTaskCompletion(item) }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "ステータス変更",
                    tint = if (isCompleted) Color(0xFF34A853) else baseColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color.Gray else Color.Black,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${startDateTime.format(dateFormatter)} ${startDateTime.format(timeFormatter)} 〜 ${endDateTime.format(timeFormatter)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                if (item.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.forEach { tag ->
                            TagLabel(
                                tag = tag,
                                textSize = 10.sp,
                                isSelected = !isCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- ★ Compose Preview ---

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
        onCreateTag = {},
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
        onCreateTag = {},
        onUpdateTagOrder = {}
    )
}