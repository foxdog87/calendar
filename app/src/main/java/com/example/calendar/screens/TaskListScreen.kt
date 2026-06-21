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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
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

    // ★ 永続化：ViewModelから本物のタグ一覧をリアルタイムで取得する（Flow/StateFlowの監視）
    val dynamicTags by viewModel.allTags.collectAsState()

    // 選択されたタグが常に前（先頭）に来るように並び替えたリスト
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

                    // 修正点1: take(3)による個数制限を完全に撤廃し、FlowRowのmaxLinesのみでサイズ・行数制御を行う
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

                        // 常時すべてのタグをFlowRowに流し込みます（制限はFlowRow側で行うため）
                        sortedTags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)

                            // 修正点2: 色のロジックを修正
                            // 選択時: タグ本来の色を背景に、文字はダークグレー。
                            // 未選択時: 背景は薄いグレー（または白）、文字とアイコンにタグ本来の色を乗せる
                            val chipBgColor = if (isSelected) Color(tag.color) else Color(0xFFF1F3F4)
                            val chipTextColor = if (isSelected) Color(0xFF1C1B1F) else Color(0xFF3C4043)
                            val contentAccentColor = if (isSelected) Color(0xFF1C1B1F) else Color(tag.color)

                            val borderStroke = if (isSelected) BorderStroke(1.5.dp, Color(tag.color)) else BorderStroke(1.dp, Color(0xFFE0E0E0))
                            val tagIcon = when (tag.icon) {
                                "Book" -> Icons.Default.Book
                                "ErrorOutline" -> Icons.Default.ErrorOutline
                                "" -> null
                                else -> Icons.Default.Bookmark
                            }

                            Surface(
                                color = chipBgColor,
                                shape = RoundedCornerShape(8.dp),
                                border = borderStroke,
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(
                                        onClick = { if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag) },
                                        onLongClick = { tagToDelete = tag }
                                    )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (tagIcon != null) {
                                        Icon(
                                            imageVector = tagIcon,
                                            contentDescription = null,
                                            tint = contentAccentColor, // 未選択時はタグ本来の色になるように修正
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = tag.name,
                                        color = chipTextColor, // 未選択時は可読性の高い文字色
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold // 色をハッキリ見せるためBoldに
                                    )
                                }
                            }
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
                        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneOffset.UTC)
                        val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneOffset.UTC)
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
                                                val cardTagIcon = when (tag.icon) {
                                                    "Book" -> Icons.Default.Book
                                                    "ErrorOutline" -> Icons.Default.ErrorOutline
                                                    "" -> null
                                                    else -> Icons.Default.Bookmark
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(tag.color).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (cardTagIcon != null) {
                                                            Icon(
                                                                imageVector = cardTagIcon,
                                                                contentDescription = null,
                                                                tint = Color(tag.color),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                        }
                                                        Text(text = tag.name, color = Color(tag.color), fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
                            // ★ 永続化：ViewModel経由でデータベースから完全削除
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
                // ★ 修正：文字列のブっこ抜きを廃止し、定義した型安全なID（"fire"や"book"など）を保存する
                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id // .imageVector.name から変更
                    is TagIconSource.CustomUri -> iconSource.uri.toString()
                }

                // ★ 永続化：RoomがIDを自動生成するためtagIdは0L固定でインスタンスを作成
                // （color.toArgb() でInt型として綺麗に保存されているのでそのまま使えます）
                val newTag = Tag(tagId = 0L, name = name, color = color.toArgb(), icon = iconString)

                // ★ 永続化：ViewModelを通じてデータベースへ保存
                viewModel.createTag(newTag)

                // 選択中リストへの追加処理（UI上で即座にフィルター有効化するため）
                selectedTags.add(newTag)
                showTagCreateDialog = false
            }
        )
    }
}