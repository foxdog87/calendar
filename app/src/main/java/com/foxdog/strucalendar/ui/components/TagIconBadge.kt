package com.foxdog.strucalendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.foxdog.strucalendar.data.entity.Tag

@Composable
fun TagIconBadge(
    tag: Tag,
    isSelected: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    iconSize: Dp = 12.dp
) {
    // 唯一の管理場所である TagIconId から直接 vector を取得
    val imageVector = TagIconId.fromId(tag.icon)?.vector ?: return

    val colorScheme = MaterialTheme.colorScheme
    val badgeColor = if (isSelected) Color(tag.color) else colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isSelected) badgeColor.copy(alpha = 0.12f) else colorScheme.surfaceVariant
            ), // 淡い円形背景でアイコンを強調する（非選択時はグレーに統一）
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = badgeColor, // アイコン本来の色（非選択時はグレー）
            modifier = Modifier.size(iconSize)
        )
    }
}
