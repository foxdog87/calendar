package com.foxdog.strucalendar.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch

@Composable
fun MonthYearPickerDialog(
    currentMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {

    var selectedYear by remember {
        mutableIntStateOf(currentMonth.year)
    }

    var selectedMonth by remember {
        mutableIntStateOf(currentMonth.monthValue)
    }

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

            Row(

                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),

                horizontalArrangement = Arrangement.SpaceEvenly,

                verticalAlignment = Alignment.CenterVertically

            ) {

                WheelPicker(


                    label = "年",

                    range = 1900..2100,

                    value = selectedYear,

                    onValueChange = {
                        selectedYear = it
                    },

                    cyclic = false
                )

                WheelPicker(

                    label = "月",

                    range = 1..12,

                    value = selectedMonth,

                    onValueChange = {
                        selectedMonth = it
                    },

                    cyclic = true
                )
            }

        },

        confirmButton = {

            Button(

                onClick = {

                    onConfirm(
                        selectedYear,
                        selectedMonth
                    )

                }

            ) {

                Text("確定")

            }

        },

        dismissButton = {

            TextButton(

                onClick = onDismiss

            ) {

                Text("キャンセル")

            }

        }

    )

}

@Composable
fun WheelPicker(
    label: String,
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    cyclic: Boolean,
    step: Int = 1
) {
    // stepを考慮した値のリストを生成
    val values = remember(range, step) {
        range.step(step).toList()
    }

    val repeatedValues = remember(values, cyclic) {
        if (!cyclic) {
            values
        } else {
            buildList {
                repeat(100) {
                    addAll(values)
                }
            }
        }
    }

    val startIndex = remember(value) {
        if (!cyclic) {
            values.indexOf(value).coerceAtLeast(0)
        } else {
            val middle = repeatedValues.size / 2
            val index = values.indexOf(value).let { if (it == -1) 0 else it }
            middle - middle % values.size + index
        }
    }

    val listState = rememberLazyListState(startIndex)

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo

            val viewportCenter =
                (layoutInfo.viewportStartOffset +
                        layoutInfo.viewportEndOffset) / 2

            layoutInfo.visibleItemsInfo
                .minByOrNull { item ->
                    kotlin.math.abs(
                        (item.offset + item.size / 2) - viewportCenter
                    )
                }
                ?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(value) {
        val current =
            listState.firstVisibleItemIndex

        val target = if (!cyclic) {
            values.indexOf(value).coerceAtLeast(0)
        } else {
            val middle =
                repeatedValues.size / 2

            val index = values.indexOf(value).let { if (it == -1) 0 else it }
            middle -
                    middle % values.size +
                    index
        }

        if (kotlin.math.abs(current - target) > 1) {
            listState.scrollToItem(target)
        } else {
            listState.animateScrollToItem(target)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val center = centeredIndex
            if (center in repeatedValues.indices) {
                val newValue = repeatedValues[center]

                if (newValue != value) {
                    onValueChange(newValue)
                }
            }

            coroutineScope.launch {
                listState.animateScrollToItem(center)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .height(170.dp)
                .width(110.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(10.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 63.dp)
            ) {
                itemsIndexed(repeatedValues) { index, item ->
                    val distance =
                        kotlin.math.abs(
                            centeredIndex - index
                        )

                    val scale =
                        when {
                            distance == 0 -> 1f
                            distance == 1 -> 0.85f
                            else -> 0.7f
                        }

                    val alpha =
                        when {
                            distance == 0 -> 1f
                            distance == 1 -> 0.6f
                            else -> 0.25f
                        }

                    val color by animateColorAsState(
                        if (distance == 0)
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray,
                        label = ""
                    )

                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.toString(),
                            color = color,
                            modifier = Modifier
                                .scale(scale)
                                .alpha(alpha),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = if (distance == 0)
                                FontWeight.Bold
                            else
                                FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}