package com.example.calendar.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.viewmodel.CalendarViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class) // ★CenterAlignedTopAppBarの採用に伴い画面全体に付与
@Composable
fun TaskCreateScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }
    val taskState = viewModel.inputState

    // ★追加：編集対象（"START" か "END" か なし）を覚えるフラグだけ画面側に置く
    var activeTarget by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // ==========================================
        // ★変更：手動 Row から CenterAlignedTopAppBar へ統一！
        // ==========================================
        CenterAlignedTopAppBar(
            title = {
                Text(text = "予定の作成", style = MaterialTheme.typography.titleLarge)
            },
            navigationIcon = {
                IconButton(onClick = { onNavigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る"
                    )
                }
            }
        )

// スクロール可能なコンテンツエリア
        Column(
            modifier = Modifier
                .fillMaxWidth() // ★修正：fill TripoliWidth から fillMaxWidth() に直します
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. タイトル入力欄
            TextField(
                value = taskState.title,
                onValueChange = { newTitle -> viewModel.updateInput { currentState -> currentState.copy(title = newTitle) } },
                label = { Text("タイトル") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // ★追加：開始日時 ＆ 終了日時の選択ボタン（ここだけ追加）
            // ==========================================
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { activeTarget = "START" }, modifier = Modifier.weight(1f)) {
                    Text("開始: ${taskState.startTime.format(formatter)}")
                }
                OutlinedButton(onClick = { activeTarget = "END" }, modifier = Modifier.weight(1f)) {
                    Text("終了: ${taskState.endTime.format(formatter)}")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- ★動的入力欄ロジック ---
            val selectedTagIds = taskState.selectedTags.map { it.tagId }
            val activeFields = viewModel.sampleCustomFields.filter { it.tagId in selectedTagIds }
            activeFields.forEach { field ->
                val currentInputValue = taskState.customFieldValues[field] ?: ""
                TextField(
                    value = currentInputValue,
                    onValueChange = { viewModel.updateCustomFieldValue(field, it) },
                    label = { Text(field.fieldName) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. 場所入力欄
            TextField(
                value = taskState.location,
                onValueChange = { newLocation -> // ★修正：itの混同を防วนため明示的に命名
                    viewModel.updateInput { currentState -> currentState.copy(location = newLocation) }
                },
                label = { Text("場所") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 3. URL入力欄
            TextField(
                value = taskState.url,
                onValueChange = { newUrl -> // ★修正
                    viewModel.updateInput { currentState -> currentState.copy(url = newUrl) }
                },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 4. チェックリスト入力欄
            TextField(
                value = taskState.checkList,
                onValueChange = { newCheckList -> // ★修正
                    viewModel.updateInput { currentState -> currentState.copy(checkList = newCheckList) }
                },
                label = { Text("チェックリスト（改行で複数入力）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 5. メモ入力欄
            TextField(
                value = taskState.memo,
                onValueChange = { newMemo -> // ★修正
                    viewModel.updateInput { currentState -> currentState.copy(memo = newMemo) }
                },
                label = { Text("メモ（任意）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 6. 保存ボタン
            Button(
                onClick = {
                    viewModel.saveTask()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存する")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ==========================================
    // ★追加：複雑なダイアログ処理は、外の関数を1行呼ぶだけにして隠す
    // ==========================================
    if (activeTarget != null) {
        DateTimePickerWizard(
            target = activeTarget!!,
            onDismiss = { activeTarget = null },
            onDateTimeSelected = { finalDateTime ->
                viewModel.updateInput {
                    if (activeTarget == "START") it.copy(startTime = finalDateTime) else it.copy(endTime = finalDateTime)
                }
                activeTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class) // ★この関数内で実験的APIを使うことを明示
@Composable
private fun DateTimePickerWizard(
    target: String,
    onDismiss: () -> Unit,
    onDateTimeSelected: (LocalDateTime) -> Unit
) {
    var isTimeStep by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = Instant.now().toEpochMilli())

    // Material 3のTimePickerの状態管理（OptInが必要）
    val now = LocalTime.now()
    val timeState = rememberTimePickerState(
        initialHour = now.hour,
        initialMinute = now.minute,
        is24Hour = true
    )

    if (!isTimeStep) {
        // 【ステップ1】日付選択
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { isTimeStep = true }) { Text("次へ (時間選択)") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
        ) { DatePicker(state = dateState) }
    } else {
        // 【ステップ2】時間選択（Material 3 の TimePicker をダイアログに載せる）
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = dateState.selectedDateMillis ?: Instant.now().toEpochMilli()
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        val time = LocalTime.of(timeState.hour, timeState.minute)
                        onDateTimeSelected(LocalDateTime.of(date, time))
                    }
                ) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { isTimeStep = false }) { Text("戻る") } },
            title = { Text(if (target == "START") "開始時間を設定" else "終了時間を設定") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // ★ここがMaterial 3最新の時計UIコンポーネント（針をぐるぐる回すアニメーションがComposeで描画される）
                    TimePicker(state = timeState)
                }
            }
        )
    }
}