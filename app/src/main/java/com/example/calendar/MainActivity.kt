package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.calendar.ui.theme.CalendarTheme
import java.time.LocalDate
import java.time.YearMonth


@Composable
fun CalendarScreen() {
    var currentMonth by remember {
        mutableStateOf(YearMonth.now())
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ){
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {
                        currentMonth = currentMonth.minusMonths(1)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "前の月"
                    )
                }
                Text(
                    text = currentMonth.toString(),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                IconButton(
                    onClick = {
                        currentMonth = currentMonth.plusMonths(1)
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "次の月"
                    )
                }
            }
        }
    }
}

fun buildMonthDates(yearMonth: YearMonth):List<LocalDate?>{
    val dates = mutableListOf<LocalDate?>()

    val firstDay = yearMonth.atDay(1)
    val offset = firstDay.dayOfWeek.value % 7

    repeat(offset){
        dates.add(null)
    }

    val lastDay = yearMonth.lengthOfMonth()
    for (day in 1..lastDay){
        dates.add(yearMonth.atDay(day))
    }
    return dates
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalendarTheme {
                CalendarScreen()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CalenderScreenPreview() {
    CalendarTheme {
        CalendarScreen()
    }
}