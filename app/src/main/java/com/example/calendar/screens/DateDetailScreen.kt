package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.components.TagLabel
import com.example.calendar.data.entity.Tag
import com.example.calendar.data.entity.Task
import com.example.calendar.data.relation.TaskWithTags
import com.example.calendar.viewmodel.DateDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
    dateMillis: Long,
    viewModel: DateDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToCreateTask: (Long) -> Unit = {}
) {
    val targetDate = remember(dateMillis) {
        Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    LaunchedEffect(targetDate) {
        viewModel.setDate(targetDate)
    }

    val dayTasks by viewModel.filteredTasks.collectAsState()

    DateDetailContent(
        targetDate = targetDate,
        dateMillis = dateMillis,
        dayTasks = dayTasks,
        onNavigateBack = onNavigateBack,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onNavigateToCreateTask = onNavigateToCreateTask,
        onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailContent(
    targetDate: LocalDate,
    dateMillis: Long,
    dayTasks: List<TaskWithTags>,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToCreateTask: (Long) -> Unit,
    onToggleTaskCompletion: (TaskWithTags) -> Unit
) {
    val headerFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }
    val now = LocalDateTime.now()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                // ★ タイトル文字色を黒に指定
                title = { Text(text = targetDate.format(headerFormatter), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToCreateTask(dateMillis) },
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "作成") },
                text = { Text("作成", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        if (dayTasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("この日の予定はありません", color = Color(0xFF70757A), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp) // FABとかぶらないように下部余白を確保
            ) {
                items(dayTasks, key = { it.task.taskId }) { item ->
                    val task = item.task
                    val isCompleted = task.completeState == "COMPLETED"
                    val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
                    val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())

                    // 期限切れ判定
                    val isExpired = !isCompleted && !task.isAllDay && endDateTime.isBefore(now)

                    val firstTag = item.tags.firstOrNull()
                    val baseColor = if (firstTag != null) Color(firstTag.color) else (if (task.color == 0) Color(0xFF4285F4) else Color(task.color))
                    val contentColor = if (isCompleted) Color(0xFF9AA0A6) else baseColor

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        // 左側：時間インジケーター
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(64.dp).padding(top = 8.dp)
                        ) {
                            if (task.isAllDay) {
                                Text(text = "終日", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) Color.Gray else Color(0xFF1A73E8))
                            } else {
                                Text(text = startDateTime.format(timeFormatter), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) Color.Gray else Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isCompleted) Color(0xFF34A853) else if (isExpired) Color(0xFFD93025) else baseColor))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = endDateTime.format(timeFormatter), fontSize = 11.sp, color = Color(0xFF70757A))
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 右側：タスクカード
                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth().clickable { onNavigateToTaskDetail(task.taskId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isCompleted) Color(0xFFF1F3F4) else Color(0xFFF8F9FA)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                                IconButton(
                                    onClick = { onToggleTaskCompletion(item) },
                                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "ステータス変更",
                                        tint = if (isCompleted) Color(0xFF34A853) else if (isExpired) Color(0xFFD93025) else contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isCompleted || isExpired) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isCompleted) Color(0xFFE6F4EA) else Color(0xFFFCE8E6),
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (isCompleted) "完了" else "期限切れ",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCompleted) Color(0xFF137333) else Color(0xFFC5221F),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = task.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) Color(0xFF80868B) else Color.Black,
                                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // ★ リファクタリング：remindMinutes != null を reminders.isNotEmpty() に変更
                                    val hasReminder = task.reminderType != null || task.reminderOffsetMinutes != null

                                    if (item.tags.isNotEmpty() || hasReminder) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            item.tags.forEach { tag ->
                                                TagLabel(tag = tag, textSize = 10.sp, isSelected = !isCompleted)
                                            }
                                            if (hasReminder) {
                                                Icon(imageVector = Icons.Default.Notifications, contentDescription = "通知あり", tint = Color(0xFF5F6368), modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    if (!task.memo.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = task.memo,
                                            fontSize = 12.sp,
                                            color = if (isCompleted) Color(0xFF9AA0A6) else Color(0xFF5F6368),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 16.sp
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

// --- ★ Compose Preview ---

@Preview(showBackground = true, name = "予定あり表示")
@Composable
fun DateDetailContentPreview() {
    val dummyTags = listOf(
        Tag(tagId = 1L, name = "重要", color = Color(0xFFE53935).toArgb(), icon = null),
        Tag(tagId = 2L, name = "仕事", color = Color(0xFF1E88E5).toArgb(), icon = null)
    )

    val dummyTasks = listOf(
        TaskWithTags(
            task = Task(
                taskId = 1L,
                title = "チームミーティング",
                startTime = Instant.now().epochSecond,
                endTime = Instant.now().plusSeconds(3600).epochSecond,
                memo = "議題：プロジェクトのロードマップについて議論します。",
                color = Color(0xFF4285F4).toArgb(),
                attachmentPath = "",
                url = "",
                locationName = "筑波大学",
                locationAddress = "茨城県つくば市天王台1-1-1",
                isAutoCompleted = false,
                completeState = "INCOMPLETE",
                // ★ 修正：新しいプロパティに置き換え
                reminderType = "BEFORE",
                reminderOffsetMinutes = 10,
                reminderDayOffset = null,
                reminderHour = null,
                reminderMinute = null,
                dayCountTarget = null,
                templateId = null,
                isAllDay = false
            ),
            tags = dummyTags
        )
    )

    DateDetailContent(
        targetDate = LocalDate.now(),
        dateMillis = System.currentTimeMillis(),
        dayTasks = dummyTasks,
        onNavigateBack = {},
        onNavigateToTaskDetail = {},
        onNavigateToCreateTask = {},
        onToggleTaskCompletion = {}
    )
}

@Preview(showBackground = true, name = "予定なし (Empty) 表示")
@Composable
fun DateDetailContentEmptyPreview() {
    DateDetailContent(
        targetDate = LocalDate.now(),
        dateMillis = System.currentTimeMillis(),
        dayTasks = emptyList(),
        onNavigateBack = {},
        onNavigateToTaskDetail = {},
        onNavigateToCreateTask = {},
        onToggleTaskCompletion = {}
    )
}