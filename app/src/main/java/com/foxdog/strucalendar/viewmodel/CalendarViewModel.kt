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
import kotlinx.coroutines.flow.combine
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class CalendarDisplayMode {
    YEAR, MONTH, WEEK
}

class CalendarViewModel(
    private val taskDao: TaskDao,
    private val calendarPreferences: CalendarPreferences,
    private val settingsRepository: SettingsRepository,
    private val holidayRepository: HolidayRepository,
    private val tagRepository: com.foxdog.strucalendar.data.repository.TagRepository,
    private val deviceCountryCode: String, // 端末ロケールから判定した国コード。設定で上書きされていない場合のフォールバック
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

    // 設定で明示的に選ばれた国があればそちらを優先し、なければ端末ロケール判定に従う
    private fun effectiveCountryCode(): String = settings.value.holidayCountryCode ?: deviceCountryCode

    // =================================================================
    // 1. データベース（Room）連携ロジック
    // =================================================================

    val allTasksByDate: StateFlow<Map<LocalDate, List<TaskWithTags>>> = taskDao.getAllTasksWithTags()
        .map { totalList ->
            val result = mutableMapOf<LocalDate, MutableList<TaskWithTags>>()

            totalList.forEach { item ->
                val startDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(item.task.startTime),
                    ZoneId.systemDefault()
                ).toLocalDate()

                val endDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(item.task.endTime),
                    ZoneId.systemDefault()
                ).toLocalDate()

                // タスクの開始日〜終了日の全ての日付にそのタスクを表示する。
                // 極端に長い期間（データ不整合等）で無限ループ的な負荷にならないよう上限を設ける。
                val spanDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
                val cappedEndDate = if (spanDays > 366) startDate.plusDays(366) else endDate

                var currentDate = startDate
                while (!currentDate.isAfter(cappedEndDate)) {
                    result.getOrPut(currentDate) { mutableListOf() }.add(item)
                    currentDate = currentDate.plusDays(1)
                }
            }

            result
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // =================================================================
    // 1.2. タグによる絞り込み（カレンダー画面）
    // =================================================================

    val allTags: StateFlow<List<com.foxdog.strucalendar.data.entity.Tag>> = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedFilterTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFilterTagIds: StateFlow<Set<Long>> = _selectedFilterTagIds

    private val _filterIsAndSearch = MutableStateFlow(false)
    val filterIsAndSearch: StateFlow<Boolean> = _filterIsAndSearch

    fun setFilterIsAndSearch(isAndSearch: Boolean) {
        _filterIsAndSearch.value = isAndSearch
    }

    fun toggleFilterTag(tagId: Long) {
        _selectedFilterTagIds.value =
            if (tagId in _selectedFilterTagIds.value) {
                _selectedFilterTagIds.value - tagId
            } else {
                _selectedFilterTagIds.value + tagId
            }
    }

    fun resetTagFilter() {
        _selectedFilterTagIds.value = emptySet()
    }

    fun deleteTag(tag: com.foxdog.strucalendar.data.entity.Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            com.foxdog.strucalendar.data.telemetry.AnalyticsLogger.logTagDeleted()
        }
    }

    fun createTag(
        tag: com.foxdog.strucalendar.data.entity.Tag,
        customFieldNames: List<String>
    ) {
        viewModelScope.launch {
            tagRepository.createTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
            com.foxdog.strucalendar.data.telemetry.AnalyticsLogger.logTagCreated()
        }
    }

    fun updateTag(
        tag: com.foxdog.strucalendar.data.entity.Tag,
        customFieldNames: List<String>
    ) {
        viewModelScope.launch {
            tagRepository.updateTagWithCustomFields(
                tag = tag,
                customFieldNames = customFieldNames
            )
            com.foxdog.strucalendar.data.telemetry.AnalyticsLogger.logTagUpdated()
        }
    }

    suspend fun getCustomFieldNamesForTag(tagId: Long): List<String> {
        return tagRepository.getCustomFieldNames(tagId)
    }

    fun updateTagOrder(tags: List<com.foxdog.strucalendar.data.entity.Tag>) {
        viewModelScope.launch {
            tagRepository.updateTagOrder(tags)
            com.foxdog.strucalendar.data.telemetry.AnalyticsLogger.logTagOrderChanged()
        }
    }

    // 週表示の「選択日の予定」表示モード（リスト⇄時刻表）を切り替える
    fun toggleWeekDayPreviewMode() {
        viewModelScope.launch {
            val current = settingsRepository.settingsFlow.first().weekDayPreviewIsTimetable
            settingsRepository.setWeekDayPreviewIsTimetable(!current)
        }
    }

    // タグ絞り込みが選択されていれば、AND/OR設定に応じて絞り込んだマップを返す
    val tasksByDate: StateFlow<Map<LocalDate, List<TaskWithTags>>> =
        combine(allTasksByDate, _selectedFilterTagIds, _filterIsAndSearch) { byDate, selectedIds, isAndSearch ->
            if (selectedIds.isEmpty()) {
                byDate
            } else {
                byDate
                    .mapValues { (_, items) ->
                        items.filter { item ->
                            if (isAndSearch) {
                                item.tags.map { it.tagId }.toSet().containsAll(selectedIds)
                            } else {
                                item.tags.any { it.tagId in selectedIds }
                            }
                        }
                    }
                    .filterValues { it.isNotEmpty() }
            }
        }.stateIn(
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

    // 設定画面で祝日の対象国が変更されたら、キャッシュを破棄して読み込み済みの年を新しい国で取り直す
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

            // 祝日のプリロードより先に、実際の設定値を待つ
            val initialSettings = settingsRepository.settingsFlow.first()
            val weekStartDay = initialSettings.weekStartDay

            when (savedMode) {
                CalendarDisplayMode.WEEK -> _currentWeekStart.value = startOfWeek(_selectedDate.value.toLocalDate(), weekStartDay)
                CalendarDisplayMode.MONTH -> _currentMonth.value = YearMonth.from(_selectedDate.value)
                CalendarDisplayMode.YEAR -> {}
            }

            // 実際の設定値が読み込まれた後で、正しい国コードで祝日を先読みする
            val startYear = initialMonth.year
            ensureHolidaysLoaded(startYear - 1)
            ensureHolidaysLoaded(startYear)
            ensureHolidaysLoaded(startYear + 1)

            // 先読みに実際使った国コードを基準値として明示的に渡す
            val appliedCode = initialSettings.holidayCountryCode ?: deviceCountryCode
            observeHolidayCountryChanges(appliedCode)

            taskDao.autoCompleteExpiredTasks(System.currentTimeMillis() / 1000)
        }
    }

    // 引数で初期国コードを受け取る（nullによる「初回スキップ」判定をやめる）
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

    var showAllTutorialsCompletedDialog by mutableStateOf(false)
        private set

    fun dismissAllTutorialsCompletedDialog() {
        showAllTutorialsCompletedDialog = false
    }

    fun markCalendarOnboardingCompleted() {
        viewModelScope.launch {
            settingsRepository.setCalendarOnboardingCompleted(true)
            if (settingsRepository.areAllOnboardingsCompleted()) {
                showAllTutorialsCompletedDialog = true
            }
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