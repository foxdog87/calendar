package com.foxdog.strucalendar.screens.templatecreate

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.foxdog.strucalendar.viewmodel.TemplateCreateViewModel

@Composable
fun TemplateCreateScreen(
    viewModel: TemplateCreateViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val availableTags by viewModel.allTags.collectAsState()

    TemplateCreateContent(
        templateState = viewModel.inputState,
        availableTags = availableTags,
        isTitleError = viewModel.isTitleError,
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
        onUpdateTagOrder = { tags -> viewModel.updateTagOrder(tags) },
    )
}