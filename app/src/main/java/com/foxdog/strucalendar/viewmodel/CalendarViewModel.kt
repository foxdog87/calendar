package com.foxdog.strucalendar.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foxdog.strucalendar.data.dao.TaskDao
import com.foxdog.strucalendar.data.holiday.HolidayRepository
import com.foxdog.strucalendar.data.preference.CalendarPreferences
import com.foxdog.strucalendar.data.relation.TaskWithTags
import com.foxdog.strucalendar.data.settings.AppSettings
import com.foxdog.strucalendar.data.settings.SettingsRepository
import com.foxdog.strucalendar.data.telemetry.AnalyticsLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields

enum class CalendarDisplayMode {
    YEAR, MONTH, WEEK
}

class CalendarViewModel(
    private val taskDao: TaskDao,
    private val calendarPreferences: CalendarPreferences,
    private val settingsRepository: SettingsRepository,
    private val holidayRepository: HolidayRepository,
    private val deviceCountryCode: String, // ★ 変更：端末ロケールから判定した国コード。設定で上書きされていない場合のフォールバック
    initialMonth: YearMonth = YearMonth.now(),
    initialDateTime: LocalDateTime = LocalDateTime.now()
) : ViewModel() {

    // =================================================================
    // 0. アプリ全体設定の反映
    // =================================================================

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    private fun currentWeekStartDay(): DayOfWeek = settings.value.weekStartDay

    // ★ 追加：設定で明示的に選ばれた国があればそちらを優先し、なければ端末ロケール判定に従う
    private fun effectiveCountryCode(): String = settings.value.holidayCountryCode ?: deviceCountryCode

    // =================================================================
    // 1. データベース（Room）連携ロジック
    // =================================================================

    val tasksByDate: StateFlow<Map<LocalDate, List<TaskWithTags>>> = taskDao.getAllTasksWithTags()
        .map { totalList ->
            totalList.groupBy { item ->
                LocalDateTime.ofInstant(Instant.ofEpochSecond(item.task.startTime), ZoneId.systemDefault()).toLocalDate()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // =================================================================
    // 1.5. 祝日データ（Nager.Date API + Roomキャッシュ）
    // =================================================================

    private val _holidayMap = MutableStateFlow<Map<LocalDate, String>>(emptyMap())
    val holidayMap: StateFlow<Map<LocalDate, String>> = _holidayMap

    private val loadedHolidayYears = mutableSetOf<Int>()

    /**
     * 指定年の祝日（現在有効な国のみ）がまだ読み込まれていなければ取得する（DBキャッシュ優先、無ければAPI）。
     * 呼び出し元は年をまたぐ操作（月送り・週送り・年表示切替）のたびにこれを呼ぶ。
     */
    fun ensureHolidaysLoaded(year: Int) {
        if (loadedHolidayYears.contains(year)) return
        loadedHolidayYears.add(year)

        val code = effectiveCountryCode()
        viewModelScope.launch {
            val yearMap = holidayRepository.getHolidayMap(year, code)
            _holidayMap.value = _holidayMap.value + yearMap
        }
    }

    // ★ 追加：設定画面で祝日の対象国が変更されたら、キャッシュを破棄して読み込み済みの年を新しい国で取り直す
    private fun observeHolidayCountryChanges() {
        var lastAppliedCode: String? = null

        viewModelScope.launch {
            settings.map { it.holidayCountryCode }
                .distinctUntilChanged()
                .collect {
                    val newCode = effectiveCountryCode()
                    if (lastAppliedCode != null && lastAppliedCode != newCode) {
                        val yearsToReload = loadedHolidayYears.toList()
                        _holidayMap.value = emptyMap()
                        loadedHolidayYears.clear()
                        yearsToReload.forEach { ensureHolidaysLoaded(it) }
                    }
                    lastAppliedCode = newCode
                }
        }
    }

    // =================================================================
    // 2. カレンダー・月選択・日付選択ロジック
    // =================================================================

    private val _currentMonth = mutableStateOf(initialMonth)
    val currentMonth: State<YearMonth> = _currentMonth

    private val _selectedDate = mutableStateOf(initialDateTime)
    val selectedDate: State<LocalDateTime> = _selectedDate

    private val _showDatePicker = mutableStateOf(false)
    val showDatePicker: State<Boolean> = _showDatePicker

    private val _displayMode = mutableStateOf(CalendarDisplayMode.MONTH)
    val displayMode: State<CalendarDisplayMode> = _displayMode

    private val _showDisplayModeMenu = mutableStateOf(false)
    val showDisplayModeMenu: State<Boolean> = _showDisplayModeMenu

    private val _currentWeekStart = mutableStateOf(startOfWeek(initialDateTime.toLocalDate(), DayOfWeek.SUNDAY))
    val currentWeekStart: State<LocalDate> = _currentWeekStart

    private fun startOfWeek(date: LocalDate, weekStartDay: DayOfWeek): LocalDate {
        val diff = (date.dayOfWeek.value - weekStartDay.value + 7) % 7
        return date.minusDays(diff.toLong())
    }

    fun onDisplayModeMenuButtonClick() {
        _showDisplayModeMenu.value = true
    }

    fun dismissDisplayModeMenu() {
        _showDisplayModeMenu.value = false
    }

    fun buildCalendarMatrix(yearMonth: YearMonth): List<LocalDate?> {
        val weekStartDay = currentWeekStartDay()
        val dates = mutableListOf<LocalDate?>()
        val firstDay = yearMonth.atDay(1)

        val firstDayOfWeek = (firstDay.dayOfWeek.value - weekStartDay.value + 7) % 7
        repeat(firstDayOfWeek) { dates.add(null) }

        for (day in 1..yearMonth.lengthOfMonth()) {
            dates.add(yearMonth.atDay(day))
        }

        while (dates.size % 7 != 0) { dates.add(null) }

        return dates
    }

    fun buildWeekCalendarMatrix(baseDate: LocalDate): List<LocalDate?> {
        val start = startOfWeek(baseDate, currentWeekStartDay())
        return (0 until 7).map { start.plusDays(it.toLong()) }
    }

    fun buildYearMatrix(year: Int): List<YearMonth> {
        return (1..12).map { month -> YearMonth.of(year, month) }
    }

    fun onPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        ensureHolidaysLoaded(_currentMonth.value.year)
    }

    fun onNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        ensureHolidaysLoaded(_currentMonth.value.year)
    }

    fun onPreviousWeek() {
        _currentWeekStart.value = _currentWeekStart.value.minusWeeks(1)
        ensureHolidaysLoaded(_currentWeekStart.value.year)
    }

    fun onNextWeek() {
        _currentWeekStart.value = _currentWeekStart.value.plusWeeks(1)
        ensureHolidaysLoaded(_currentWeekStart.value.year)
    }

    fun onPreviousPeriod() {
        when (_displayMode.value) {
            CalendarDisplayMode.YEAR -> {
                _currentMonth.value = _currentMonth.value.minusYears(1)
                ensureHolidaysLoaded(_currentMonth.value.year)
            }
            CalendarDisplayMode.MONTH -> onPreviousMonth()
            CalendarDisplayMode.WEEK -> onPreviousWeek()
        }
    }

    fun onNextPeriod() {
        when (_displayMode.value) {
            CalendarDisplayMode.YEAR -> {
                _currentMonth.value = _currentMonth.value.plusYears(1)
                ensureHolidaysLoaded(_currentMonth.value.year)
            }
            CalendarDisplayMode.MONTH -> onNextMonth()
            CalendarDisplayMode.WEEK -> onNextWeek()
        }
    }

    fun onDateSelected(date: LocalDate) {
        _selectedDate.value = date.atTime(_selectedDate.value.toLocalTime())
    }

    fun onDateSelectedFromYearView(date: LocalDate) {
        onDateSelected(date)
        _currentMonth.value = YearMonth.from(date)
        _displayMode.value = CalendarDisplayMode.MONTH
        ensureHolidaysLoaded(date.year)
    }

    fun onMonthYearPickerClick() {
        _showDatePicker.value = true
    }

    fun dismissDatePicker() {
        _showDatePicker.value = false
    }

    fun updateYearMonth(year: Int, month: Int) {
        _currentMonth.value = YearMonth.of(year, month)
        _showDatePicker.value = false
        ensureHolidaysLoaded(year)
    }

    fun updateWeekStartFromYearMonth(year: Int, month: Int) {
        _currentWeekStart.value = startOfWeek(YearMonth.of(year, month).atDay(1), currentWeekStartDay())
        _showDatePicker.value = false
        ensureHolidaysLoaded(year)
    }

    fun updateWeekStart(weekStart: LocalDate) {
        _currentWeekStart.value = startOfWeek(weekStart, currentWeekStartDay())
        ensureHolidaysLoaded(weekStart.year)
    }

    init {
        AnalyticsLogger.logCalendarOpened()

        viewModelScope.launch {
            val savedMode = calendarPreferences.getDisplayMode()
            _displayMode.value = savedMode

            // ★ 修正：祝日のプリロードより先に、実際の設定値を待つ
            val initialSettings = settingsRepository.settingsFlow.first()
            val weekStartDay = initialSettings.weekStartDay

            when (savedMode) {
                CalendarDisplayMode.WEEK -> _currentWeekStart.value = startOfWeek(_selectedDate.value.toLocalDate(), weekStartDay)
                CalendarDisplayMode.MONTH -> _currentMonth.value = YearMonth.from(_selectedDate.value)
                CalendarDisplayMode.YEAR -> {}
            }

            // ★ 修正：実際の設定値が読み込まれた後で、正しい国コードで祝日を先読みする
            val startYear = initialMonth.year
            ensureHolidaysLoaded(startYear - 1)
            ensureHolidaysLoaded(startYear)
            ensureHolidaysLoaded(startYear + 1)

            // ★ 修正：先読みに実際使った国コードを基準値として明示的に渡す
            val appliedCode = initialSettings.holidayCountryCode ?: deviceCountryCode
            observeHolidayCountryChanges(appliedCode)
        }
    }

    // ★ 修正：引数で初期国コードを受け取る（nullによる「初回スキップ」判定をやめる）
    private fun observeHolidayCountryChanges(initialCode: String) {
        var lastAppliedCode: String = initialCode

        viewModelScope.launch {
            settings.map { it.holidayCountryCode }
                .distinctUntilChanged()
                .collect {
                    val newCode = effectiveCountryCode()
                    if (lastAppliedCode != newCode) {
                        val yearsToReload = loadedHolidayYears.toList()
                        _holidayMap.value = emptyMap()
                        loadedHolidayYears.clear()
                        yearsToReload.forEach { ensureHolidaysLoaded(it) }
                    }
                    lastAppliedCode = newCode
                }
        }
    }

    fun selectDisplayMode(mode: CalendarDisplayMode) {
        _displayMode.value = mode
        _showDisplayModeMenu.value = false
        viewModelScope.launch { calendarPreferences.saveDisplayMode(mode) }

        when (mode) {
            CalendarDisplayMode.WEEK -> _currentWeekStart.value = startOfWeek(_selectedDate.value.toLocalDate(), currentWeekStartDay())
            CalendarDisplayMode.MONTH -> _currentMonth.value = YearMonth.from(_selectedDate.value)
            CalendarDisplayMode.YEAR -> {}
        }
    }
    fun weekNumberOf(date: LocalDate): Int {
        return date.get(WeekFields.ISO.weekOfWeekBasedYear())
    }

    fun markCalendarOnboardingCompleted() {
        viewModelScope.launch {
            settingsRepository.setCalendarOnboardingCompleted(true)
        }
    }
    fun toggleTaskCompletion(item: TaskWithTags) {
        viewModelScope.launch {
            val task = item.task
            // 現在が "COMPLETED" なら "INCOMPLETE"（または未完了を表す任意の文字列）に戻す
            val newState = if (task.completeState == "COMPLETED") "INCOMPLETE" else "COMPLETED"

            // 状態を上書きした新しいTaskオブジェクトを作成してDBを更新
            val updatedTask = task.copy(completeState = newState)
            taskDao.updateTask(updatedTask) // ※TaskDaoに updateTask(Task) が定義されている前提です
        }
    }
}