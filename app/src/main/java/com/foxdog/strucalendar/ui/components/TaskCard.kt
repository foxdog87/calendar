package com.foxdog.strucalendar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foxdog.strucalendar.components.TagLabel
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.ui.bounceClick
import com.foxdog.strucalendar.ui.theme.calendarColors
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TaskCard(
    item: TaskWithTags,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onToggleTaskCompletion: (TaskWithTags) -> Unit,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val calColors = MaterialTheme.calendarColors

    val task = item.task

    val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.startTime), ZoneId.systemDefault())
    val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(task.endTime), ZoneId.systemDefault())

    val isCompleted = task.completeState == "COMPLETED"
    val isExpired = !isCompleted && !task.isAutoCompleted && !task.isAllDay && endDateTime.isBefore(LocalDateTime.now())


    val firstTag = item.tags.firstOrNull()
    val baseColor = if (firstTag != null) Color(firstTag.color) else (if (task.color == 0) colorScheme.primary else Color(task.color))
    val contentColor = if (isCompleted) colorScheme.onSurfaceVariant else baseColor

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) colorScheme.surfaceVariant else colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCompleted) 0.dp else 1.5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
                    .bounceClick(showWave = true, isWaveCircle = true, waveExpansionSize = 12.dp) {
                        onToggleTaskCompletion(item)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "ステータス変更",
                    tint = if (isCompleted) calColors.success else if (isExpired) colorScheme.error else contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCompleted || isExpired) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCompleted) calColors.successContainer else colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "完了" else "期限切れ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) calColors.onSuccessContainer else colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (task.recurrenceGroupId != null) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "繰り返しタスク",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${startDateTime.format(dateFormatter)} ${startDateTime.format(timeFormatter)} 〜 ${endDateTime.format(timeFormatter)}",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )

                val hasReminder = task.reminderType != null || task.reminderOffsetMinutes != null

                if (item.tags.isNotEmpty() || hasReminder) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item.tags.forEach { tag ->
                            TagLabel(tag = tag, textSize = 10.sp, isSelected = !isCompleted)
                        }
                        if (hasReminder) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = "通知あり", tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                if (!task.memo.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.memo,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
        }
    }
}