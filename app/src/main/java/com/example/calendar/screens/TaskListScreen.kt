package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.entity.TaskWithTags
import java.time.format.DateTimeFormatter

@Composable
fun TaskListScreen(
    allTasksWithTags: List<TaskWithTags> = emptyList(),
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit // ★ここを Int から Long に変更！
) {
    // 1. ステータス（completeState）に基づいて未完了・完了済みをフィルタリング
    val uncompletedTasks = allTasksWithTags.filter { it.task.completeState != "COMPLETED" }
    val completedTasks = allTasksWithTags.filter { it.task.completeState == "COMPLETED" }


    // ★熟考：初期表示件数をセーブし、1画面あたりのタスク数を減らす（チャッピーでのワイヤー設計思想）
    var visibleUncompletedCount by remember { mutableStateOf(3) }
    var visibleCompletedCount by remember { mutableStateOf(3) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)), // カレンダー画面と統一したノイズのない背景色
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ========== 未完了セクション ==========
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "未完了 (${uncompletedTasks.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )
                Text(text = "期限が近い順 ∨", fontSize = 12.sp, color = Color(0xFF70757A))
            }
        }

        // 制限された現在の表示件数分だけリスト化
// 制限された現在の表示件数分だけリスト化
        items(uncompletedTasks.take(visibleUncompletedCount), key = { it.task.taskId }) { item ->
            // ★ .toLong() をつけて Long 型に変換して渡す
            TaskRowItem(item = item, onClick = { onNavigateToTaskDetail(item.task.taskId.toLong()) })
        }

        // ★熟考：さらにタスクが残っている場合のみ「さらに表示」を出現させ、押すと6件ずつ追加ロード
        if (uncompletedTasks.size > visibleUncompletedCount) {
            item {
                Button(
                    onClick = { visibleUncompletedCount += 6 }, // 6タスク追加
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "さらに表示", color = Color(0xFF5F6368), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ========== 完了済みセクション ==========
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "完了済み (${completedTasks.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F)
            )
        }

        items(completedTasks.take(visibleCompletedCount), key = { it.task.taskId }) { item ->
            // ★ こちらも同様に .toLong() をつけて渡す
            TaskRowItem(item = item, onClick = { onNavigateToTaskDetail(item.task.taskId.toLong()) })
        }

        // 完了済み側の「さらに表示」（同様に6件ずつロード）
        if (completedTasks.size > visibleCompletedCount) {
            item {
                Button(
                    onClick = { visibleCompletedCount += 6 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "さらに表示", color = Color(0xFF5F6368), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRowItem(item: TaskWithTags, onClick: () -> Unit) {
    val task = item.task
    val isCompleted = task.completeState == "COMPLETED"
    val timeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E) HH:mm")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ★熟考（モノクロ対応）：色の値が 0 （指定なし）の場合は、引き締まったモダングレーを適用
            val barColor = if (task.color == 0) Color(0xFF9AA0A6) else Color(task.color)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp) // スタイリッシュに統一されたバーの高さ
                    .background(barColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ★熟考：未完了の丸アイコンを、浮かないように他の文字と同じ黒（ダークグレー）に統一
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) Color(0xFF34A853) else Color(0xFF1C1B1F), // 他と同じ黒（1C1B1F）
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) Color(0xFF9AA0A6) else Color(0xFF1C1B1F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // ★表記の揺れを排除：startTime構造から綺麗に時刻をフォーマットして表示
                    Text(
                        text = "時刻 ${task.startTime.format(timeFormatter)}",
                        fontSize = 12.sp,
                        color = Color(0xFF70757A)
                    )
                }
            }
        }
    }
}