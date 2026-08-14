package com.example.calendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.calendar.data.entity.Tag
import kotlinx.coroutines.isActive

private data class PositionedTag(
    val tag: Tag,
    val bounds: Rect
)

/**
 * [tags]（ドラッグ中のタグを除いたリスト）の実レイアウト座標から、挿入スロットを返す。
 *
 * 戻り値は [tags] 内の index なので、0 は先頭、tags.size は末尾を表す。
 * itemBounds に [tags] 全員分の座標が揃っていない場合は null を返す
 * （＝そのフレームのレイアウトがまだ追いついていないので計算をスキップすべき、というサイン）。
 */
private fun calculateInsertionIndex(
    tags: List<Tag>,
    pointerPositionInRoot: Offset,
    itemBounds: Map<Long, Rect>
): Int? {
    val positionedTags = tags.mapNotNull { tag ->
        itemBounds[tag.tagId]?.let { bounds -> PositionedTag(tag, bounds) }
    }.sortedWith(compareBy<PositionedTag> { it.bounds.top }.thenBy { it.bounds.left })

    // remainingTags 全員分の座標が揃っていない＝レイアウトが前回の並び替えに
    // まだ追いついていない状態。ここで古い座標のまま計算すると、境界付近
    // （行の端など）で誤ったインデックスを出しやすいので、このフレームは
    // 判定を見送って次のフレームに任せる。
    if (positionedTags.size != tags.size) return null
    if (positionedTags.isEmpty()) return 0

    val rows = mutableListOf<MutableList<PositionedTag>>()
    positionedTags.forEach { positionedTag ->
        val row = rows.lastOrNull()
        val belongsToCurrentRow = row != null &&
                positionedTag.bounds.top <= row.maxOf { it.bounds.bottom } + 1f

        if (belongsToCurrentRow) {
            row!!.add(positionedTag)
        } else {
            rows += mutableListOf(positionedTag)
        }
    }

    val targetRow = rows.minBy { row ->
        val top = row.minOf { it.bounds.top }
        val bottom = row.maxOf { it.bounds.bottom }
        when {
            pointerPositionInRoot.y < top -> top - pointerPositionInRoot.y
            pointerPositionInRoot.y > bottom -> pointerPositionInRoot.y - bottom
            else -> 0f
        }
    }.sortedBy { it.bounds.left }

    val firstTagToTheRight = targetRow.firstOrNull {
        pointerPositionInRoot.x < it.bounds.center.x
    }

    return if (firstTagToTheRight == null) {
        tags.indexOf(targetRow.last().tag) + 1
    } else {
        tags.indexOf(firstTagToTheRight.tag)
    }
}

/**
 * 表示中のサブセット([tags])を並び替えた結果を、非表示タグの位置を保ったまま
 * 全体の順序([allTags])へマージする。
 *
 * [allTags] に含まれないタグが [tags] にある場合や、[tags] が [allTags] の
 * サブセットでない場合は呼び出し側の不整合なので、そのまま [tags] を返す。
 */
private fun mergeReorderedSubsetIntoFullList(allTags: List<Tag>, reorderedTags: List<Tag>): List<Tag> {
    val reorderedIds = reorderedTags.map { it.tagId }.toSet()
    if (!allTags.map { it.tagId }.containsAll(reorderedIds)) return reorderedTags

    val iterator = reorderedTags.iterator()
    return allTags.map { tag -> if (tag.tagId in reorderedIds) iterator.next() else tag }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DraggableTagList(
    tags: List<Tag>,
    selectedTags: List<Tag>,
    onTagClick: (Tag) -> Unit,
    onOrderChanged: (List<Tag>) -> Unit,
    onDeleteTagRequest: (Tag) -> Unit,
    // 全タグの中の一部（例:「さらに表示」で折りたたまれた状態）だけを [tags] に渡す場合に指定する。
    // 未指定時は [tags] がそのまま全体とみなされる。
    // ドラッグで並び替えた結果は、非表示タグの位置を保ったままこの [allTags] へマージしてから
    // [onOrderChanged] に渡される。
    allTags: List<Tag> = tags
) {
    var orderedTags by remember { mutableStateOf(tags) }
    var draggedTagId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartPointerInRoot by remember { mutableStateOf<Offset?>(null) }
    var pointerPositionInRoot by remember { mutableStateOf<Offset?>(null) }
    var draggedStartBounds by remember { mutableStateOf<Rect?>(null) }
    var containerBounds by remember { mutableStateOf<Rect?>(null) }

    // 座標は並び順ではなく tagId で追跡する。再レイアウト後も現在の orderedTags の
    // 各タグだけを判定に使うため、古い index やドラッグ対象の古い座標を参照しない。
    val itemBounds = remember { mutableMapOf<Long, Rect>() }
    val itemCoordinates = remember { mutableMapOf<Long, LayoutCoordinates>() }
    var trashBounds by remember { mutableStateOf<Rect?>(null) }
    var isHoveringTrash by remember { mutableStateOf(false) }

    // 外部から渡される tags が変わったとき、ドラッグ中でなければ表示を同期する。
    // ドラッグ中に無条件で同期してしまうと、親の再コンポーズのたびに
    // 並び替え中のリストが巻き戻ってしまう（「関係ないタグの並びが勝手に変わる」
    // 症状の一因になり得る）。
    LaunchedEffect(tags) {
        if (draggedTagId == null) {
            orderedTags = tags
        }
    }

    fun clearDragState() {
        draggedTagId = null
        dragOffset = Offset.Zero
        dragStartPointerInRoot = null
        pointerPositionInRoot = null
        draggedStartBounds = null
        isHoveringTrash = false
    }

    fun cancelDrag() {
        // ドラッグ中に行った並び替えは onOrderChanged でまだ確定していないので、
        // 表示上の順序を親から渡された最新の tags に戻してから状態をクリアする。
        orderedTags = tags
        clearDragState()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerBounds = coordinates.boundsInRoot()
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                orderedTags.forEach { tag ->
                    key(tag.tagId) {
                        val isSelected = selectedTags.any { it.tagId == tag.tagId }
                        val isDragged = draggedTagId == tag.tagId

                        // 重要: ここで別ノード（Spacerなど）に切り替えない。
                        // isDragged が true になった瞬間にノードを差し替えると、
                        // このノードにぶら下がっている pointerInput(detectDragGestures) の
                        // コルーチンごと破棄され、ドラッグ開始直後にジェスチャーが
                        // 強制終了してしまう。常に同じ TagLabel ノードを維持し、
                        // alpha だけで「空白」に見せることでノードの寿命を途切れさせない。
                        TagLabel(
                            tag = tag,
                            textSize = 13.sp,
                            isSelected = isSelected,
                            modifier = Modifier
                                .height(32.dp)
                                .onGloballyPositioned { coordinates ->
                                    itemBounds[tag.tagId] = coordinates.boundsInRoot()
                                    itemCoordinates[tag.tagId] = coordinates
                                }
                                .graphicsLayer { alpha = if (isDragged) 0f else 1f }
                                .pointerInput(tag.tagId) {
                                    detectDragGestures(
                                        onDragStart = { startPosition ->
                                            val coordinates = itemCoordinates[tag.tagId]
                                            val bounds = itemBounds[tag.tagId]
                                            val startInRoot = coordinates?.localToRoot(startPosition)
                                                ?: bounds?.topLeft?.plus(startPosition)
                                                ?: return@detectDragGestures

                                            draggedTagId = tag.tagId
                                            draggedStartBounds = bounds
                                            dragOffset = Offset.Zero
                                            dragStartPointerInRoot = startInRoot
                                            pointerPositionInRoot = startInRoot
                                            isHoveringTrash = trashBounds?.contains(startInRoot) == true
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            // ここでは「指の生座標」を保存するだけにとどめる。
                                            // 並び替えの計算（orderedTags の更新）はここでは行わず、
                                            // 下の LaunchedEffect が 1 フレームに 1 回だけ行う。
                                            // こうしないと、1フレームに複数回 onDrag が呼ばれた際に
                                            // まだレイアウトが追いついていない古い座標で
                                            // 再計算してしまい、境界（行の端など）付近で
                                            // 誤判定・ちらつきの原因になる。
                                            val newOffset = dragOffset + dragAmount
                                            val startInRoot = dragStartPointerInRoot
                                                ?: return@detectDragGestures
                                            val pointer = startInRoot + newOffset

                                            dragOffset = newOffset
                                            pointerPositionInRoot = pointer
                                            isHoveringTrash = trashBounds?.contains(pointer) == true
                                        },
                                        onDragEnd = {
                                            val draggedId = draggedTagId
                                            val pointer = pointerPositionInRoot

                                            if (draggedId != null && pointer != null) {
                                                if (trashBounds?.contains(pointer) == true) {
                                                    orderedTags
                                                        .firstOrNull { it.tagId == draggedId }
                                                        ?.let(onDeleteTagRequest)
                                                } else {
                                                    // 順序はドラッグ中の per-frame 更新で確定済み。
                                                    // ここで再計算・再挿入はしない。
                                                    // orderedTags は「表示中サブセット」の並び順なので、
                                                    // 非表示タグの位置を保ったまま allTags 全体へマージしてから通知する。
                                                    onOrderChanged(mergeReorderedSubsetIntoFullList(allTags, orderedTags))
                                                }
                                            }
                                            clearDragState()
                                        },
                                        onDragCancel = ::cancelDrag
                                    )
                                }
                                .clickable { onTagClick(tag) }
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = draggedTagId != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(60.dp)
                        .onGloballyPositioned { coordinates ->
                            trashBounds = coordinates.boundsInRoot()
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHoveringTrash) Color(0xFFD93025) else Color(0xFFF1F3F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = if (isHoveringTrash) Color.White else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ここにドロップして削除",
                            color = if (isHoveringTrash) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 指に追従する本体は FlowRow と別レイヤーに置く。これで FlowRow の再レイアウトと
        // ドラッグ表示が干渉しない。
        val draggedTag = draggedTagId?.let { id -> orderedTags.firstOrNull { it.tagId == id } }
        val startBounds = draggedStartBounds
        val rootContainerBounds = containerBounds
        if (draggedTag != null && startBounds != null && rootContainerBounds != null) {
            TagLabel(
                tag = draggedTag,
                textSize = 13.sp,
                isSelected = selectedTags.any { it.tagId == draggedTag.tagId },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .height(32.dp)
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = startBounds.left - rootContainerBounds.left + dragOffset.x
                        translationY = startBounds.top - rootContainerBounds.top + dragOffset.y
                        scaleX = 1.1f
                        scaleY = 1.1f
                        alpha = 0.9f
                    }
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        // 並び替え計算はここで「1フレームにつき1回」だけ行う。
        // withFrameNanos で次の描画フレームまで待つことで、直前の orderedTags 更新が
        // 確実にレイアウトへ反映された（＝itemBounds が最新になった）状態で
        // 次の計算ができる。draggedTagId が null になった瞬間このエフェクトは
        // 自動的にキャンセルされる。
        LaunchedEffect(draggedTagId) {
            val draggedId = draggedTagId ?: return@LaunchedEffect
            while (isActive) {
                withFrameNanos { }

                val currentDraggedId = draggedTagId ?: break
                if (currentDraggedId != draggedId) break
                val pointer = pointerPositionInRoot ?: continue
                if (isHoveringTrash) continue

                val draggedTagNow = orderedTags.firstOrNull { it.tagId == draggedId } ?: continue
                val remainingTags = orderedTags.filter { it.tagId != draggedId }

                val insertionIndex = calculateInsertionIndex(
                    tags = remainingTags,
                    pointerPositionInRoot = pointer,
                    itemBounds = itemBounds
                ) ?: continue // このフレームはまだ座標が揃っていない → 次フレームへ持ち越し

                val reorderedTags = remainingTags.toMutableList().apply {
                    add(insertionIndex.coerceIn(0, size), draggedTagNow)
                }

                if (reorderedTags != orderedTags) {
                    orderedTags = reorderedTags
                }
            }
        }
    }
}