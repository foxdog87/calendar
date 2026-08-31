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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
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

    /**
     * リマインダー通知をONにした瞬間にのみ呼ばれる想定の権限要求関数。
     * 通知権限(Android 13+)が無ければリクエストし、正確なアラーム権限(Android 12+)が
     * 無ければ設定画面へ遷移する。両方とも既に許可済みなら何もしない。
     */
    private fun ensureNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
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

        // 通知権限・正確なアラーム権限は、ユーザーがタスク作成画面で
        // リマインダー通知をONにした瞬間にのみ要求する（起動直後の権限ダイアログはUXを妨げるため廃止）。
        // 具体的な要求処理は ensureNotificationPermissions() に委譲し、
        // Compose側（ReminderSectionのスイッチ）からコールバック経由で呼び出す。

        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(
            context = applicationContext,
            scope = lifecycleScope
        )

        val taskDao = database.taskDao()
        val calendarPreferences = CalendarPreferences(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext)
        val holidayRepository = HolidayRepository(database.holidayDao())
        val calendarTagRepository = com.foxdog.strucalendar.data.repository.TagRepository(
            tagDao = database.tagDao(),
            tagDisplayOrderDao = database.tagDisplayOrderDao(),
            taskTagDao = database.taskTagDao(),
            tagCustomFieldDao = database.tagCustomFieldDao()
        )
        val deviceCountryCode = resolveDeviceCountryCode(applicationContext) // 命名をdeviceCountryCodeに統一

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
                            calendarTagRepository,
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

            // ステータスバー・ナビゲーションバーのアイコン色を、システムのテーマではなく
            // アプリ自身が現在採用しているテーマ（useDarkTheme）に合わせて決定する。
            // enableEdgeToEdge()の既定動作はシステムのライト/ダーク判定に依存するため、
            // 「システムはライトのままアプリだけダークにする」場合にアイコンが
            // 黒背景に黒文字で見えなくなる問題が発生していた。
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
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
                    settingsRepository = settingsRepository, // DateDetailViewModelへの配線に必要
                    countryCode = deviceCountryCode,
                    tagCustomFieldDao = database.tagCustomFieldDao(),
                    onNotificationPermissionNeeded = { ensureNotificationPermissions() },
                    initialTaskId = if (targetTaskId != -1L)
                        targetTaskId
                    else
                        null
                )
            }
        }
    }
}