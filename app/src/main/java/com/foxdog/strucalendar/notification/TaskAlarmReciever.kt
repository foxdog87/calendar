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

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ALARM_TEST", "Receiver called")

        val taskId = intent.getLongExtra("TASK_ID", -1L)
        if (taskId == -1L) return

        // データベースから最新のタスク情報を取得しつつ、通知の全体設定も同じブロックで確認する
        val (isNotificationEnabled, task) = runBlocking {
            val settingsRepository = SettingsRepository(context.applicationContext)
            val enabled = settingsRepository.settingsFlow.first().isNotificationEnabled

            val fetchedTask: Task? = AppDatabase
                .getDatabase(
                    context.applicationContext,
                    CoroutineScope(Dispatchers.IO)
                )
                .taskDao()
                .getTaskById(taskId)

            enabled to fetchedTask
        }

        // ★ セーフティネット：AlarmManager側のキャンセルが漏れていても、
        // 設定画面で通知が全体OFFになっていればここで確実にブロックする
        if (!isNotificationEnabled) {
            Log.d("ALARM_TEST", "Notification is globally disabled in settings")
            return
        }

        if (task == null) {
            Log.d("ALARM_TEST", "task not found")
            return
        }

        if (task.completeState == "COMPLETED") {
            Log.d("ALARM_TEST", "completed task")
            return
        }

        // ★ 新しい通知設定モデル (Domain) を取得し、通知テキストを生成
        val text = when (val setting = task.getReminderSetting()) {
            is ReminderSetting.None -> {
                // 万が一アラームのキャンセルが漏れていても、設定がOFFならここでブロックする（安全装置）
                Log.d("ALARM_TEST", "Notification is disabled in DB")
                return
            }
            is ReminderSetting.AtStartTime -> "開始時間になりました"
            is ReminderSetting.Before -> "開始の${setting.minutes}分前です"
            is ReminderSetting.DayBefore -> "明日の予定です"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = NotificationConstants.CHANNEL_ID

        // Android 8.0以上は通知チャンネルが必須
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

        // 通知のビルド
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_calendar)
            // ★ Intentからではなく、常にDBの最新のタイトルを使用する
            .setContentTitle(task.title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // 通知を発行（通知IDにtaskIdを使うことで重複を防ぐ）
        notificationManager.notify(taskId.toInt(), notification)
    }
}