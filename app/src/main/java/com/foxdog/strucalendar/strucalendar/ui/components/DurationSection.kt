package com.foxdog.strucalendar.screens.templatecreate

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foxdog.strucalendar.components.WheelPicker
import com.foxdog.strucalendar.screens.taskcreate.SectionLabel

@Composable
fun DurationSection(
    durationMinutes: Int,
    onDurationChange: (Int) -> Unit
) {
    SectionLabel("タスクの時間の長さ")

    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60

    // ★ Rowを画面幅いっぱいに広げ、中央に配置するよう修正
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPicker(
            value = hours,
            range = 0..23,
            onValueChange = { newHours -> onDurationChange(newHours * 60 + minutes) },
            label = "時間",
            cyclic = false
        )
        Spacer(modifier = Modifier.width(16.dp))
        WheelPicker(
            value = minutes,
            range = 0..55,
            onValueChange = { newMinutes -> onDurationChange(hours * 60 + newMinutes) },
            label = "分",
            cyclic = false,
            step = 5
        )
    }
}