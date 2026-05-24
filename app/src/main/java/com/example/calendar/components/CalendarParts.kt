package com.example.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun DateCell(
    date: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(0.55f) // ★さらに数値を下げて、縦にひょろ長いマスにする
            .border(
                width = if (isToday) 2.dp else 0.5.dp,
                color = if (isToday) Color.Blue else Color.LightGray
            )
            .clickable(enabled = date != null) { onClick() }
            .background(
                if (isSelected) Color(0xFFD1E4FF) // ★ご要望通り、薄い青のまま
                else Color.White
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        if (date != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    // ★文字色は白に変えず、黒系（曜日ごとの色）を維持
                    color = when {
                        date.dayOfWeek.value == 7 -> Color.Red
                        date.dayOfWeek.value == 6 -> Color.Blue
                        else -> Color.Black
                    }
                )
            }
        }
    }
}

@Composable
fun DayOfWeekRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8)) // ほんのりグレーの背景でヘッダー感を出す
            .border(0.5.dp, Color.LightGray) // 下線の代わり
    ) {
        val daysOfWeek = listOf("日", "月", "火", "水", "木", "金", "土")
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp), // 少し縦長にするために余白を増やす
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = when (day) {
                    "日" -> Color.Red
                    "土" -> Color.Blue
                    else -> Color.DarkGray
                }
            )
        }
    }
}