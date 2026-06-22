package com.example.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.calendar.data.AppDatabase
import com.example.calendar.ui.theme.CalendarTheme
import com.example.calendar.viewmodel.CalendarViewModel
import android.Manifest
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(
            context = applicationContext,
            scope = lifecycleScope
        )

        val taskDao = database.taskDao()

        setContent {
            CalendarTheme {
                // 2. ★修正：ファクトリを利用し、CalendarViewModelにtaskDaoを正しく注入して生成する
                val mainViewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CalendarViewModel(taskDao) as T
                        }
                    }
                )

                // 3. ナビゲーションハブに2つの依存関係を正しく託す
                AppNavigation(
                    taskDao = taskDao,
                    calendarViewModel = mainViewModel
                )
            }
        }
    }
}