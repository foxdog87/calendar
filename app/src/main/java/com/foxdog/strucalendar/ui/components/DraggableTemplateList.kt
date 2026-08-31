package com.foxdog.strucalendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.foxdog.strucalendar.data.entity.Template
import kotlinx.coroutines.isActive

@Composable
fun DraggableTemplateList(
    templates: List<Template>,
    onOrderChanged: (List<Template>) -> Unit,
    onTemplateClick: (Template) -> Unit,
    onEditClick: (Template) -> Unit,
    onDeleteClick: (Template) -> Unit,
    fieldBackgroundColor: Color,
    templateIconColor: Color,
) {
    val colorScheme = MaterialTheme.colorScheme

    var orderedTemplates by remember { mutableStateOf(templates) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var dragStartPointerY by remember { mutableStateOf<Float?>(null) }
    var pointerY by remember { mutableStateOf<Float?>(null) }
    var draggedStartBounds by remember { mutableStateOf<Rect?>(null) }
    var containerBounds by remember { mutableStateOf<Rect?>(null) }
    val itemBounds = remember { mutableMapOf<Long, Rect>() }

    LaunchedEffect(templates) {
        if (draggedId == null) orderedTemplates = templates
    }

    fun clearDrag() {
        draggedId = null
        dragOffsetY = 0f
        dragStartPointerY = null
        pointerY = null
        draggedStartBounds = null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerBounds = it.boundsInRoot() }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            orderedTemplates.forEach { template ->
                key(template.templateId) {
                    val isDragged = draggedId == template.templateId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { itemBounds[template.templateId] = it.boundsInRoot() }
                            .graphicsLayer { alpha = if (isDragged) 0f else 1f }
                            .clip(RoundedCornerShape(8.dp))
                            .background(fieldBackgroundColor)
                            .clickable(enabled = draggedId == null) { onTemplateClick(template) }
                            .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = templateIconColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = template.title,
                            fontSize = 14.sp,
                            color = colorScheme.onSurface, // Color(0xFF1C1B1F) → テーマ追従
                            modifier = Modifier.weight(1f)
                        )

                        // 編集ボタン
                        IconButton(
                            onClick = { onEditClick(template) },
                            enabled = draggedId == null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "テンプレートを編集",
                                tint = colorScheme.onSurfaceVariant, // Color(0xFF5F6368) → テーマ追従
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 削除ボタン
                        IconButton(
                            onClick = { onDeleteClick(template) },
                            enabled = draggedId == null,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "テンプレートを削除",
                                tint = colorScheme.error, // Color(0xFFD93025) → テーマのerror色
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // ドラッグハンドル
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = "並び替え",
                            tint = colorScheme.onSurfaceVariant, // Color(0xFF9AA0A6) → テーマ追従
                            modifier = Modifier
                                .padding(start = 4.dp, end = 6.dp)
                                .pointerInput(template.templateId) {
                                    detectDragGestures(
                                        onDragStart = { startPosition ->
                                            val bounds = itemBounds[template.templateId] ?: return@detectDragGestures
                                            val startY = bounds.top + startPosition.y
                                            draggedId = template.templateId
                                            draggedStartBounds = bounds
                                            dragOffsetY = 0f
                                            dragStartPointerY = startY
                                            pointerY = startY
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val startY = dragStartPointerY ?: return@detectDragGestures
                                            dragOffsetY += dragAmount.y
                                            pointerY = startY + dragOffsetY
                                        },
                                        onDragEnd = {
                                            onOrderChanged(orderedTemplates)
                                            clearDrag()
                                        },
                                        onDragCancel = {
                                            orderedTemplates = templates
                                            clearDrag()
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }

        val draggedTemplate = draggedId?.let { id -> orderedTemplates.firstOrNull { it.templateId == id } }
        val startBounds = draggedStartBounds
        val rootBounds = containerBounds
        if (draggedTemplate != null && startBounds != null && rootBounds != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = startBounds.left - rootBounds.left
                        translationY = startBounds.top - rootBounds.top + dragOffsetY
                        alpha = 0.95f
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(fieldBackgroundColor)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = templateIconColor)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = draggedTemplate.title,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface, // Color(0xFF1C1B1F) → テーマ追従
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant // Color(0xFF9AA0A6) → テーマ追従
                )
            }
        }

        LaunchedEffect(draggedId) {
            val id = draggedId ?: return@LaunchedEffect
            while (isActive) {
                withFrameNanos { }
                if (draggedId != id) break
                val y = pointerY ?: continue

                val others = orderedTemplates.filter { it.templateId != id }
                val positioned = others.mapNotNull { t -> itemBounds[t.templateId]?.let { t to it } }
                if (positioned.size != others.size) continue

                val insertionIndex = positioned.indexOfFirst { (_, bounds) -> y < bounds.center.y }
                    .let { if (it == -1) others.size else it }

                val draggedTemplateItem = orderedTemplates.firstOrNull { it.templateId == id } ?: continue
                val reordered = others.toMutableList().apply {
                    add(insertionIndex.coerceIn(0, size), draggedTemplateItem)
                }
                if (reordered != orderedTemplates) orderedTemplates = reordered
            }
        }
    }
}