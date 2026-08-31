package com.foxdog.strucalendar.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.data.settings.AppThemeMode
import com.foxdog.strucalendar.data.settings.HolidayCountryOptions
import com.foxdog.strucalendar.data.settings.HolidayRegion
import com.foxdog.strucalendar.viewmodel.SettingViewModel
import java.time.DayOfWeek
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.res.painterResource
import com.foxdog.strucalendar.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingViewModel,
    onNavigateBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showOsmLicenseDialog by remember { mutableStateOf(false) }
    var showHolidayCountryDialog by remember { mutableStateOf(false) } // （他のvar remember群と並べて宣言）

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface, maxLines = 1, softWrap = false) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, tint = colorScheme.onSurface, contentDescription = "戻る")
                    }
                },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.strucalendar_logowithtext),
                        contentDescription = "Strucalendar",
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(32.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ------------------------------------------------------
            // 【1】一般設定
            // ------------------------------------------------------
            SettingSectionTitle("一般設定")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ClickableSettingRow(
                        title = "テーマ",
                        value = settings.themeMode.toDisplayLabel()
                    ) { showThemeDialog = true }

                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    ClickableSettingRow(
                        title = "週の開始日",
                        value = settings.weekStartDay.toDisplayLabel()
                    ) { showWeekStartDialog = true }
                }
            }

            // ------------------------------------------------------
            // 【2】通知設定
            // ------------------------------------------------------
            SettingSectionTitle("通知設定")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = colorScheme.onSurface, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "通知", fontSize = 15.sp, modifier = Modifier.weight(1f), color = colorScheme.onSurface)
                        Switch(
                            checked = settings.isNotificationEnabled,
                            onCheckedChange = { viewModel.setNotificationEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.surface, checkedTrackColor = colorScheme.onSurfaceVariant)
                        )
                    }

                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    ClickableSettingRow(
                        title = "新規タスクの初期通知時間",
                        value = formatReminderMinutes(settings.defaultReminderOffsetMinutes),
                        enabled = settings.isNotificationEnabled
                    ) { if (settings.isNotificationEnabled) showReminderDialog = true }
                }
            }

            // ------------------------------------------------------
            // 【3】チュートリアル
            // ------------------------------------------------------
            SettingSectionTitle("チュートリアル")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                var onboardingResetFeedback by remember { mutableStateOf(false) }

                LaunchedEffect(onboardingResetFeedback) {
                    if (onboardingResetFeedback) {
                        kotlinx.coroutines.delay(2000)
                        onboardingResetFeedback = false
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.resetTutorialGuides() // resetCalendarOnboarding() から差し替え
                            onboardingResetFeedback = true
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "チュートリアルガイドを再度有効にする",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        )
                    }
                    if (onboardingResetFeedback) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "有効にしました",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ------------------------------------------------------
            // 【4】表示設定
            // ------------------------------------------------------
            SettingSectionTitle("表示設定")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {

                    SwitchSettingRow(
                        title = "タスク作成時に詳細設定を常に表示",
                        subtitle = "タスク作成画面で、場所やメモなどの詳細入力欄を\n最初から開いておきます",
                        checked = settings.alwaysShowDetailedTaskSettings
                    ) { viewModel.setAlwaysShowDetailedTaskSettings(it) }

                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchSettingRow(
                        title = "カレンダーの色",
                        subtitle = "タグの色を表示",
                        checked = settings.showTagColorOnCalendar
                    ) { viewModel.setShowTagColorOnCalendar(it) }

                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchSettingRow(
                        title = "完了済みの予定を表示",
                        subtitle = "",
                        checked = settings.showCompletedTasks
                    ) { viewModel.setShowCompletedTasks(it) }

                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchSettingRow(
                        title = "週の番号を表示",
                        subtitle = "",
                        checked = settings.showWeekNumber
                    ) { viewModel.setShowWeekNumber(it) }

                    // 祝日の対象国
                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    ClickableSettingRow(
                        title = "祝日の国",
                        value = HolidayCountryOptions.displayNameFor(settings.holidayCountryCode)
                    ) { showHolidayCountryDialog = true }


                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchSettingRow(
                        title = "タスク削除時に確認する",
                        subtitle = "誤って削除しないよう、削除前に確認ダイアログを表示します",
                        checked = settings.confirmBeforeDeleteTask
                    ) { viewModel.setConfirmBeforeDeleteTask(it) }

                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))

                    SwitchSettingRow(
                        title = "保存前に閉じたら確認する",
                        subtitle = "予定・テンプレート・タグの作成/編集中に×で閉じたとき、\n保存されない旨の確認ダイアログを表示します",
                        checked = settings.confirmDiscardChanges
                    ) { viewModel.setConfirmDiscardChanges(it) }
                }
            }



            // ------------------------------------------------------
            // 【5】今後対応予定(現時点では未実装)
            // ------------------------------------------------------
            SettingSectionTitle("その他")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    DisabledSettingRow("アカウント連携")
                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))
                    DisabledSettingRow("バックアップと同期")
                    HorizontalDivider(color = colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))
                    DisabledSettingRow("データのエクスポート")
                }
            }

            Text(
                text = "アカウント連携・バックアップ機能はユーザー規模確認後、近日中のアップデートで対応予定です。",
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            // ------------------------------------------------------
            // 【6】ライセンス情報
            // ------------------------------------------------------
            SettingSectionTitle("ライセンス情報")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ClickableSettingRow(
                        title = "位置情報データについて",
                        value = ""
                    ) { showOsmLicenseDialog = true }
                }
            }
        }
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = "テーマ",
            options = AppThemeMode.entries.map { it to it.toDisplayLabel() },
            selected = settings.themeMode,
            onSelect = { viewModel.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showWeekStartDialog) {
        SelectionDialog(
            title = "週の開始日",
            options = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY).map { it to it.toDisplayLabel() },
            selected = settings.weekStartDay,
            onSelect = { viewModel.setWeekStartDay(it) },
            onDismiss = { showWeekStartDialog = false }
        )
    }

    if (showReminderDialog) {
        SelectionDialog(
            title = "新規タスクの初期通知時間",
            options = listOf(5, 10, 30, 60, 120, 1440).map { it to formatReminderMinutes(it) },
            selected = settings.defaultReminderOffsetMinutes,
            onSelect = { viewModel.setDefaultReminderOffsetMinutes(it) },
            onDismiss = { showReminderDialog = false }
        )
    }

    if (showHolidayCountryDialog) {
        HolidayCountrySelectionDialog(
            selectedCode = settings.holidayCountryCode,
            onSelect = { viewModel.setHolidayCountryCode(it) },
            onDismiss = { showHolidayCountryDialog = false }
        )
    }

    if (showOsmLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showOsmLicenseDialog = false },
            title = { Text("位置情報データについて", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "本アプリの場所検索機能は OpenStreetMap のデータを利用しています。",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "© OpenStreetMap contributors",
                        fontSize = 13.sp,
                        color = colorScheme.primary,
                        modifier = Modifier.clickable {
                            val uri = Uri.parse("https://www.openstreetmap.org/copyright")
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "データは Open Database License (ODbL) の下で提供されています。",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOsmLicenseDialog = false }) { Text("閉じる") }
            }
        )
    }
}

// ============================================================
// 汎用選択ダイアログ
// ============================================================

@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value); onDismiss() })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    )
}

// ============================================================
// 表示ラベル変換
// ============================================================

fun AppThemeMode.toDisplayLabel(): String = when (this) {
    AppThemeMode.SYSTEM -> "システム設定に従う"
    AppThemeMode.LIGHT -> "ライト"
    AppThemeMode.DARK -> "ダーク"
}

fun DayOfWeek.toDisplayLabel(): String = when (this) {
    DayOfWeek.SUNDAY -> "日曜日"
    DayOfWeek.MONDAY -> "月曜日"
    else -> this.name
}

fun formatReminderMinutes(minutes: Int): String = when (minutes) {
    1440 -> "前日"
    60 -> "1時間前"
    120 -> "2時間前"
    else -> "${minutes}分前"
}

// ============================================================
// 祝日の国選択ダイアログ（地域→国の2段階選択）
// ============================================================

@Composable
private fun HolidayCountrySelectionDialog(
    selectedCode: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    // ダイアログを開くたびに地域選択はリセットする（前回の選択国とは独立した一時的な絞り込みUI）
    var selectedRegion by remember { mutableStateOf<HolidayRegion?>(null) }
    var showRegionPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("祝日の国", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // 「自動」は地域を経由せず常時ここから選べる
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null); onDismiss() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedCode == null, onClick = { onSelect(null); onDismiss() })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("自動（端末の設定）", fontSize = 14.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 地域選択ボタン：未選択時は「地域を選択してください」のプレースホルダーのみ
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRegionPicker = true }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedRegion?.displayName ?: "地域を選択してください",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }

                // 地域が選ばれるまでは国の選択肢を一切表示しない
                if (selectedRegion != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        HolidayCountryOptions.countriesByRegion[selectedRegion]?.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(code); onDismiss() }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedCode == code, onClick = { onSelect(code); onDismiss() })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(name, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    )

    // 地域一覧そのものは既存のジェネリックSelectionDialogを再利用
    if (showRegionPicker) {
        SelectionDialog(
            title = "地域を選択",
            options = HolidayRegion.entries.map { it to it.displayName },
            selected = selectedRegion,
            onSelect = { selectedRegion = it },
            onDismiss = { showRegionPicker = false }
        )
    }
}
// ============================================================
// 共通パーツ
// ============================================================

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun ClickableSettingRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val disabledColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 15.sp, color = if (enabled) colorScheme.onSurface else disabledColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(text = value, fontSize = 14.sp, color = colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (enabled) colorScheme.onSurfaceVariant else disabledColor.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DisabledSettingRow(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    val disabledColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 15.sp, color = disabledColor)
        Text(text = "近日対応", fontSize = 12.sp, color = disabledColor)
    }
}

@Composable
fun SwitchSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, color = colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.surface, checkedTrackColor = colorScheme.onSurfaceVariant)
        )
    }
}