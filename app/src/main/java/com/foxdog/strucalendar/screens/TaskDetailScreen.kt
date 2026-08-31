package com.foxdog.strucalendar.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.TagCustomField
import com.foxdog.strucalendar.data.entity.Template
import com.foxdog.strucalendar.ui.theme.calendarColors
import com.foxdog.strucalendar.viewmodel.TaskDetailViewModel
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
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    if (itemWithTags == null) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    TaskDetailContent(
        itemWithTags = itemWithTags,
        checklistItems = checklistItems,
        template = viewModel.currentTemplate,
        confirmBeforeDelete = settings.confirmBeforeDeleteTask,
        onNavigateBack = onNavigateBack,
        customFields = viewModel.currentCustomFields,
        customFieldValues = viewModel.currentCustomFieldValues,
        onDeleteTask = {
            viewModel.deleteTask(
                context = context,
                onSuccess = onNavigateBack
            )
        },
        onDeleteRecurrenceGroup = {
            viewModel.deleteRecurrenceGroup(
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
    template: Template? = null,
    customFields: List<TagCustomField>,
    customFieldValues: Map<Long, String>,
    confirmBeforeDelete: Boolean = true,
    onNavigateBack: () -> Unit,
    onDeleteTask: () -> Unit,
    onDeleteRecurrenceGroup: () -> Unit = {},
    onToggleTaskCompletion: () -> Unit,
    onToggleChecklistItem: (Int, Boolean) -> Unit,
    onNavigateToEditTask: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors
    val timeFormatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd（E） HH:mm", Locale.JAPANESE) }
    val task = itemWithTags.task
    val isCompleted = task.completeState == "COMPLETED"
    val isRecurring = task.recurrenceGroupId != null

    var showDeleteOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val mainTag = itemWithTags.tags.firstOrNull()
    val baseColor = if (mainTag != null) Color(mainTag.color) else (if (task.color == 0) colorScheme.primary else Color(task.color))

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
                title = { Text("予定の詳細", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            when {
                                isRecurring -> showDeleteOptionsDialog = true
                                confirmBeforeDelete -> showDeleteConfirmDialog = true
                                else -> onDeleteTask()
                            }
                        },
                        modifier = Modifier.size(44.dp) // タップ領域も拡大
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "削除",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp) // 24dp→26dpに拡大
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToEditTask,
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Edit, contentDescription = "編集") },
                text = { Text("編集する", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        },
        containerColor = colorScheme.background
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
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
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
                                .border(2.dp, if (isCompleted) calColors.success else colorScheme.onSurfaceVariant, CircleShape)
                                .background(if (isCompleted) calColors.success else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isCompleted) "完了" else "未完了",
                            color = if (isCompleted) calColors.success else colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleTaskCompletion,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Undo else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "未完了にする" else "完了にする",
                            fontSize = 13.sp,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // --- 【2】メイン情報カード ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
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
                            color = if (isCompleted) colorScheme.onSurfaceVariant else colorScheme.onSurface
                        )
                        if (isRecurring) { // 繰り返しタスクのアイコン表示
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "繰り返しタスク",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))

                    // タグセクション
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("タグ", fontSize = 14.sp, color = colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (itemWithTags.tags.isEmpty()) {
                                    Text("なし", fontSize = 14.sp, color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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
                    HorizontalDivider(color = colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))

                    // メモセクション
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("メモ", fontSize = 14.sp, color = colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = if (task.memo.isNullOrEmpty()) "メモはありません" else task.memo ?: "",
                                fontSize = 14.sp,
                                color = if (task.memo.isNullOrEmpty()) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else colorScheme.onSurface,
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
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckBox, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("チェックリスト", fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(modifier = Modifier.border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))) {
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
                                        color = if (item.isChecked) colorScheme.onSurfaceVariant else colorScheme.onSurface
                                    )
                                }
                                if (index < checklistItems.size - 1) {
                                    HorizontalDivider(color = colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            // --- カスタム項目 ---
            if (customFields.isNotEmpty()) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "カスタム項目",
                            fontSize = 14.sp,
                            color = colorScheme.onSurface
                        )
                    }

                    customFields.forEach { field ->

                        val value =
                            customFieldValues[field.fieldId]

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {

                            Text(
                                text = field.fieldName,
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(100.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = value?.takeIf { it.isNotBlank() } ?: "未入力",
                                fontSize = 14.sp,
                                color =
                                    if (value.isNullOrBlank())
                                        colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else
                                        colorScheme.onSurface,
                                fontWeight =
                                    if (value.isNullOrBlank())
                                        FontWeight.Normal
                                    else
                                        FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = colorScheme.outline)
            }

            // --- 【4】詳細拡張属性リスト ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
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
                        label = "リンク",
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
                                try {
                                    val query = Uri.encode(task.locationName)
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("geo:0,0?q=$query")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "地図アプリが見つかりませんでした", Toast.LENGTH_SHORT).show()
                                }
                            },
                        verticalAlignment = Alignment.Top
                    ) {


                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            "場所",
                            fontSize = 14.sp,
                            color = colorScheme.onSurface,
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
                                        colorScheme.primary
                                    else
                                        colorScheme.onSurfaceVariant,

                                fontWeight = FontWeight.Bold
                            )

                            if (!task.locationAddress.isNullOrBlank()) {

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = task.locationAddress!!,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }



                    val daysLeft = if (endDateTime.isAfter(LocalDateTime.now(ZoneId.systemDefault()))) "実施中" else "期限終了"
                    ExtensionRowMock(Icons.Default.CalendarMonth, "ステータス期限", "目標設定時間：${endDateTime.format(timeFormatter)} ($daysLeft)", textColor = baseColor)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("表示カラー", fontSize = 14.sp, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
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
                Text("タスク内部ID：${task.taskId}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text("現在のステータスコード：${task.completeState}", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
        }
    }

    // 繰り返しタスクの削除方法選択ダイアログ
    if (showDeleteOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteOptionsDialog = false },
            title = { Text("繰り返し予定の削除", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "この予定は繰り返し設定されています。削除方法を選んでください。",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    DeleteOptionRow(
                        title = "このタスクのみを削除する",
                        subtitle = "この回の予定だけが削除されます",
                        icon = Icons.Default.Delete,
                        tint = colorScheme.error,
                        onClick = {
                            showDeleteOptionsDialog = false
                            onDeleteTask()
                        }
                    )

                    HorizontalDivider(color = colorScheme.outline)

                    DeleteOptionRow(
                        title = "この繰り返し予定をすべて削除する",
                        subtitle = "過去・未来を含む、この繰り返し設定の全ての回が削除されます",
                        icon = Icons.Default.DeleteSweep,
                        tint = colorScheme.error,
                        onClick = {
                            showDeleteOptionsDialog = false
                            onDeleteRecurrenceGroup()
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDeleteOptionsDialog = false }) { Text("閉じる") }
            }
        )
    }
    // 非繰り返しタスク用の削除確認ダイアログ
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("予定を削除しますか？", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "「${task.title}」を削除します。この操作は取り消せません。\nこの表示は設定画面から無効にできます。",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteTask()
                    }
                ) {
                    Text("削除する", color = colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("キャンセル") }
            }
        )
    }
}



@Composable
fun TimeDisplayRow(label: String, timeStr: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AccessTime, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, color = colorScheme.onSurfaceVariant, modifier = Modifier.width(48.dp))
        Text(text = timeStr, fontSize = 14.sp, color = colorScheme.onSurface, fontWeight = FontWeight.Medium)
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
    textColor: Color? = null,
    onRowClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val resolvedTextColor = textColor ?: colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isLink, onClick = onRowClick),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(84.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = content,
            fontSize = 13.sp,
            color = if (isLink) colorScheme.primary else resolvedTextColor,
            fontWeight = if (isLink) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            softWrap = true
        )
    }
}


@Composable
private fun DeleteOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}