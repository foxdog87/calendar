package com.foxdog.strucalendar.screens.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.foxdog.strucalendar.viewmodel.CalendarViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToTaskCreate: (LocalDate) -> Unit,
    onNavigateToTaskList: () -> Unit,
    onNavigateToDateDetail: (LocalDate) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTaskDetail: (Long) -> Unit
) {
    val currentMonth by viewModel.currentMonth
    val selectedDateTime by viewModel.selectedDate
    val tasksByDate by viewModel.tasksByDate.collectAsState()
    val holidayMap by viewModel.holidayMap.collectAsState()
    val displayMode by viewModel.displayMode
    val showDisplayModeMenu by viewModel.showDisplayModeMenu
    val currentWeekStart by viewModel.currentWeekStart
    val settings by viewModel.settings.collectAsState()

    val selectedDate = selectedDateTime.toLocalDate()

    CalendarScreenContent(
        currentMonth = currentMonth,
        currentWeekStart = currentWeekStart,
        selectedDate = selectedDate,
        tasksByDate = tasksByDate,
        holidayMap = holidayMap,
        displayMode = displayMode,
        showDisplayModeMenu = showDisplayModeMenu,
        weekStartDay = settings.weekStartDay,
        showTagColorOnCalendar = settings.showTagColorOnCalendar,
        showWeekNumber = settings.showWeekNumber,
        weekNumberOf = { date -> viewModel.weekNumberOf(date) },
        buildCalendarMatrix = { month -> viewModel.buildCalendarMatrix(month) },
        buildWeekCalendarMatrix = { base -> viewModel.buildWeekCalendarMatrix(base) },
        buildYearMatrix = { year -> viewModel.buildYearMatrix(year) },
        onDateSelected = { viewModel.onDateSelected(it) },
        onDateSelectedFromYearView = { viewModel.onDateSelectedFromYearView(it) },
        onPreviousPeriod = { viewModel.onPreviousPeriod() },
        onNextPeriod = { viewModel.onNextPeriod() },
        onUpdateYearMonth = { y, m -> viewModel.updateYearMonth(y, m) },
        onUpdateWeekStartFromYearMonth = { y, m -> viewModel.updateWeekStartFromYearMonth(y, m) },
        onUpdateWeekStart = { date -> viewModel.updateWeekStart(date) },
        onDisplayModeMenuButtonClick = { viewModel.onDisplayModeMenuButtonClick() },
        onDismissDisplayModeMenu = { viewModel.dismissDisplayModeMenu() },
        onSelectDisplayMode = { viewModel.selectDisplayMode(it) },
        onNavigateToTaskCreate = onNavigateToTaskCreate,
        onNavigateToTaskList = onNavigateToTaskList,
        onNavigateToDateDetail = onNavigateToDateDetail,
        onNavigateToSettings = onNavigateToSettings,
        showOnboarding = !settings.calendarOnboardingCompleted,
        onOnboardingFinished = { viewModel.markCalendarOnboardingCompleted() },
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onToggleTaskCompletion = { taskItem ->
            viewModel.toggleTaskCompletion(taskItem)
        },
        showAllTutorialsCompletedDialog = viewModel.showAllTutorialsCompletedDialog,
        onDismissAllTutorialsCompletedDialog = { viewModel.dismissAllTutorialsCompletedDialog() },
        allTags = viewModel.allTags.collectAsState().value,
        selectedFilterTagIds = viewModel.selectedFilterTagIds.collectAsState().value,
        onToggleFilterTag = { tagId -> viewModel.toggleFilterTag(tagId) },
        onResetTagFilter = { viewModel.resetTagFilter() },
        filterIsAndSearch = viewModel.filterIsAndSearch.collectAsState().value,
        onSetFilterIsAndSearch = { isAnd -> viewModel.setFilterIsAndSearch(isAnd) },
        onDeleteTag = { tag -> viewModel.deleteTag(tag) },
        onCreateTag = { tag, customFieldNames -> viewModel.createTag(tag, customFieldNames) },
        onUpdateTag = { tag, customFieldNames -> viewModel.updateTag(tag, customFieldNames) },
        onLoadCustomFieldsForTag = { tagId -> viewModel.getCustomFieldNamesForTag(tagId) },
        onUpdateTagOrder = { tags -> viewModel.updateTagOrder(tags) },
        confirmDiscardChanges = settings.confirmDiscardChanges,
        weekDayPreviewIsTimetable = settings.weekDayPreviewIsTimetable,
        onToggleWeekDayPreviewMode = { viewModel.toggleWeekDayPreviewMode() },
    )
}