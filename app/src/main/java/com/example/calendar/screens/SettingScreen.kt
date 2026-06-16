package com.example.calendar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    // モックアップに配置されているトグルスイッチの状態（State）
    var isNotificationEnabled by remember { mutableStateOf(true) }
    var isTagColorEnabled by remember { mutableStateOf(true) }
    var isShowCompletedTasksEnabled by remember { mutableStateOf(true) }
    var isShowWeekNumberEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9F9F9) // モックアップ通りの薄いグレー背景
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ------------------------------------------------------
            // 【1】アカウント セクション
            // ------------------------------------------------------
            SettingSectionTitle("アカウント")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* アカウント詳細へ */ }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 丸背景付きのユーザーアイコン
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF1F3F4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(26.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "筑波 太郎", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Text(text = "taro.tsukuba@example.com", fontSize = 13.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                }
            }

            // ------------------------------------------------------
            // 【2】一般設定 セクション
            // ------------------------------------------------------
            SettingSectionTitle("一般設定")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ClickableSettingRow("言語", "日本語") { }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    ClickableSettingRow("テーマ", "システム設定に従う") { }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    ClickableSettingRow("週の開始日", "日曜日") { }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    ClickableSettingRow("タイムゾーン", "(GMT+9:00) 東京") { }
                }
            }

            // ------------------------------------------------------
            // 【3】通知設定 セクション
            // ------------------------------------------------------
            SettingSectionTitle("通知設定")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // 通知大元のトグルスイッチ（アイコン付き）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "通知", fontSize = 15.sp, modifier = Modifier.weight(1f), color = Color.Black)
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { isNotificationEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF555555)) // モックアップのシックな黒/濃いグレーのトグルを再現
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    ClickableSettingRow("通知時間", "予定の 10 分前") { }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    ClickableSettingRow("通知音", "デフォルト") { }
                }
            }

            // ------------------------------------------------------
            // 【4】表示設定 セクション
            // ------------------------------------------------------
            SettingSectionTitle("表示設定")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SwitchSettingRow("カレンダーの色", "タグの色を表示", isTagColorEnabled) { isTagColorEnabled = it }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow("完了済みの予定を表示", "", isShowCompletedTasksEnabled) { isShowCompletedTasksEnabled = it }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingRow("週の番号を表示", "", isShowWeekNumberEnabled) { isShowWeekNumberEnabled = it }
                }
            }

            // ------------------------------------------------------
            // 【5】データ管理 セクション
            // ------------------------------------------------------
            SettingSectionTitle("データ管理")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ClickableSettingRow("バックアップと同期", "") { }
                    HorizontalDivider(color = Color(0xFFF5F5F5), modifier = Modifier.padding(horizontal = 16.dp))
                    ClickableSettingRow("データのエクスポート", "") { }
                }
            }

            // ------------------------------------------------------
            // 【6】その他 セクション
            // ------------------------------------------------------
            SettingSectionTitle("その他")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                ClickableSettingRow("ヘルプとフィードバック", "") { }
            }
        }
    }
}

/**
 * モックアップ仕様：各セクションの左端に揃えられた、上品な太字見出し
 */
@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

/**
 * 再利用コンポーネント①：タップして右画面やダイアログに進む一般的な設定行
 */
@Composable
fun ClickableSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 15.sp, color = Color.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(text = value, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * 再利用コンポーネント②：トグルスイッチでその場で設定を切り替える行
 */
@Composable
fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = Color.Black)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF555555))
        )
    }
}