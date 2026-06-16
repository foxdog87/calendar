package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// ==========================================
// ドメイン定義（S2専用のタイムライン表示用データ構造）
// ==========================================
data class TimelineTaskItem(
    val id: Long,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val categoryColor: Color,
    val memo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDetailScreen(
    dateMillis: Long,
    onNavigateBack: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit // ★これを追加！
) {
    // ------------------------------------------------------
    // 状態デコード：ミリ秒データからLocalDateを復元（単一ソースの原則）
    // ------------------------------------------------------
    val targetDate = remember(dateMillis) {
        Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    val headerFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日 (E)", Locale.JAPANESE) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE) }

    // ワイヤーフレームに準拠した、当日のサンプルタイムラインデータ
    val dayTasks = remember(targetDate) {
        listOf(
            TimelineTaskItem(
                id = 1L,
                title = "数学 課題レポート提出（第4回）",
                startTime = targetDate.atTime(10, 0),
                endTime = targetDate.atTime(12, 0),
                categoryColor = Color(0xFF1976D2),
                memo = "提出先：LMSポータル"
            ),
            TimelineTaskItem(
                id = 2L,
                title = "【ゼミ】進捗オンラインミーティング",
                startTime = targetDate.atTime(14, 30),
                endTime = targetDate.atTime(16, 0),
                categoryColor = Color(0xFFFF9800),
                memo = "Zoomリンクはカレンダー詳細に添付"
            ),
            TimelineTaskItem(
                id = 3L,
                title = "システム開発 アルゴリズム復習",
                startTime = targetDate.atTime(18, 0),
                endTime = targetDate.atTime(20, 0),
                categoryColor = Color(0xFF4CAF50),
                memo = "図書館で自習"
            )
        )
    }

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
                Text("この日の予定はありません", color = Color.Gray)
            }
        } else {
            // 『日付詳細画面 ワイヤーフレーム』に則った垂直タイムライン構造
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(dayTasks, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // --- 左側：時刻・タイムラインのインジケータ（ワイヤーフレームの左軸線を表現） ---
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(60.dp)
                        ) {
                            Text(
                                text = task.startTime.format(timeFormatter),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // タイムラインの点を視覚化
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(task.categoryColor)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = task.endTime.format(timeFormatter),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // --- 右側：予定のカード内容（単一責任のUIカード） ---
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // 左端にカラー縦棒
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(task.categoryColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = task.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                if (task.memo.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = task.memo,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
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