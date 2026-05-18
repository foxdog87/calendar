package com.example.calendar.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.viewmodel.CalendarViewModel

@Composable
fun TaskCreateScreen(viewModel: CalendarViewModel) {
    // ViewModelから現在の「下書き状態」を取得
    val taskState = viewModel.inputState
    // 画面が縦に長くなってもスクロールできるようにする状態
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState), // ★追加：画面をスクロール可能にする
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "予定の作成", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // 1. タイトル入力欄
        TextField(
            value = taskState.title,
            onValueChange = { newText ->
                viewModel.updateInput { it.copy(title = newText) }
            },
            label = { Text("タイトル") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 場所入力欄 (★新設：ER図の location に対応)
        TextField(
            value = taskState.location,
            onValueChange = { newLocation ->
                viewModel.updateInput { it.copy(location = newLocation) }
            },
            label = { Text("場所") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. URL入力欄 (★新設：ER図の url に対応)
        TextField(
            value = taskState.url,
            onValueChange = { newUrl ->
                viewModel.updateInput { it.copy(url = newUrl) }
            },
            label = { Text("URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. チェックリスト入力欄 (★新設：ER図の checkList に対応)
        TextField(
            value = taskState.checkList,
            onValueChange = { newCheckList ->
                viewModel.updateInput { it.copy(checkList = newCheckList) }
            },
            label = { Text("チェックリスト（改行で複数入力）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3 // 少し広めの入力欄にする
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. メモ入力欄
        TextField(
            value = taskState.memo,
            onValueChange = { newMemo ->
                viewModel.updateInput { it.copy(memo = newMemo) }
            },
            label = { Text("メモ（任意）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 6. 保存ボタン
        Button(
            onClick = {
                viewModel.saveManualTask()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存する")
        }
    }
}