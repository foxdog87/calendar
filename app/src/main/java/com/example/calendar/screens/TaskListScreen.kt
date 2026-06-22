package com.example.calendar.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.entity.TaskWithTags
import com.example.calendar.data.entity.Tag
import com.example.calendar.viewmodel.TaskListViewModel
import com.example.calendar.components.TagCreateDialog
import com.example.calendar.components.TagIconSource
import com.example.calendar.components.TagLabel // ★ 共通コンポーネントを使用
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit
) {
    val tasksWithTags by viewModel.allTasksWithTags.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    val selectedTags = remember { mutableStateListOf<Tag>() }
    var isAndSearch by remember { mutableStateOf(false) }

    var tagToDelete by remember { mutableStateOf<Tag?>(null) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }

    val dynamicTags by viewModel.allTags.collectAsState()

    val sortedTags = remember(dynamicTags.toList(), selectedTags.toList()) {
        dynamicTags.sortedWith(compareByDescending { selectedTags.contains(it) })
    }

    val filteredTasks = remember(tasksWithTags, selectedTags.toList(), isAndSearch) {
        if (selectedTags.isEmpty()) {
            tasksWithTags
        } else {
            tasksWithTags.filter { taskWithTags ->
                val taskTagIds = taskWithTags.tags.map { it.tagId }.toSet()
                val selectedTagIds = selectedTags.map { it.tagId }
                if (isAndSearch) {
                    taskTagIds.containsAll(selectedTagIds)
                } else {
                    selectedTagIds.any { it in taskTagIds }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("予定・タスク一覧", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "戻る") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // --- タグフィルター ＆ AND/OR 切り替えバー ---
            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA)).padding(vertical = 10.dp)) {

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxLines = if (isTagFolderExpanded) Int.MAX_VALUE else 2
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

                        // すべてのタグを流し込み
                        sortedTags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)

                            // ★ 修正：フィルターエリアのチップを完全に新 TagLabel に統合
                            TagLabel(
                                tag = tag,
                                isSelected = isSelected,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = { if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag) },
                                        onLongClick = { tagToDelete = tag }
                                    )
                            )
                        }
                    }

                    if (dynamicTags.size > 2) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = { isTagFolderExpanded = !isTagFolderExpanded },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(text = if (isTagFolderExpanded) "閉じる ▲" else "さらに表示 ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (!isAndSearch) activeColor else inactiveColor).clickable { isAndSearch = false }.padding(horizontal = 10.dp, vertical = 4.dp)) { Text("いずれか (OR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isAndSearch) Color.Black else Color.Gray) }
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (isAndSearch) activeColor else inactiveColor).clickable { isAndSearch = true }.padding(horizontal = 10.dp, vertical = 4.dp)) { Text("すべて含む (AND)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isAndSearch) Color.Black else Color.Gray) }
                    }
                }
            }

            // --- タスクリスト本体表示 ---
            if (filteredTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if (selectedTags.isEmpty()) "予定はありません" else "該当するタスクが見つかりません", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                ) {
                    items(filteredTasks, key = { it.task.taskId }) { item ->
                        val task = item.task
                        val isCompleted = task.completeState == "COMPLETED"

                        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
                        val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())
                        val baseColor = if (task.color == 0) Color(0xFF1A73E8) else Color(task.color)

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToTaskDetail(task.taskId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF1F3F4) else Color(0xFFF8F9FA))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { coroutineScope.launch { viewModel.toggleTaskCompletion(item) } }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = "ステータス変更", tint = if (isCompleted) Color(0xFF34A853) else baseColor, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = task.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) Color.Gray else Color.Black, textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${startDateTime.format(dateFormatter)} ${startDateTime.format(timeFormatter)} 〜 ${endDateTime.format(timeFormatter)}", fontSize = 12.sp, color = Color.Gray)

                                    if (item.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            item.tags.forEach { tag ->
                                                // ★ 修正：カード内の表示も共通の TagLabel に完全置換
                                                TagLabel(
                                                    tag = tag,
                                                    textSize = 10.sp,
                                                    isSelected = !isCompleted // 完了時は少し控えめな未選択カラー風にするか、そのままtrueでもOK
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
                            selectedTags.remove(target)
                            viewModel.deleteTag(target)
                        }
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("削除する") }
            }
        )
    }

    if (showTagCreateDialog) {
        TagCreateDialog(
            onDismissRequest = { showTagCreateDialog = false },
            onTagSave = { name, iconSource, color, _ ->
                // ★ 修正：when式での分岐はDialogからのデータ受け取りのみ。すでにアイコン自体の判定whenは排除されています。
                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id
                }

                val newTag = Tag(tagId = 0L, name = name, color = color.toArgb(), icon = iconString)
                viewModel.createTag(newTag)

                selectedTags.add(newTag)
                showTagCreateDialog = false
            }
        )
    }
}