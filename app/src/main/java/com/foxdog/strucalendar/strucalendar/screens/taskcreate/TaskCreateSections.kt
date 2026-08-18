package com.foxdog.strucalendar.screens.taskcreate

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.DraggableTagList
import com.foxdog.strucalendar.data.entity.ChecklistItem
import com.foxdog.strucalendar.data.entity.Tag
import com.foxdog.strucalendar.notification.ReminderOption
import com.foxdog.strucalendar.notification.ReminderSetting
import com.foxdog.strucalendar.ui.theme.calendarColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.foxdog.strucalendar.components.TagIconId
import com.foxdog.strucalendar.data.entity.TagCustomField
import com.foxdog.strucalendar.data.entity.Template
import com.foxdog.strucalendar.data.recurrence.RecurrenceType


// ============================================================
// 共通の小さいUIパーツ
// ============================================================

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp) // ★ 8.dp/6.dp から削る
    )
}

@Composable
fun SectionLabelWithIcon(text: String, icon: ImageVector) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp) // ★ 8.dp/6.dp から削る
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )
    }
}

@Composable
fun WireframeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    isError: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colorScheme.onSurfaceVariant, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline,
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface,
            errorBorderColor = colorScheme.error
        ),
        minLines = minLines,
        textStyle = TextStyle(fontSize = 14.sp, color = colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}

@Composable
fun WireframeDetailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = colorScheme.onSurfaceVariant, fontSize = 13.sp) },
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colorScheme.primary,
            unfocusedBorderColor = colorScheme.outline,
            focusedContainerColor = colorScheme.surface,
            unfocusedContainerColor = colorScheme.surface
        ),
        maxLines = 1,
        textStyle = TextStyle(fontSize = 13.sp, color = colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}

@Composable
fun TimeDisplayBox(
    dateTimeMillis: Long,
    isAllDay: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val localDateTime = remember(dateTimeMillis) {
        LocalDateTime.ofInstant(Instant.ofEpochSecond(dateTimeMillis), ZoneId.systemDefault())
    }

    val formatter = remember(isAllDay) {
        if (isAllDay) {
            DateTimeFormatter.ofPattern("yyyy/MM/dd (E)", Locale.JAPANESE)
        } else {
            DateTimeFormatter.ofPattern("yyyy/MM/dd (E) HH:mm", Locale.JAPANESE)
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(localDateTime.format(formatter), fontSize = 13.sp, color = colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
fun DetailRowItem(
    icon: ImageVector,
    label: String,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = colorScheme.onSurface)
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
        content()
    }
}

// ============================================================
// 【1】テンプレート選択行
// ============================================================

@Composable
fun TemplateSelectorRow(
    selectedTemplateName: String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, calColors.templateAccentContainer), RoundedCornerShape(8.dp))
            .background(calColors.templateAccentContainer)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = calColors.templateAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = selectedTemplateName, fontSize = 14.sp, color = colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = colorScheme.onSurfaceVariant)
    }
}

// ============================================================
// テンプレート選択欄の直下に出す「最近使用したテンプレート」
// 常に3スロット分の幅を確保し、埋まっている分だけ左詰めで表示する。
// ============================================================

private const val RECENT_TEMPLATE_SLOT_COUNT = 3

@Composable
fun RecentTemplatesRow(
    recentTemplates: List<Template>,
    onTemplateClick: (Template) -> Unit
) {
    if (recentTemplates.isEmpty()) return

    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    SectionLabel("最近使用したテンプレート")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(RECENT_TEMPLATE_SLOT_COUNT) { index ->
            val template = recentTemplates.getOrNull(index)

            if (template != null) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(calColors.templateAccentContainer)
                        .clickable { onTemplateClick(template) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = calColors.templateAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = template.title,
                        fontSize = 12.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ============================================================
// 【2】タイトル入力
// ============================================================

@Composable
fun TitleSection(
    title: String,
    isTitleError: Boolean,
    onTitleChange: (String) -> Unit,
    label: String = "タイトル (必須)",
    placeholder: String = "タイトルを入力",
    errorMessage: String = "タイトルは必須入力です"
) {
    SectionLabel(label)
    WireframeTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = placeholder,
        isError = isTitleError
    )
    if (isTitleError) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
    }
}

// ============================================================
// 終日設定トグル
// ============================================================

@Composable
fun AllDayToggleRow(
    isAllDay: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp), // ★ vertical=2.dpから0.dpへ
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("終日予定にする", fontSize = 14.sp, color = colorScheme.onSurface)
        }
        Switch(
            checked = isAllDay,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.onPrimary, checkedTrackColor = colorScheme.primary),
            modifier = Modifier.scale(0.85f)
        )
    }
}

// ============================================================
// 【3】時間設定
// ============================================================

@Composable
fun TimeSection(
    startTime: Long,
    endTime: Long,
    isAllDay: Boolean,
    isDateTimeError: Boolean,
    onTimeBoxClick: (target: String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            SectionLabel(if (isAllDay) "日付" else "開始")
            TimeDisplayBox(dateTimeMillis = startTime, isAllDay = isAllDay, onClick = { onTimeBoxClick("START") })
        }
        if (!isAllDay) {
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel("終了")
                TimeDisplayBox(dateTimeMillis = endTime, isAllDay = isAllDay, onClick = { onTimeBoxClick("END") })
            }
        }
    }
    if (isDateTimeError) {
        Text(
            text = "終了日時は開始日時より後に設定してください",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    }
}

// ============================================================
// 【4】タグセクション
// ============================================================

@Composable
fun TagSection(
    availableTags: List<Tag>,
    selectedTags: List<Tag>,
    isTagFolderExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onToggleTagSelection: (Tag) -> Unit,
    onUpdateTagOrder: (List<Tag>) -> Unit,
    onDeleteTagRequest: (Tag) -> Unit,
    onAddTagClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    SectionLabelWithIcon("タグ", Icons.Default.Label)
    Text(
        "選択したタグ（長押しで並び替え・削除）",
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val usableWidthPx = with(density) { (maxWidth - 32.dp).toPx() }

        val displayTags = remember(
            availableTags,
            selectedTags,
            isTagFolderExpanded,
            usableWidthPx
        ) {
            if (isTagFolderExpanded) {
                availableTags
            } else {
                val defaultVisibleIds = LinkedHashSet<Long>()
                var usedWidthPx = 0f
                val spacingPx = with(density) { 8.dp.toPx() }

                availableTags.forEach { tag ->
                    val textWidthPx = textMeasurer.measure(
                        text = tag.name,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    ).size.width.toFloat()
                    val fixedWidthPx = with(density) {
                        (16.dp + 20.dp +
                                if (TagIconId.fromId(tag.icon) != null) 6.dp else 0.dp).toPx()
                    }
                    val chipWidthPx = fixedWidthPx + textWidthPx
                    val requiredWidthPx = chipWidthPx +
                            if (defaultVisibleIds.isEmpty()) 0f else spacingPx

                    if (
                        defaultVisibleIds.isEmpty() ||
                        usedWidthPx + requiredWidthPx <= usableWidthPx
                    ) {
                        defaultVisibleIds.add(tag.tagId)
                        usedWidthPx += requiredWidthPx
                    }
                }

                selectedTags.forEach { defaultVisibleIds.add(it.tagId) }
                availableTags.filter { it.tagId in defaultVisibleIds }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DraggableTagList(
                tags = displayTags,
                allTags = availableTags,
                selectedTags = selectedTags,
                onTagClick = onToggleTagSelection,
                onOrderChanged = onUpdateTagOrder,
                onDeleteTagRequest = onDeleteTagRequest
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(BorderStroke(1.dp, colorScheme.outline), RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddTagClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "タグを追加",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (displayTags.size < availableTags.size || isTagFolderExpanded) {
                    TextButton(
                        onClick = onExpandToggle,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (isTagFolderExpanded) "閉じる ▲" else "さらに表示 ▼",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 【5】メモ
// ============================================================

@Composable
fun MemoSection(
    memo: String,
    onMemoChange: (String) -> Unit
) {
    SectionLabelWithIcon("メモ", Icons.Default.EditNote)
    WireframeTextField(
        value = memo,
        onValueChange = onMemoChange,
        placeholder = "メモを入力",
        minLines = 2 // ★ 3 から 2 に減らす
    )
}

// ============================================================
// 【6】チェックリスト
// ============================================================

@Composable
fun ChecklistSection(
    checkList: List<ChecklistItem>,
    onCheckListChange: (List<ChecklistItem>) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    SectionLabel("チェックリスト")

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        checkList.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked ->
                        val updatedList = checkList.toMutableList()
                        updatedList[index] = item.copy(isChecked = checked)
                        onCheckListChange(updatedList)
                    }
                )

                OutlinedTextField(
                    value = item.text,
                    onValueChange = { newText ->
                        val updatedList = checkList.toMutableList()
                        updatedList[index] = item.copy(text = newText)
                        onCheckListChange(updatedList)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("項目を入力") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface
                    )
                )

                IconButton(
                    onClick = {
                        val updatedList = checkList
                            .filterIndexed { itemIndex, _ -> itemIndex != index }
                            .mapIndexed { newIndex, checklistItem -> checklistItem.copy(position = newIndex) }
                        onCheckListChange(updatedList)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = colorScheme.error
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                onCheckListChange(
                    checkList + ChecklistItem(
                        id = 0L,
                        taskId = 0L,
                        text = "",
                        isChecked = false,
                        position = checkList.size
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, colorScheme.outline)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = colorScheme.onSurface)
            Spacer(modifier = Modifier.width(4.dp))
            Text("項目を追加", color = colorScheme.onSurface)
        }
    }
}

// ============================================================
// 【7】リマインダー通知
// ============================================================

@Composable
fun ReminderSection(
    reminderSetting: ReminderSetting,
    isAllDay: Boolean,
    onReminderSettingChange: (ReminderSetting) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var showNotificationMenu by remember { mutableStateOf(false) }
    val isNotifyEnabled = reminderSetting !is ReminderSetting.None

    val allDayOptions = remember {
        listOf(
            "3日前 12:00" to ReminderSetting.DayBefore(daysBack = 3, hour = 12, minute = 0),
            "3日前 17:00" to ReminderSetting.DayBefore(daysBack = 3, hour = 17, minute = 0),
            "前日 12:00" to ReminderSetting.DayBefore(daysBack = 1, hour = 12, minute = 0),
            "前日 17:00" to ReminderSetting.DayBefore(daysBack = 1, hour = 17, minute = 0),
            "当日 9:00" to ReminderSetting.DayBefore(daysBack = 0, hour = 9, minute = 0),
            "当日 12:00" to ReminderSetting.DayBefore(daysBack = 0, hour = 12, minute = 0),
            "当日 15:00" to ReminderSetting.DayBefore(daysBack = 0, hour = 15, minute = 0),
        )
    }

    fun allDayLabelFor(setting: ReminderSetting): String {
        if (setting is ReminderSetting.DayBefore) {
            allDayOptions.firstOrNull { (_, candidate) ->
                candidate.daysBack == setting.daysBack &&
                        candidate.hour == setting.hour &&
                        candidate.minute == setting.minute
            }?.let { return it.first }
        }
        return "設定"
    }

    DetailRowItem(Icons.Default.Notifications, "リマインダー通知") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.width(if (isAllDay) 210.dp else 180.dp)
        ) {
            if (isNotifyEnabled) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {

                    val currentLabel = if (isAllDay) {
                        allDayLabelFor(reminderSetting)
                    } else {
                        ReminderOption.fromDomain(reminderSetting).label
                    }

                    OutlinedButton(
                        onClick = { showNotificationMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
                    ) {
                        Text(currentLabel, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                    }

                    DropdownMenu(
                        expanded = showNotificationMenu,
                        onDismissRequest = { showNotificationMenu = false },
                        modifier = Modifier.background(colorScheme.surface),
                        containerColor = colorScheme.surface
                    ) {
                        if (isAllDay) {
                            allDayOptions.forEach { (label, setting) ->
                                DropdownMenuItem(
                                    text = { Text(text = label, fontSize = 14.sp, color = colorScheme.onSurface) },
                                    onClick = {
                                        onReminderSettingChange(setting)
                                        showNotificationMenu = false
                                    }
                                )
                            }
                        } else {
                            ReminderOption.defaultOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(text = option.label, fontSize = 14.sp, color = colorScheme.onSurface) },
                                    onClick = {
                                        onReminderSettingChange(option.toDomain())
                                        showNotificationMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Switch(
                checked = isNotifyEnabled,
                onCheckedChange = { isChecked ->
                    onReminderSettingChange(
                        when {
                            !isChecked -> ReminderSetting.None
                            isAllDay -> ReminderSetting.DayBefore(daysBack = 0, hour = 9, minute = 0)
                            else -> ReminderSetting.Before(10)
                        }
                    )
                },
                colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.onPrimary, checkedTrackColor = colorScheme.primary),
                modifier = Modifier.scale(0.85f)
            )
        }
    }
}

// ============================================================
// 場所セクション
// ============================================================

@Composable
fun LocationSection(
    locationName: String?,
    locationAddress: String?,
    latitude: Double?,
    longitude: Double?,
    onSelectClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    SectionLabel("場所")

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(containerColor = colorScheme.surface)
    ) {
        if (locationName.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectClick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "場所を選択",
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (latitude != null && longitude != null) {
                                val uri = Uri.parse(
                                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "地図で見る",
                        tint = colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = locationName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colorScheme.onSurface
                        )
                        if (!locationAddress.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = locationAddress,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "地図で見る",
                                fontSize = 11.sp,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "場所を変更",
                        tint = colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "場所を削除",
                        tint = colorScheme.error
                    )
                }
            }
        }
    }
}

// ============================================================
// カスタムフィールド
// ============================================================

@Composable
fun CustomFieldsSection(
    fields: List<TagCustomField>,
    values: Map<Long, String>,
    onValueChange: (Long, String) -> Unit
) {
    if (fields.isEmpty()) return

    SectionLabelWithIcon("追加項目", Icons.Default.Tune)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        fields.forEach { field ->
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = field.fieldName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                WireframeTextField(
                    value = values[field.fieldId] ?: "",
                    onValueChange = { newValue ->
                        onValueChange(field.fieldId, newValue)
                    },
                    placeholder = "${field.fieldName}を入力"
                )
            }
        }
    }
}

// ============================================================
// 【追加】繰り返し設定
// ============================================================
@Composable
fun RecurrenceSection(
    recurrenceType: RecurrenceType,
    intervalDays: Int,
    nth: Int,
    weekday: Int, // DayOfWeek.value（月=1〜日=7）
    endDateMillis: Long?,
    baseDateMillis: Long, // 開始日（endDate未設定時の初期値計算に使用）
    onTypeChange: (RecurrenceType) -> Unit,
    onIntervalDaysChange: (Int) -> Unit,
    onNthChange: (Int) -> Unit,
    onWeekdayChange: (Int) -> Unit,
    onEndDateClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var showTypeMenu by remember { mutableStateOf(false) }
    var showNthMenu by remember { mutableStateOf(false) }
    var showWeekdayMenu by remember { mutableStateOf(false) }

    val weekdayLabels = mapOf(1 to "月", 2 to "火", 3 to "水", 4 to "木", 5 to "金", 6 to "土", 7 to "日")
    val nthLabels = mapOf(1 to "第1", 2 to "第2", 3 to "第3", 4 to "第4", 5 to "最終")

    SectionLabelWithIcon("繰り返し", Icons.Default.Repeat)

    // --- 種類選択 ---
    Box {
        OutlinedButton(
            onClick = { showTypeMenu = true },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.onSurface)
        ) {
            val label = when (recurrenceType) {
                RecurrenceType.NONE -> "繰り返さない"
                RecurrenceType.INTERVAL_DAYS -> "○日ごと"
                RecurrenceType.MONTHLY_NTH_WEEKDAY -> "毎月 第○曜日"
            }
            Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
            DropdownMenuItem(text = { Text("繰り返さない") }, onClick = { onTypeChange(RecurrenceType.NONE); showTypeMenu = false })
            DropdownMenuItem(text = { Text("○日ごと") }, onClick = { onTypeChange(RecurrenceType.INTERVAL_DAYS); showTypeMenu = false })
            DropdownMenuItem(text = { Text("毎月 第○曜日") }, onClick = { onTypeChange(RecurrenceType.MONTHLY_NTH_WEEKDAY); showTypeMenu = false })
        }
    }

    if (recurrenceType != RecurrenceType.NONE) {
        Spacer(modifier = Modifier.height(10.dp))

        when (recurrenceType) {
            RecurrenceType.INTERVAL_DAYS -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = if (intervalDays <= 0) "" else intervalDays.toString(),
                        onValueChange = { text ->
                            val num = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                            onIntervalDaysChange(num)
                        },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("日ごと", fontSize = 14.sp, color = colorScheme.onSurface)
                }
            }

            RecurrenceType.MONTHLY_NTH_WEEKDAY -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("毎月", fontSize = 14.sp, color = colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        OutlinedButton(onClick = { showNthMenu = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(nthLabels[nth] ?: "第1", fontSize = 13.sp)
                        }
                        DropdownMenu(expanded = showNthMenu, onDismissRequest = { showNthMenu = false }) {
                            nthLabels.forEach { (value, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { onNthChange(value); showNthMenu = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        OutlinedButton(onClick = { showWeekdayMenu = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("${weekdayLabels[weekday] ?: "月"}曜日", fontSize = 13.sp)
                        }
                        DropdownMenu(expanded = showWeekdayMenu, onDismissRequest = { showWeekdayMenu = false }) {
                            weekdayLabels.forEach { (value, label) ->
                                DropdownMenuItem(text = { Text("${label}曜日") }, onClick = { onWeekdayChange(value); showWeekdayMenu = false })
                            }
                        }
                    }
                }
            }

            RecurrenceType.NONE -> {}
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("終了日", fontSize = 12.sp, color = colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))

        // ★ 変更：未設定時は baseDateMillis の1年後をデフォルト表示にする
        val displayEndDateMillis = endDateMillis ?: run {
            val baseDate = Instant.ofEpochSecond(baseDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            baseDate.plusYears(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        }
        TimeDisplayBox(
            dateTimeMillis = displayEndDateMillis,
            isAllDay = true,
            onClick = onEndDateClick
        )
    }
}

// ============================================================
// 色選択
// ============================================================

@Composable
fun ColorSection(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    SectionLabelWithIcon("色", Icons.Default.Palette)

    val paletteColors = listOf(
        Color(0xFF4285F4), Color(0xFF81C784), Color(0xFFFFB74D),
        Color(0xFFD1C4E9), Color(0xFFF48FB1), Color(0xFFE0E0E0)
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        paletteColors.forEach { color ->
            val isSelected = selectedColor == color.toArgb()

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        border = if (isSelected) BorderStroke(2.dp, colorScheme.primary) else BorderStroke(0.dp, Color.Transparent),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color.toArgb()) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(modifier = Modifier.size(6.dp).background(Color.White, CircleShape))
                }
            }
        }
    }
}

// ============================================================
// 自動完了設定
// ============================================================

@Composable
fun AutoCompleteSection(
    isAutoCompleted: Boolean,
    onAutoCompletedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = "終了後に自動で完了にする",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = colorScheme.onSurface
        )

        IconButton(onClick = onInfoClick, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = "説明を表示",
                tint = colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Switch(
            checked = isAutoCompleted,
            onCheckedChange = onAutoCompletedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.onPrimary, checkedTrackColor = colorScheme.primary)
        )
    }
}



// ============================================================
// 詳細設定（チェックリスト・通知・場所・自動完了をまとめる折りたたみ）
// ============================================================

@Composable
fun ExpandableDetailsSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggle() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "詳細設定",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "閉じる" else "開く",
                tint = colorScheme.onSurfaceVariant
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}