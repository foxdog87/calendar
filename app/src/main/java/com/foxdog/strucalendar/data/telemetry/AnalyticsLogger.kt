package com.foxdog.strucalendar.data.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics


/**
 * Firebase Analyticsへのイベント送信をまとめる薄いラッパー。
 * Firestoreへは一切通信しない。ここで送ったイベントの発生回数・DAUを
 * Firebaseコンソール上で確認し、将来Firestoreへ移行した場合の
 * read/write数の見積もりに使う。
 */
object AnalyticsLogger {
    private lateinit var analytics: FirebaseAnalytics

    /** MainActivity.onCreate() など、アプリ起動時に1回だけ呼ぶ。 */
    fun init(context: Context) {
        analytics = Firebase.analytics
    }

    fun logAppOpened() = log("app_opened")

    fun logCalendarOpened() = log("calendar_opened")

    fun logDateDetailOpened() = log("date_detail_opened")

    fun logTaskDetailOpened() = log("task_detail_opened")

    fun logTaskCreated() = log("task_created")

    fun logTaskUpdated() = log("task_updated")

    fun logTaskDeleted() = log("task_deleted")

    fun logTaskCompletionToggled() = log("task_completion_toggled")

    fun logChecklistItemToggled() = log("checklist_item_toggled")

    fun logTagCreated() = log("tag_created")

    fun logTagUpdated() = log("tag_updated")

    fun logTagDeleted() = log("tag_deleted")

    fun logTagOrderChanged() = log("tag_order_changed")

    fun logTemplateCreated() = log("template_created")

    fun logTemplateUpdated() = log("template_updated")

    fun logTemplateApplied() = log("template_applied")

    fun logSettingChanged(settingName: String) {
        if (!::analytics.isInitialized) return
        val params = Bundle().apply { putString("setting_name", settingName) }
        analytics.logEvent("setting_changed", params)
    }

    private fun log(eventName: String) {
        if (!::analytics.isInitialized) return
        analytics.logEvent(eventName, null)
    }
}