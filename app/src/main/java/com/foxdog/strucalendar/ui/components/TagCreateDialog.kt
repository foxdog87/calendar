package com.foxdog.strucalendar.components

import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

sealed class TagIconSource {
    object InitialText : TagIconSource()
    data class Vector(val iconId: TagIconId) : TagIconSource()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagCreateDialog(
    onDismissRequest: () -> Unit,
    onTagSave: (name: String, iconSource: TagIconSource, color: Color, customFields: List<String>) -> Unit
) {
    val tagName = remember { mutableStateOf("") }
    val customFields = remember { mutableStateListOf<String>() }
    val selectedIconSource = remember { mutableStateOf<TagIconSource>(TagIconSource.InitialText) }

    var showInfoDialog by remember { mutableStateOf(false) }

    val sampleIconEnums = remember { TagIconId.entries }
    val sampleColors = listOf(
        Color(0xFFFF4081),Color(0xFFFFD54F), Color(0xFFFF8A80), Color(0xFF80D8FF), Color(0xB369F0AE), Color(0xFFEA80FC), Color(0xFFFFB74D),
        Color(0xFF29B6F6), Color(0xFF00E676), Color(0xFFD1C4E9), Color(0xFFE1BEE7), Color(0xFFB0BEC5)
    )
    val selectedColor = remember { mutableStateOf(sampleColors[0]) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(text = "新規タグの作成", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            val focusManager = LocalFocusManager.current
            val context = LocalContext.current
            val dialogView = LocalView.current

            val dialogWindow = remember(dialogView) {
                (dialogView.parent as? DialogWindowProvider)?.window
            }

            fun hideKeyboard() {
                Log.d("TagCreateDialog", "hideKeyboard start")
                focusManager.clearFocus(force = true)
                if (dialogWindow != null) {
                    val controller = WindowCompat.getInsetsController(dialogWindow, dialogView)
                    controller.hide(WindowInsetsCompat.Type.ime())
                    Log.d("TagCreateDialog", "keyboard hide via WindowInsetsControllerCompat (dialog window)")
                } else {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(dialogView.windowToken, 0)
                    Log.d("TagCreateDialog", "keyboard hide via InputMethodManager fallback")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. タグ名入力 ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(selectedColor.value.copy(alpha = 0.2f))
                                .border(width = 1.dp, color = selectedColor.value, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val source = selectedIconSource.value) {
                                is TagIconSource.InitialText -> {
                                    val displayChar = if (tagName.value.isNotBlank()) tagName.value.take(1) else "?"
                                    Text(text = displayChar, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = selectedColor.value)
                                }
                                is TagIconSource.Vector -> {
                                    Icon(imageVector = source.iconId.vector, contentDescription = null, modifier = Modifier.size(28.dp), tint = selectedColor.value)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = tagName.value,
                            onValueChange = { newValue ->
                                tagName.value = newValue
                                if (selectedIconSource.value is TagIconSource.InitialText) {
                                    selectedIconSource.value = TagIconSource.InitialText
                                }
                            },
                            label = { Text("タグ名を入力") },
                            placeholder = { Text("例: 提出物") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    hideKeyboard()
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // --- 2. サンプルアイコン選択 (順番を入れ替え) ---
                item {
                    Text(text = "アイコンを選択", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 4.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        maxItemsInEachRow = 5
                    ) {
                        sampleIconEnums.forEach { iconEnum ->
                            val isSelected = (selectedIconSource.value as? TagIconSource.Vector)?.iconId == iconEnum
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) selectedColor.value.copy(alpha = 0.3f) else Color.Transparent)
                                    .border(width = if (isSelected) 2.dp else 1.dp, color = if (isSelected) selectedColor.value else Color.LightGray, shape = CircleShape)
                                    .clickable { selectedIconSource.value = TagIconSource.Vector(iconEnum) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = iconEnum.vector, contentDescription = null, modifier = Modifier.size(22.dp), tint = if (isSelected) selectedColor.value else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // --- 3. タグカラー選択 (順番を入れ替え) ---
                item {
                    Text(text = "タグカラー", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(bottom = 8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        maxItemsInEachRow = 6
                    ) {
                        sampleColors.forEach { color ->
                            val isSelected = selectedColor.value == color
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = CircleShape)
                                    .clickable { selectedColor.value = color }
                            )
                        }
                    }
                }

                // --- 4. カスタム項目 ---
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "カスタム項目", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            IconButton(
                                onClick = {
                                    hideKeyboard()
                                    showInfoDialog = true
                                },
                                modifier = Modifier.size(28.dp).padding(start = 4.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "説明を表示", tint = Color(0xFF1A73E8), modifier = Modifier.size(18.dp))
                            }
                        }

                        OutlinedButton(
                            onClick = { customFields.add("") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("追加", fontSize = 12.sp)
                        }
                    }
                }

                itemsIndexed(customFields) { index, fieldText ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fieldText,
                            onValueChange = { newValue -> customFields[index] = newValue },
                            label = { Text("項目名 ${index + 1}") },
                            placeholder = { Text("例: 提出先") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    hideKeyboard()
                                }
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        IconButton(
                            onClick = {
                                hideKeyboard()
                                // ★ 修正：リストから対象インデックスの項目を削除する
                                customFields.removeAt(index)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "削除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("キャンセル", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tagName.value.isNotBlank()) {
                        onTagSave(tagName.value, selectedIconSource.value, selectedColor.value, customFields.filter { it.isNotBlank() })
                    }
                },
                enabled = tagName.value.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("タグを保存する")
            }
        }
    )

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("カスタム項目とは", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "タグごとに追加の入力項目を設定できる機能です。\n\n" +
                            "予定に必須な情報をフォーマット化し、一目で判断しやすくします。\n\n" +
                            "【設定例】\n" +
                            "・「遊び」タグに「メンバー」\n" +
                            "・「宿題」タグに「提出方法」\n" +
                            "・「会議」タグに「準備物」"
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("閉じる", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}