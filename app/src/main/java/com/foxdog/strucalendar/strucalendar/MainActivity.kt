package com.foxdog.strucalendar

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.foxdog.strucalendar.data.AppDatabase
import com.foxdog.strucalendar.data.holiday.HolidayRepository
import com.foxdog.strucalendar.data.preference.CalendarPreferences
import com.foxdog.strucalendar.data.settings.AppThemeMode
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import com.foxdog.strucalendar.ui.theme.CalendarTheme
import com.foxdog.strucalendar.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    private var notificationTaskId: Long? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // 端末の地域設定（ロケール）から国コード（ISO 3166-1 alpha-2、例: "JP"）を判定する。
    // 設定画面で明示的に国が選ばれていない場合のフォールバックとして使う。
    private fun resolveDeviceCountryCode(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        val country = locale.country
        return if (country.isNotBlank()) country.uppercase() else "JP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AnalyticsLogger.init(applicationContext)
        AnalyticsLogger.logAppOpened()


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

        // Android 12以降の「正確なアラーム（スケジュール通知）」の権限要求
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
        val calendarPreferences = CalendarPreferences(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val holidayRepository = HolidayRepository(database.holidayDao())
        val deviceCountryCode = resolveDeviceCountryCode(applicationContext) // ★ 変更：命名をdeviceCountryCodeに統一

        setContent {
            val mainViewModel: CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return CalendarViewModel(
                            taskDao,
                            calendarPreferences,
                            settingsRepository,
                            holidayRepository,
                            deviceCountryCode
                        ) as T
                    }
                }
            )

            val settings by mainViewModel.settings.collectAsState()

            val useDarkTheme = when (settings.themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            CalendarTheme(darkTheme = useDarkTheme) {
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
                    settingsRepository = settingsRepository, // ★ 追加：DateDetailViewModelへの配線に必要
                    countryCode = deviceCountryCode,
                    tagCustomFieldDao = database.tagCustomFieldDao(),
                    initialTaskId = if (targetTaskId != -1L)
                        targetTaskId
                    else
                        null
                )
            }
        }
    }
}