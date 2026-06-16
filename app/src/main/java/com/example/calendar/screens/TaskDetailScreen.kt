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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    onNavigateBack: () -> Unit
) {
    // モックアップに配置されている全データを完全にState・定数化
    var isCompleted by remember { mutableStateOf(false) }

    val taskTitle = "数学課題提出"
    val startTimeStr = "2026/05/06（水） 23:59"
    val endTimeStr = "2026/05/06（水） 23:59"
    val memoText = "問題集の第3章までを提出すること。\nファイルはPDFで提出。"

    // チェックリストデータ（モックアップ通り状態保持）
    var checklistItems by remember {
        mutableStateOf(
            listOf(
                ChecklistItem(1, "第1章の復習", false),
                ChecklistItem(2, "第2章の問題演習", false),
                ChecklistItem(3, "第3章の演習", true)
            )
        )
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
                    IconButton(onClick = { /* TODO: 削除 */ }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "削除")
                    }
                    IconButton(onClick = { /* TODO: その他メニュー */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "メニュー")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9F9F9) // 全体的に薄いグレー背景でカードを引き立たせる
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
                                .border(2.dp, if (isCompleted) Color(0xFF1A73E8) else Color.Gray, CircleShape)
                                .background(if (isCompleted) Color(0xFF1A73E8) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isCompleted) "完了" else "未完了",
                            color = if (isCompleted) Color(0xFF1A73E8) else Color(0xFF1A73E8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { isCompleted = !isCompleted },
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

            // --- 【2】メイン情報カード（タイトル・タグ・日時・メモ） ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // タイトル行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = Color(0xFF1A73E8), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = taskTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(modifier = Modifier.height(16.dp))

                    // タグセクション
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("タグ", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(48.dp))

                        // マップされた横並びタグチップ
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MockTagChip("提出物", Color(0xFFE8F0FE), Color(0xFF1A73E8), Icons.Default.Book)
                            MockTagChip("重要", Color(0xFFFCE8E6), Color(0xFFD93025), Icons.Default.RadioButtonUnchecked)
                            MockTagChip("数学", Color(0xFFF1F3F4), Color.Black)
                            MockTagChip("レポート", Color(0xFFF1F3F4), Color.Black)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF1F3F4), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 開始・終了日時セクション
                    TimeRowMock(label = "開始", timeStr = startTimeStr)
                    Spacer(modifier = Modifier.height(12.dp))
                    TimeRowMock(label = "終了", timeStr = endTimeStr)

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
                                Text(text = memoText, fontSize = 14.sp, color = Color.Black, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // --- 【3】チェックリストカード（動的アイテム追加・状態管理対応） ---
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

                    // チェックリストアイテム一覧
                    Column(
                        modifier = Modifier
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    ) {
                        checklistItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        checklistItems = checklistItems.toMutableList().apply {
                                            this[index] = item.copy(isChecked = checked)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = item.text, fontSize = 14.sp, modifier = Modifier.weight(1f))

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

                        // 項目を追加ボタン
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

            // --- 【4】詳細拡張属性リスト（添付ファイルからその他設定まで） ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExtensionRowMock(Icons.Default.AttachFile, "添付ファイル", "課題_第3章.pdf", isLink = true)
                    ExtensionRowMock(Icons.Default.Link, "URL", "https://example.com/assignment/3", isLink = true)
                    ExtensionRowMock(Icons.Default.LocationOn, "位置情報", "筑波大学 中央図書館", isLink = true)
                    ExtensionRowMock(Icons.Default.CalendarMonth, "カウントダウン", "目標日時：2026/05/06（水） 23:59（あと 0 日）", textColor = Color(0xFF1A73E8))

                    // 色インジケータ
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("色", fontSize = 14.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF6699FF)))
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    }

                    ExtensionRowMock(Icons.Default.Settings, "その他設定", "自動ステータス変更：OFF")
                }
            }

            // --- 【5】最下部：作成・更新メタデータ ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("作成日時：2026/05/01（金） 14:30", fontSize = 12.sp, color = Color.Gray)
                Text("更新日時：2026/05/03（日） 16:45", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// データモデル
data class ChecklistItem(val id: Int, val text: String, val isChecked: Boolean)

// 補助コンポーネント群（モックアップUI用）
@Composable
fun MockTagChip(text: String, bgColor: Color, textColor: Color, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text = text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (icon != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Close, contentDescription = null, tint = textColor, modifier = Modifier.size(12.dp))
        }
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