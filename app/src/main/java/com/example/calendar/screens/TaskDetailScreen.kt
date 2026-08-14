package com.example.calendar.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.relation.TaskWithTags
import com.example.calendar.data.entity.ChecklistItem
import com.example.calendar.viewmodel.TaskDetailViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TaskDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit = {}
) {
    LaunchedEffect(taskId) {
        viewModel.loadTaskDetail(taskId)
    }

    val itemWithTags = viewModel.currentTaskWithTags
    val checklistItems = viewModel.checklistState
    val context = LocalContext.current

    if (itemWithTags == null) {
        Scaffold(containerColor = Color.White) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    TaskDetailContent(
        itemWithTags = itemWithTags,
        checklistItems = checklistItems,
        onNavigateBack = onNavigateBack,
        onDeleteTask = {
            viewModel.deleteTask(
                context = context,
                onSuccess = onNavigateBack
            )
        },
        onToggleTaskCompletion = {
            viewModel.toggleTaskCompletion(context)
        },
        onToggleChecklistItem = { index, checked -> viewModel.toggleChecklistItem(index, checked) },
        onNavigateToEditTask = { onNavigateToEditTask(taskId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailContent(
    itemWithTags: TaskWithTags,
    checklistItems: List<ChecklistItem>,
    onNavigateBack: () -> Unit,
    onDeleteTask: () -> Unit,
    onToggleTaskCompletion: () -> Unit,
    onToggleChecklistItem: (Int, Boolean) -> Unit,
    onNavigateToEditTask: () -> Unit
) {
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd（E） HH:mm", Locale.JAPANESE) }
    val task = itemWithTags.task
    val isCompleted = task.completeState == "COMPLETED"

    val mainTag = itemWithTags.tags.firstOrNull()
    val baseColor = if (mainTag != null) Color(mainTag.color) else (if (task.color == 0) Color(0xFF1A73E8) else Color(task.color))

    val startDateTime = remember(task.startTime) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
    }
    val endDateTime = remember(task.endTime) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())
    }

    val mainIcon = when (mainTag?.icon) {
        "Book" -> Icons.Default.Book
        "ErrorOutline" -> Icons.Default.ErrorOutline
        else -> Icons.Default.Bookmark
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("予定の詳細", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = Color.Black)
                    }
                },
                actions = {
                    // ★ ここにあった「編集」TextButtonを削除しました！
                    IconButton(onClick = onDeleteTask) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "削除", tint = Color.DarkGray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToEditTask,
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Edit, contentDescription = "編集") },
                text = { Text("編集する", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        },
        containerColor = Color(0xFFF9F9F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // --- 【1】未完了 / 完了にする トグルエリア ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(2.dp, if (isCompleted) Color(0xFF34A853) else Color.Gray, CircleShape)
                                .background(if (isCompleted) Color(0xFF34A853) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isCompleted) "完了" else "未完了",
                            color = if (isCompleted) Color(0xFF34A853) else Color(0xFF70757A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleTaskCompletion,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, Color(0xFF1A73E8))
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Undo else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF1A73E8)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "未完了にする" else "完了にする",
                            fontSize = 13.sp,
                            color = Color(0xFF1A73E8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- 【2】メイン情報カード ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(mainIcon, contentDescription = null, tint = baseColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = task.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (isCompleted) Color.Gray else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // タグセクション
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("タグ", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(48.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (itemWithTags.tags.isEmpty()) {
                                    Text("なし", fontSize = 14.sp, color = Color.LightGray)
                                } else {
                                    itemWithTags.tags.forEach { tag ->
                                        val currentTagColor = Color(tag.color)
                                        val currentTagIcon = when (tag.icon) {
                                            "Book" -> Icons.Default.Book
                                            "ErrorOutline" -> Icons.Default.ErrorOutline
                                            else -> Icons.Default.Bookmark
                                        }
                                        MockTagChip(
                                            text = tag.name,
                                            bgColor = currentTagColor.copy(alpha = 0.15f),
                                            textColor = currentTagColor,
                                            icon = currentTagIcon
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TimeDisplayRow(
                        label = "開始",
                        timeStr = if (task.isAllDay) startDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd（E） 終日", Locale.JAPANESE)) else startDateTime.format(timeFormatter)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeDisplayRow(
                        label = "終了",
                        timeStr = if (task.isAllDay) endDateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd（E） 終日", Locale.JAPANESE)) else endDateTime.format(timeFormatter)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // メモセクション
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("メモ", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(48.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (task.memo.isNullOrEmpty()) "メモはありません" else task.memo ?: "",
                                fontSize = 14.sp,
                                color = if (task.memo.isNullOrEmpty()) Color.LightGray else Color.Black,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // --- 【3】チェックリストカード ---
            if (checklistItems.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckBox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("チェックリスト", fontSize = 14.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))) {
                            checklistItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked -> onToggleChecklistItem(index, checked) },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = item.text,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.isChecked) Color.Gray else Color.Black
                                    )
                                }
                                if (index < checklistItems.size - 1) {
                                    HorizontalDivider(color = Color(0xFFE0E0E0))
                                }
                            }
                        }
                    }
                }
            }

            // --- 【4】詳細拡張属性リスト ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    val hasAttachment = !task.attachmentPath.isNullOrEmpty()
                    ExtensionRowMock(
                        icon = Icons.Default.AttachFile,
                        label = "添付ファイル",
                        content = if (hasAttachment) "ファイルを開く" else "なし",
                        isLink = hasAttachment,
                        onRowClick = {
                            if (hasAttachment) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(task.attachmentPath)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            }
                        }
                    )

                    val hasUrl = !task.url.isNullOrEmpty()
                    ExtensionRowMock(
                        icon = Icons.Default.Link,
                        label = "リンク・場所",
                        content = if (hasUrl) "リンクを開く" else "なし",
                        isLink = hasUrl,
                        onRowClick = {
                            if (hasUrl) {
                                try {
                                    val urlString = if (!task.url.startsWith("http://") && !task.url.startsWith("https://")) "https://${task.url}" else task.url
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            }
                        }
                    )

                    val hasLocation = !task.locationName.isNullOrBlank()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = hasLocation) {
                                val query = Uri.encode(task.locationName)

                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("geo:0,0?q=$query")
                                )

                                context.startActivity(intent)
                            },
                        verticalAlignment = Alignment.Top
                    ) {

                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            "場所",
                            fontSize = 14.sp,
                            modifier = Modifier.width(60.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = task.locationName ?: "なし",
                                fontSize = 14.sp,
                                color =
                                    if (hasLocation)
                                        Color(0xFF1A73E8)
                                    else
                                        Color.Gray,

                                fontWeight = FontWeight.Bold
                            )

                            if (!task.locationAddress.isNullOrBlank()) {

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = task.locationAddress!!,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    val daysLeft = if (endDateTime.isAfter(LocalDateTime.now(ZoneId.systemDefault()))) "実施中" else "期限終了"
                    ExtensionRowMock(Icons.Default.CalendarMonth, "ステータス期限", "目標設定時間：${endDateTime.format(timeFormatter)} ($daysLeft)", textColor = baseColor)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("表示カラー", fontSize = 14.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(baseColor))
                    }

                    ExtensionRowMock(
                        icon = Icons.Default.Settings,
                        label = "その他設定",
                        content = "自動ステータス変更(期限切れ即時完了)：${if (task.isAutoCompleted) "ON" else "OFF"}"
                    )
                }
            }

            // --- 【5】最下部：作成・更新メタデータ ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 64.dp, start = 4.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("タスク内部ID：${task.taskId}", fontSize = 12.sp, color = Color.Gray)
                Text("現在のステータスコード：${task.completeState}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// --- 画面内で使用する補助コンポーネント ---

@Composable
fun TimeDisplayRow(label: String, timeStr: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(48.dp))
        Text(text = timeStr, fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MockTagChip(
    text: String,
    bgColor: Color,
    textColor: Color,
    icon: ImageVector
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExtensionRowMock(
    icon: ImageVector,
    label: String,
    content: String,
    isLink: Boolean = false,
    textColor: Color = Color.Black,
    onRowClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isLink, onClick = onRowClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = Color.Black, modifier = Modifier.weight(1f))
        Text(
            text = content,
            fontSize = 13.sp,
            color = if (isLink) Color(0xFF1A73E8) else textColor,
            fontWeight = if (isLink) FontWeight.Bold else FontWeight.Normal
        )
    }
}