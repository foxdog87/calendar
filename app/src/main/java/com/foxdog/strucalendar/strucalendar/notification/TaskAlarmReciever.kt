package com.foxdog.strucalendar.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.foxdog.strucalendar.R
import com.foxdog.strucalendar.data.AppDatabase
import com.foxdog.strucalendar.data.entity.Task
import com.foxdog.strucalendar.data.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ALARM_TEST", "Receiver called")

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepository = SettingsRepository(appContext)
                val isNotificationEnabled = settingsRepository.settingsFlow.first().isNotificationEnabled

                val task: Task? = AppDatabase
                    .getDatabase(appContext, this)
                    .taskDao()
                    .getTaskById(taskId)

                if (!isNotificationEnabled) {
                    Log.d("ALARM_TEST", "Notification is globally disabled in settings")
                    return@launch
                }
                if (task == null) {
                    Log.d("ALARM_TEST", "task not found")
                    return@launch
                }
                if (task.completeState == "COMPLETED") {
                    Log.d("ALARM_TEST", "completed task")
                    return@launch
                }

                val text = when (val setting = task.getReminderSetting()) {
                    is ReminderSetting.None -> {
                        Log.d("ALARM_TEST", "Notification is disabled in DB")
                        return@launch
                    }
                    is ReminderSetting.AtStartTime -> "開始時間になりました"
                    is ReminderSetting.Before -> "開始の${setting.minutes}分前です"
                    is ReminderSetting.DayBefore -> "明日の予定です"
                }

                // 通知の発行はUI操作を含まないためバックグラウンドスレッドのままでOK
                showNotification(appContext, taskId, task, text)

            } catch (e: Exception) {
                Log.e("ALARM_TEST", "通知処理失敗", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, taskId: Long, task: Task, text: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = NotificationConstants.CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                NotificationConstants.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "予定の事前通知を行います"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(
            context,
            Class.forName("${context.packageName}.MainActivity")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(task.title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId.toInt(), notification)
    }
}