package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.components.TagIconBadge // ★ 新設された独立コンポーネントをインポート
import com.example.calendar.viewmodel.DateDetailViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
    dateMillis: Long, // カレンダーなどから渡されるミリ秒タイムスタンプ
    viewModel: DateDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit
) {
    // 1. ミリ秒タイムスタンプから LocalDate を復元
    val targetDate = remember(dateMillis) {
        Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    // 2. 画面が表示・変更されたタイミングで、ViewModel の抽出日ターゲットを更新
    LaunchedEffect(targetDate) {
        viewModel.setDate(targetDate)
    }

    // 3. ViewModel からリアルタイムにフィルタリングされたその日のタスク一覧を購読
    val dayTasks by viewModel.filteredTasks.collectAsState()

    val headerFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }
    val now = LocalDateTime.now()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = targetDate.format(headerFormatter), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        if (dayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("この日の予定はありません", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                items(dayTasks, key = { it.task.taskId }) { item ->
                    val task = item.task
                    val isCompleted = task.completeState == "COMPLETED"

                    val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
                    val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())

                    // 期限切れ判定
                    val isExpired = !isCompleted && endDateTime.isBefore(now)

                    // ベースカラーの決定
                    val firstTag = item.tags.firstOrNull()
                    val baseColor = if (firstTag != null) Color(firstTag.color) else (if (task.color == 0) Color(0xFF70757A) else Color(task.color))
                    val contentColor = if (isCompleted) Color(0xFF9AA0A6) else baseColor

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // --- 左側：時刻・タイムラインインジケータ ---
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(60.dp)
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = startDateTime.format(timeFormatter),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color.Gray else Color.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) Color(0xFF34A853) else if (isExpired) Color(0xFFD93025) else baseColor)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = endDateTime.format(timeFormatter),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // --- 右側：予定のカード内容 ---
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable { onNavigateToTaskDetail(task.taskId) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCompleted) Color(0xFFF1F3F4) else Color(0xFFF8F9FA)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // 完了チェックボタン
                                IconButton(
                                    onClick = { viewModel.toggleTaskCompletion(item) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "ステータス変更",
                                        tint = if (isCompleted) Color(0xFF34A853) else if (isExpired) Color(0xFFD93025) else contentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // 情報表示カラム
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

                                    // タグ情報一覧
                                    if (item.tags.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            item.tags.forEach { tag ->
                                                val tagColor = Color(tag.color)
                                                val tagBgColor = if (isCompleted) Color(0xFFE8EAED) else tagColor.copy(alpha = 0.12f)
                                                val hasIcon = !tag.icon.isNullOrBlank()

                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(tagBgColor)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // ★ 修正：分離されたファイルにある共通の「TagIconBadge」に完全置換！
                                                    if (hasIcon) {
                                                        TagIconBadge(
                                                            tag = tag,
                                                            size = 14.dp,     // チップ内に綺麗に収まるミニサイズ
                                                            iconSize = 10.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                    }

                                                    Text(
                                                        text = tag.name,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isCompleted) Color(0xFF80868B) else Color(0xFF3C4043)
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "通知あり",
                                                tint = Color(0xFF5F6368),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // 3. メモ部分
                                    if (!task.memo.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = task.memo ?: "",
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