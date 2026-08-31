package com.foxdog.strucalendar.screens.templatecreate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.foxdog.strucalendar.viewmodel.TemplateCreateViewModel

@Composable
fun TemplateCreateScreen(
    viewModel: TemplateCreateViewModel,
    onNavigateBack: () -> Unit
) {
    val availableTags by viewModel.allTags.collectAsState()
    val settings by viewModel.settings.collectAsState()

    TemplateCreateContent(
        templateState = viewModel.inputState,
        availableTags = availableTags,
        isTitleError = viewModel.isTitleError,
        isEditMode = viewModel.isEditMode,
        isSaving = viewModel.saveState == TemplateCreateViewModel.SaveState.SAVING,
        onSaveTemplate = { viewModel.saveTemplate(onSuccess = onNavigateBack) },
        onNavigateBack = onNavigateBack,
        onUpdateInput = { update -> viewModel.updateInput(update) },
        onToggleTagSelection = { tag -> viewModel.toggleTagSelection(tag) },
        onDeleteTag = { tag -> viewModel.deleteTag(tag) },
        onCreateTag = { tag, customFieldNames ->
            viewModel.createTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
        },
        onUpdateTag = { tag, customFieldNames ->
            viewModel.updateTag(
                tag = tag,
                customFieldNames = customFieldNames
            )
        },
        onLoadCustomFieldsForTag = { tagId -> viewModel.getCustomFieldNamesForTag(tagId) },
        onUpdateTagOrder = { tags -> viewModel.updateTagOrder(tags) },
        hasUnsavedChanges = viewModel.hasUnsavedChanges,
        confirmDiscardChanges = settings.confirmDiscardChanges,
    )
}