package com.foxdog.strucalendar.screens

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.PushPin
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
import com.foxdog.strucalendar.components.TagLabel
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.ui.bounceClick
import com.foxdog.strucalendar.ui.theme.calendarColors
import com.foxdog.strucalendar.viewmodel.DateDetailViewModel
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
    val holidayName by viewModel.holidayName.collectAsState()

    DateDetailContent(
        targetDate = targetDate,
        dateMillis = dateMillis,
        dayTasks = dayTasks,
        holidayName = holidayName,
        onNavigateBack = onNavigateBack,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onNavigateToCreateTask = onNavigateToCreateTask,
        onToggleTaskCompletion = { viewModel.toggleTaskCompletion(it) },
        onTogglePin = { viewModel.togglePin(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailContent(
    targetDate: LocalDate,
    dateMillis: Long,
    dayTasks: List<TaskWithTags>,
    holidayName: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit,
    onNavigateToCreateTask: (Long) -> Unit,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onTogglePin: (TaskWithTags) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    val headerFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }
    val now = LocalDateTime.now()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = targetDate.format(headerFormatter), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorScheme.surface)
            )
        },
        floatingActionButton = {
            // ★ bounceClick適用：ExtendedFloatingActionButtonをBoxで置き換え
            // 1. FAB（作成ボタン）の正しいコード
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.primary)
                    // ★ 波紋あり、角丸四角形（16.dp）で広く広がる
                    .bounceClick(showWave = true, isWaveCircle = false, waveCornerRadius = 16.dp) {
                        onNavigateToCreateTask(dateMillis)
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "作成", tint = colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("作成", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colorScheme.onPrimary)
                }
            }
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->
        if (dayTasks.isEmpty() && holidayName == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("この日の予定はありません", color = colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                if (holidayName != null) {
                    item(key = "holiday") {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Spacer(modifier = Modifier.width(64.dp))

                            Spacer(modifier = Modifier.width(8.dp))

                            Card(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = calColors.sunday.copy(alpha = 0.08f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "祝日",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = calColors.sunday,
                                        modifier = Modifier
                                            .background(calColors.sunday.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = holidayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                items(dayTasks, key = { it.task.taskId }) { item ->
                    val task = item.task
                    val isCompleted = task.completeState == "COMPLETED"
                    val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
                    val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())

                    val isExpired = !isCompleted && !task.isAllDay && endDateTime.isBefore(now)

                    val firstTag = item.tags.firstOrNull()
                    val baseColor = if (firstTag != null) Color(firstTag.color) else (if (task.color == 0) colorScheme.primary else Color(task.color))
                    val contentColor = if (isCompleted) colorScheme.onSurfaceVariant else baseColor

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(64.dp).padding(top = 8.dp)
                        ) {
                            if (task.isAllDay) {
                                Text(text = "終日", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) colorScheme.onSurfaceVariant else colorScheme.primary)
                            } else {
                                Text(text = startDateTime.format(timeFormatter), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCompleted) colorScheme.onSurfaceVariant else colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isCompleted) calColors.success else if (isExpired) colorScheme.error else baseColor))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = endDateTime.format(timeFormatter), fontSize = 11.sp, color = colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Card(
                            modifier = Modifier.weight(1f).fillMaxWidth().clickable { onNavigateToTaskDetail(task.taskId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) colorScheme.surfaceVariant else colorScheme.surfaceContainerHigh
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.5.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 2.dp)
                                        .bounceClick(showWave = true, isWaveCircle = true, waveExpansionSize = 12.dp) {
                                            onToggleTaskCompletion(item)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "ステータス変更",
                                        tint = if (isCompleted) calColors.success else if (isExpired) colorScheme.error else contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isCompleted || isExpired) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isCompleted) calColors.successContainer else colorScheme.errorContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (isCompleted) "完了" else "期限切れ",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCompleted) calColors.onSuccessContainer else colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = task.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCompleted) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (task.recurrenceGroupId != null) {
                                            Icon(
                                                imageVector = Icons.Default.Repeat,
                                                contentDescription = "繰り返しタスク",
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        IconButton(
                                            onClick = { onTogglePin(item) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (task.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = if (task.isPinned) "ピン止めを解除" else "ピン止め",
                                                tint = if (task.isPinned) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

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
                                                Icon(imageVector = Icons.Default.Notifications, contentDescription = "通知あり", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    if (!task.memo.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = task.memo,
                                            fontSize = 12.sp,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Compose Preview（変更なし） ---

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
        holidayName = "元日",
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