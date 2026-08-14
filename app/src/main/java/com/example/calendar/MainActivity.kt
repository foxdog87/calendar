package com.example.calendar

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.calendar.data.AppDatabase
import com.example.calendar.ui.theme.CalendarTheme
import com.example.calendar.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    private var notificationTaskId: Long? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetTaskId =
            intent.getLongExtra(
                "TARGET_TASK_ID",
                -1L
            )


        notificationTaskId =
            intent.getLongExtra(
                "TARGET_TASK_ID",
                -1L
            ).takeIf {
                it != -1L
            }
        android.util.Log.d(
            "NOTIFICATION_DEBUG",
            "targetTaskId=$notificationTaskId"
        )

        // 1. Android 13以降の通常通知権限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // ★ ⑮追加：Android 12以降の「正確なアラーム（スケジュール通知）」の権限要求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(
            context = applicationContext,
            scope = lifecycleScope
        )

        val taskDao = database.taskDao()

        setContent {
            CalendarTheme {
                val mainViewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CalendarViewModel(taskDao) as T
                        }
                    }
                )

                AppNavigation(
                    taskDao = taskDao,
                    tagDao = database.tagDao(),
                    tagDisplayOrderDao = database.tagDisplayOrderDao(),
                    taskTagDao = database.taskTagDao(),
                    calendarViewModel = mainViewModel,
                    checklistItemDao = database.checklistItemDao(),
                    templateChecklistItemDao = database.templateChecklistItemDao(),
                    database = database,
                    templateDao = database.templateDao(),
                    templateTagDao = database.templateTagDao(),
                    templateDisplayOrderDao = database.templateDisplayOrderDao(),
                    initialTaskId = if (targetTaskId != -1L)
                        targetTaskId
                    else
                        null
                )
            }
        }
    }
}
