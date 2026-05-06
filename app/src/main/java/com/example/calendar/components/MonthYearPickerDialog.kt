import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.YearMonth

@Composable
fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    // 状態管理：最初は現在の年月をセット
    var tempYear by remember { mutableIntStateOf(currentMonth.year) }
    var tempMonth by remember { mutableIntStateOf(currentMonth.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "年月を選択",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- 年の操作 ---
                    NumberPickerColumn(
                        value = tempYear,
                        label = "年",
                        onValueChange = { tempYear = it },
                        range = 1..3000
                    )

                    // --- 月の操作 ---
                    NumberPickerColumn(
                        value = tempMonth,
                        label = "月",
                        onValueChange = { tempMonth = it },
                        range = 1..12
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(tempYear, tempMonth) }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
fun NumberPickerColumn(
    value: Int, // 親（ダイアログ）から渡される現在の数値
    label: String,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    // ★ポイント1：入力中の文字列を内部で保持する（表示のチラつき防止）
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Icon(Icons.Default.KeyboardArrowUp, null)
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValueString ->
                // まず表示上の文字を更新
                textValue = newValueString

                // 数字として妥当なら親の値を更新する
                val newValueInt = newValueString.toIntOrNull()
                if (newValueInt != null && newValueInt in range) {
                    onValueChange(newValueInt)
                }
            },
            modifier = Modifier.width(90.dp), // 少し幅に余裕を持たせる
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        Text(text = label, style = MaterialTheme.typography.bodySmall)

        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
    }
}