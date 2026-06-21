package com.example.calendar.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.viewmodel.TaskDetailViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TaskDetailViewModel,
    onNavigateBack: () -> Unit
) {
    // 画面起動時に該当タスクの情報をロード
    LaunchedEffect(taskId) {
        viewModel.loadTaskDetail(taskId)
    }

    val itemWithTags = viewModel.currentTaskWithTags
    val checklistItems = viewModel.checklistState
    val timeFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd（E） HH:mm", Locale.JAPANESE) }

    // ロード中のフォールバック処理
    if (itemWithTags == null) {
        Scaffold(containerColor = Color.White) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val task = itemWithTags.task
    val isCompleted = task.completeState == "COMPLETED"

    // ★ 修正：タグがある場合は第一タグの色、なければタスクカラーを基準にする
    val mainTag = itemWithTags.tags.firstOrNull()
    val baseColor = if (mainTag != null) Color(mainTag.color) else (if (task.color == 0) Color(0xFF1A73E8) else Color(task.color))

    // Long(EpochSecond) を安全に LocalDateTime に相互変換するヘルパー
    val startDateTime = remember(task.startTime) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneOffset.UTC)
    }
    val endDateTime = remember(task.endTime) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneOffset.UTC)
    }

    // メイン情報カード用のアイコントグル
    val mainIcon = when (mainTag?.icon) {
        "Book" -> Icons.Default.Book
        "ErrorOutline" -> Icons.Default.ErrorOutline
        else -> Icons.Default.Bookmark
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("予定の詳細", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteTask(onSuccess = onNavigateBack) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "削除")
                    }
                    IconButton(onClick = { /* その他メニュー */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                        onClick = { viewModel.toggleTaskCompletion() },
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // ★ 修正：メインアイコンと色を連動
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
                        Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
                                    // ★ 修正：ハードコードを廃止し、本物のカラーとアイコンを割り当て
                                    itemWithTags.tags.forEach { tag ->
                                        val currentTagColor = Color(tag.color)
                                        val currentTagIcon = when (tag.icon) {
                                            "Book" -> Icons.Default.Book
                                            "ErrorOutline" -> Icons.Default.ErrorOutline
                                            else -> Icons.Default.Bookmark
                                        }
                                        MockTagChip(
                                            text = tag.name,
                                            bgColor = currentTagColor.copy(alpha = 0.15f), // 薄い背景
                                            textColor = currentTagColor,                   // クッキリした文字色
                                            icon = currentTagIcon                          // 該当アイコン
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TimeRowMock(label = "開始", timeStr = startDateTime.format(timeFormatter))
                    Spacer(modifier = Modifier.height(12.dp))
                    TimeRowMock(label = "終了", timeStr = endDateTime.format(timeFormatter))

                    Spacer(modifier = Modifier.height(20.dp))
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
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (task.memo.isNullOrEmpty()) "メモはありません" else task.memo ?: "",
                                    fontSize = 14.sp,
                                    color = if (task.memo.isNullOrEmpty()) Color.LightGray else Color.Black,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // --- 【3】チェックリストカード ---
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
                                    onCheckedChange = { checked -> viewModel.toggleChecklistItem(index, checked) },
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

                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DragHandle, contentDescription = "並び替え", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "削除", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < checklistItems.size - 1) {
                                HorizontalDivider(color = Color(0xFFE0E0E0))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* 項目追加アクション */ }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("項目を追加", fontSize = 14.sp, color = Color.Gray)
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
                    ExtensionRowMock(Icons.Default.AttachFile, "添付ファイル", task.attachmentPath ?: "なし", isLink = task.attachmentPath != null)
                    ExtensionRowMock(Icons.Default.Link, "URL", task.url ?: "なし", isLink = task.url != null)

                    val locationString = if (task.latitude != null && task.longitude != null) {
                        "緯度: ${task.latitude}, 経度: ${task.longitude}"
                    } else {
                        "なし"
                    }
                    ExtensionRowMock(Icons.Default.LocationOn, "位置情報", locationString, isLink = task.latitude != null)

                    val daysLeft = if (endDateTime.isAfter(LocalDateTime.now())) "実施中" else "期限終了"
                    ExtensionRowMock(Icons.Default.CalendarMonth, "ステータス期限", "目標設定時間：${endDateTime.format(timeFormatter)} ($daysLeft)", textColor = baseColor)

                    // 色インジケータ
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("表示カラー", fontSize = 14.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(baseColor))
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
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
                    .padding(top = 12.dp, bottom = 12.dp, start = 4.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("タスク内部ID：${task.taskId}", fontSize = 12.sp, color = Color.Gray)
                Text("現在のステータスコード：${task.completeState}", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

data class ChecklistItem(val id: Int, val text: String, val isChecked: Boolean)

@Composable
fun MockTagChip(text: String, bgColor: Color, textColor: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp)) // カレンダーより少し大きめの詳細表示サイズ
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) // 太字でハッキリ化
    }
}

@Composable
fun TimeRowMock(label: String, timeStr: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(48.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = timeStr, fontSize = 14.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.DarkGray)
            }
        }
    }
}

@Composable
fun ExtensionRowMock(icon: ImageVector, label: String, content: String, isLink: Boolean = false, textColor: Color = Color.Black) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(content, fontSize = 14.sp, color = if (isLink) Color(0xFF1A73E8) else textColor, fontWeight = if (isLink) FontWeight.Medium else FontWeight.Normal)
        }
        Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
    }
}