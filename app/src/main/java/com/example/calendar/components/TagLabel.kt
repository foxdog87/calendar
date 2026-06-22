package com.example.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.data.entity.Tag

/**
 * 1. 【新設】タグのアイコン部分だけを独立させた丸背景バッジ
 * カレンダーのマスや、日付詳細画面の小さなアイコンはこれを直接呼び出します
 */


/**
 * 2. 【修正】TagIconBadge とテキストを組み合わせた統合コンポーネント
 * タスク一覧のフィルターやカード内のタグ表示に使われます
 */
@Composable
fun TagLabel(
    tag: Tag,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 12.sp,
    isSelected: Boolean = true
) {
    // 選択状態に応じた背景・文字・枠線のカラー設計
    val chipBgColor =
        if (isSelected) Color(tag.color).copy(alpha = 0.15f)
        else Color(0xFFF1F3F4)

    val chipTextColor =
        if (isSelected) Color(0xFF1C1B1F)
        else Color(0xFF5F6368)

    val borderStrokeColor =
        if (isSelected) Color(tag.color)
        else Color(0xFFE0E0E0)

    Row(
        modifier = modifier
            .background(chipBgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 先ほど切り出した TagIconBadge をここで再利用
        TagIconBadge(
            tag = tag,
            size = 20.dp,
            iconSize = 12.dp
        )

        // アイコンが存在する場合のみテキストとの間にスペースを空ける
        if (!tag.icon.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = tag.name,
            color = chipTextColor,
            fontSize = textSize,
            fontWeight = FontWeight.Bold
        )
    }
}