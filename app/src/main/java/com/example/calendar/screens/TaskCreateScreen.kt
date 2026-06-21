package com.example.calendar.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.viewmodel.TaskCreateViewModel
import com.example.calendar.state.TaskInputState
import com.example.calendar.data.entity.Template
import com.example.calendar.data.entity.Tag
import com.example.calendar.components.TagCreateDialog
import com.example.calendar.components.TagIconSource
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCreateScreen(
    viewModel: TaskCreateViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val context = LocalContext.current

    val taskState = viewModel.inputState
    val scrollState = rememberScrollState()

    val fieldBackgroundColor = Color(0xFFFAFAFA)
    val borderStrokeColor = Color(0xFFE0E0E0)
    val templateBgColor = Color(0xFFF8E5FF)
    val templateIconColor = Color(0xFF6200EE)

    var activeTarget by remember { mutableStateOf<String?>(null) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var selectedTemplateName by remember { mutableStateOf("テンプレートを選択") }
    var isDetailExpanded by remember { mutableStateOf(true) }

    var showTagCreateDialog by remember { mutableStateOf(false) }
    var isTagFolderExpanded by remember { mutableStateOf(false) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    // ★ 追加: ドロップダウンメニューの開閉状態
    var showNotificationMenu by remember { mutableStateOf(false) }

    // ★ 追加: 通知の選択肢定義（表示名 ⇄ 分数 のマップ）
    val reminderOptions = remember {
        listOf(
            "開始時間" to 0,
            "5分前" to 5,
            "10分前" to 10,
            "30分前" to 30,
            "1時間前" to 60,
            "2時間前" to 120,
            "前日" to 1440
        )
    }

    // ★ 永続化：ViewModelから本物のタグ一覧をリアルタイムで取得する
    val availableTags by viewModel.allTags.collectAsState()
    LaunchedEffect(availableTags) {
        Log.d(
            "TagDB",
            availableTags.joinToString {
                "${it.tagId}:${it.name}:${it.icon}"
            }
        )
    }

    // 選択されたタグが常に前（先頭）に来るように並び替えたリストを作成
    val sortedAvailableTags = remember(availableTags.toList(), taskState.selectedTags.toList()) {
        availableTags.sortedWith(compareByDescending { taskState.selectedTags.contains(it) })
    }

    val templates = remember {
        listOf(
            Template(
                templateId = 1L, title = "【宿題】期日：", icon = "Assignment", timeLength = 3600L,
                description = "学校の課題提出用のテンプレートです", color = Color(0xFF4285F4).value.toInt(),
                memo = "提出先：\n持ち物：", checkList = "・名前を書いたか", latitude = 35.6812, longitude = 139.7671,
                dayCountTarget = null, url = "", attachmentPath = "", isAutoCompleted = false
            )
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("予定を作成", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color(0xFF1C1B1F), modifier = Modifier.size(24.dp))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // ★ viewModel.saveTask に context を渡すように修正
                        viewModel.saveTask(context = context, onSuccess = onNavigateBack)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = Color(0xFF1C1B1F), modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- 【1】テンプレート選択 ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, Color(0xFFE8E5FF)), RoundedCornerShape(8.dp))
                    .background(templateBgColor)
                    .clickable { showTemplateDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = templateIconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = selectedTemplateName, fontSize = 14.sp, color = Color(0xFF1C1B1F), modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF5F6368))
            }

            // --- 【2】タイトル入力 ---
            SectionLabel("タイトル")
            WireframeTextField(value = taskState.title, onValueChange = { viewModel.updateInput { current -> current.copy(title = it) } }, placeholder = "タイトルを入力")

            // --- 【3】時間設定 ---
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("開始")
                    TimeDisplayBox(dateTimeMillis = taskState.startTime, onClick = { activeTarget = "START" })
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("終了")
                    TimeDisplayBox(dateTimeMillis = taskState.endTime, onClick = { activeTarget = "END" })
                }
            }

            // --- 【4】タグセクション (複数行 ＆ 「さらに表示」制御) ---
            SectionLabel("タグ")
            Text("選択したタグ (長押しで削除)", fontSize = 12.sp, color = Color(0xFF70757A), modifier = Modifier.padding(bottom = 8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxLines = if (isTagFolderExpanded) Int.MAX_VALUE else 2
                ) {
                    // プラスボタン
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(BorderStroke(1.dp, borderStrokeColor), RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showTagCreateDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "タグを追加", tint = Color(0xFF3C4043), modifier = Modifier.size(18.dp))
                    }

                    // 並び替え済みのリストを使用
                    sortedAvailableTags.forEach { tag ->
                        val isSelected = taskState.selectedTags.contains(tag)

                        // 一覧画面（TaskListScreen）のカラー仕様と統一
                        val chipBgColor = if (isSelected) Color(tag.color) else Color(0xFFF1F3F4)
                        val chipTextColor = if (isSelected) Color(0xFF1C1B1F) else Color(0xFF3C4043)
                        val contentAccentColor = if (isSelected) Color(0xFF1C1B1F) else Color(tag.color)
                        val borderStroke = if (isSelected) BorderStroke(1.5.dp, Color(tag.color)) else BorderStroke(1.dp, Color(0xFFE0E0E0))

                        val tagIcon = when (tag.icon) {
                            "Book" -> Icons.Default.Book
                            "ErrorOutline" -> Icons.Default.ErrorOutline
                            "" -> null
                            else -> Icons.Default.Bookmark
                        }

                        Surface(
                            color = chipBgColor,
                            shape = RoundedCornerShape(8.dp),
                            border = borderStroke,
                            modifier = Modifier
                                .height(32.dp)
                                .combinedClickable(
                                    onClick = { viewModel.toggleTagSelection(tag) },
                                    onLongClick = { tagToDelete = tag }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                if (tagIcon != null) {
                                    Icon(
                                        imageVector = tagIcon,
                                        contentDescription = null,
                                        tint = contentAccentColor, // 未選択時は本来の色を適用
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = tag.name,
                                    color = chipTextColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (availableTags.size > 2) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = { isTagFolderExpanded = !isTagFolderExpanded },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isTagFolderExpanded) "閉じる ▲" else "さらに表示 ▼",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // --- 【5】メモ ---
            SectionLabel("メモ")
            WireframeTextField(value = taskState.memo, onValueChange = { viewModel.updateInput { current -> current.copy(memo = it) } }, placeholder = "メモを入力", minLines = 3)

            // --- 【6】チェックリスト ---
            SectionLabel("チェックリスト")
            WireframeTextField(value = taskState.checkList, onValueChange = { viewModel.updateInput { current -> current.copy(checkList = it) } }, placeholder = "・項目を改行区切りで入力", minLines = 2)

            // --- 【7】詳細設定 ---
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isDetailExpanded = !isDetailExpanded }
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "詳細設定",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isDetailExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF1C1B1F)
                )
            }

            AnimatedVisibility(
                visible = isDetailExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // ★ 追加: 通知設定項目
                    DetailRowItem(Icons.Default.Notifications, "リマインダー通知") {
                        val isNotifyEnabled = taskState.remindMinutes != null

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // 通知のオン／オフを切り替えるSwitch
                            Switch(
                                checked = isNotifyEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateInput { current ->
                                        // オンにした瞬間に初期値「10分前」をセット、オフで null にする
                                        current.copy(remindMinutes = if (isChecked) 10 else null)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1A73E8)
                                ),
                                modifier = Modifier.scale(0.85f) // スイッチのサイズを少しコンパクトに
                            )

                            // オンに設定されている時だけ、ドロップダウンを表示
                            if (isNotifyEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box {
                                    val currentMinutes = taskState.remindMinutes ?: 10
                                    val displayText = reminderOptions.find { it.second == currentMinutes }?.first ?: "${currentMinutes}分前"

                                    OutlinedButton(
                                        onClick = { showNotificationMenu = true },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, borderStrokeColor),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1C1B1F))
                                    ) {
                                        Text(displayText, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }

                                    DropdownMenu(
                                        expanded = showNotificationMenu,
                                        onDismissRequest = { showNotificationMenu = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        reminderOptions.forEach { (label, minutes) ->
                                            DropdownMenuItem(
                                                text = { Text(label, fontSize = 14.sp) },
                                                onClick = {
                                                    viewModel.updateInput { current -> current.copy(remindMinutes = minutes) }
                                                    showNotificationMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DetailRowItem(Icons.Default.AttachFile, "添付ファイル") {
                        Button(
                            onClick = {
                                viewModel.updateInput { current ->
                                    current.copy(attachmentPath = "content://media/sample")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF3C4043)
                            ),
                            border = BorderStroke(1.dp, borderStrokeColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(if (taskState.attachmentPath.isEmpty()) "ファイルを追加" else "選択済み", fontSize = 13.sp)
                        }
                    }

                    DetailRowItem(Icons.Default.Link, "URL") {
                        WireframeDetailTextField(
                            value = taskState.url,
                            onValueChange = { viewModel.updateInput { current -> current.copy(url = it) } },
                            placeholder = "URLを入力"
                        )
                    }

                    DetailRowItem(Icons.Default.LocationOn, "位置情報(緯度)") {
                        WireframeDetailTextField(
                            value = taskState.latitude?.toString() ?: "",
                            onValueChange = { viewModel.updateInput { current -> current.copy(latitude = it.toDoubleOrNull()) } },
                            placeholder = "緯度を入力"
                        )
                    }
                }
            }

            // --- 【8】自動ステータス ＆ カラーパレット ---
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "完了時の自動ステータス変更",
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = Color(0xFF1C1B1F)
                )
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF9AA0A6),
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
                Switch(
                    checked = taskState.isAutoCompleted,
                    onCheckedChange = { viewModel.updateInput { current -> current.copy(isAutoCompleted = it) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF1A73E8)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("色", fontSize = 14.sp, color = Color(0xFF1C1B1F))
                }

                val paletteColors = listOf(
                    Color(0xFF4285F4), Color(0xFF81C784), Color(0xFFFFB74D),
                    Color(0xFFD1C4E9), Color(0xFFF48FB1), Color(0xFFE0E0E0)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    paletteColors.forEach { color ->
                        val isSelected = taskState.color == color.value.toInt()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .then(
                                    if (isSelected) Modifier
                                        .border(BorderStroke(2.dp, Color(0xFF1A73E8)), CircleShape)
                                        .padding(3.dp)
                                    else Modifier
                                )
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else color)
                                .clickable { viewModel.updateInput { current -> current.copy(color = color.value.toInt()) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ダイアログ ＆ ウィザード制御 ---
    if (tagToDelete != null) {
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("タグの削除", fontWeight = FontWeight.Bold) },
            text = { Text("タグ「${tagToDelete?.name}」を削除しますか？\n(この操作は取り消せません)") },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("キャンセル") } },
            confirmButton = {
                Button(
                    onClick = {
                        tagToDelete?.let { target ->
                            viewModel.deleteTag(target)
                        }
                        tagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除する")
                }
            }
        )
    }

    if (showTagCreateDialog) {
        TagCreateDialog(
            onDismissRequest = { showTagCreateDialog = false },
            onTagSave = { name, iconSource, color, _ ->

                val iconString = when (iconSource) {
                    is TagIconSource.InitialText -> null
                    is TagIconSource.Vector -> iconSource.iconId.id
                    is TagIconSource.CustomUri -> null
                }

                val newTag = Tag(
                    tagId = 0L,
                    name = name,
                    color = color.toArgb(),
                    icon = iconString
                )

                Log.d("TagSave", "name=$name icon=$iconString")
                viewModel.createTag(newTag)

                showTagCreateDialog = false
            }
        )
    }

    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("テンプレートを選択", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    templates.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(fieldBackgroundColor)
                                .clickable {
                                    selectedTemplateName = template.title
                                    viewModel.applyTemplate(template)
                                    showTemplateDialog = false
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = templateIconColor)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = template.title, fontSize = 14.sp, color = Color(0xFF1C1B1F))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplateDialog = false }) { Text("閉じる") } }
        )
    }

    if (activeTarget != null) {
        DateTimePickerWizard(
            target = activeTarget!!,
            onDismiss = { activeTarget = null },
            onDateTimeSelected = { finalDateTimeLong ->
                viewModel.updateInput { current ->
                    if (activeTarget == "START") current.copy(startTime = finalDateTimeLong)
                    else current.copy(endTime = finalDateTimeLong)
                }
                activeTarget = null
            }
        )
    }
}


// --- 画面共通の軽量コンポーネントパーツ群（以下、既存と同様） ---
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1B1F),
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun WireframeTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, minLines: Int = 1) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9AA0A6), fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1A73E8),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        minLines = minLines,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color(0xFF1C1B1F))
    )
}

@Composable
fun TimeDisplayBox(dateTimeMillis: Long, onClick: () -> Unit) {
    val localDateTime = remember(dateTimeMillis) { LocalDateTime.ofInstant(Instant.ofEpochSecond(dateTimeMillis), ZoneOffset.UTC) }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd (E) HH:mm", Locale.JAPANESE) }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(localDateTime.format(formatter), fontSize = 13.sp, color = Color(0xFF1C1B1F), maxLines = 1)
        }
    }
}

@Composable
fun DetailRowItem(icon: ImageVector, label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF1C1B1F))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = Color(0xFF1C1B1F), modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
fun WireframeDetailTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF9AA0A6), fontSize = 13.sp) },
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1A73E8),
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        maxLines = 1,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Color(0xFF1C1B1F))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerWizard(target: String, onDismiss: () -> Unit, onDateTimeSelected: (Long) -> Unit) {
    var isTimeStep by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())
    val now = LocalTime.now()
    val timeState = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = true)

    if (!isTimeStep) {
        DatePickerDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = { TextButton(onClick = { isTimeStep = true }) { Text("次へ (時間選択)") } },
            dismissButton = { TextButton(onClick = { onDismiss() }) { Text("キャンセル") } }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                TextButton(onClick = {
                    val millis = dateState.selectedDateMillis ?: Instant.now().toEpochMilli()
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val time = LocalTime.of(timeState.hour, timeState.minute)
                    onDateTimeSelected(LocalDateTime.of(date, time).toEpochSecond(ZoneOffset.UTC))
                }) {
                    Text("決定")
                }
            },
            dismissButton = { TextButton(onClick = { isTimeStep = false }) { Text("戻る") } },
            title = { Text(if (target == "START") "開始時間を設定" else "終了時間を設定") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            }
        )
    }
}