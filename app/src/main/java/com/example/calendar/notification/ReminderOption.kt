package com.example.calendar.notification

// ★ UI表示用の「選択肢」モデル
sealed class ReminderOption(val label: String) {
    data object None : ReminderOption("なし")
    data object AtStartTime : ReminderOption("開始時間")
    data class Before(val minutes: Int, val displayLabel: String) : ReminderOption(displayLabel)
    data class DayBefore(val hour: Int, val minute: Int, val displayLabel: String) : ReminderOption(displayLabel)

    // UIの選択肢 -> Domainモデル(State) への変換
    fun toDomain(): ReminderSetting = when (this) {
        is None -> ReminderSetting.None
        is AtStartTime -> ReminderSetting.AtStartTime
        is Before -> ReminderSetting.Before(minutes)
        is DayBefore -> ReminderSetting.DayBefore(1, hour, minute)
    }

    companion object {
        // ドロップダウンに表示するデフォルトの選択肢リスト
        val defaultOptions = listOf(
            AtStartTime,
            Before(5, "5分前"),
            Before(10, "10分前"),
            Before(30, "30分前"),
            Before(60, "1時間前"),
            Before(120, "2時間前"),
            DayBefore(9, 0, "前日 9:00") // 1440分のハックを廃止し、明確に定義
        )

        // Domainモデル(State) -> UIの選択肢 への逆変換（現在の設定画面を開いた時の表示用）
        fun fromDomain(setting: ReminderSetting): ReminderOption = when (setting) {
            is ReminderSetting.None -> None
            is ReminderSetting.AtStartTime -> AtStartTime
            is ReminderSetting.Before ->
                defaultOptions.filterIsInstance<Before>().find { it.minutes == setting.minutes }
                    ?: Before(setting.minutes, "${setting.minutes}分前")
            is ReminderSetting.DayBefore ->
                defaultOptions.filterIsInstance<DayBefore>().find { it.hour == setting.hour && it.minute == setting.minute }
                    ?: DayBefore(setting.hour, setting.minute, "前日 ${setting.hour}:${String.format("%02d", setting.minute)}")
        }
    }
}