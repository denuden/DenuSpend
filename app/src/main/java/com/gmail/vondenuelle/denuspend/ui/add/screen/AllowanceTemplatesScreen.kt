package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateItem
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import com.gmail.vondenuelle.denuspend.ui.add.components.AllowanceTemplateCard
import com.gmail.vondenuelle.denuspend.ui.add.components.TemplateEditor

@Composable
fun AllowanceTemplatesScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // The template waiting for delete confirmation; null means the dialog is closed.
    var templateToDelete by remember { mutableStateOf<AllowanceTemplateModel?>(null) }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> onPopBackStack()
            is OneTimeEvents.OnNavigate -> onNavigate(event.route, event.behavior.toNavOptions())
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            is OneTimeEvents.ShowSnackbar -> {
                scope.launch {
                    SnackBarController.sendEvent(event.snackbarEvent)
                }
            }
            OneTimeEvents.OnCloseDialog -> {}
            OneTimeEvents.OnLoginOrRegisterCompleted -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onEvent(AddScreenEvents.OnGetAllowanceTemplates)
            Lifecycle.Event.ON_STOP -> viewModel.stopTemplatesListener()
            else -> Unit
        }
    }

    LoadingDialog(
        showDialog = state.isLoading,
        text = stringResource(R.string.lbl_loading),
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    CustomAlertDialog(
        showDialog = templateToDelete != null,
        onDismissRequest = { templateToDelete = null },
        onConfirmation = {
            templateToDelete?.let { viewModel.onEvent(AddScreenEvents.OnDeleteAllowanceTemplate(it)) }
            templateToDelete = null
        },
        dialogTitle = stringResource(R.string.add_dialog_delete_template_title),
        dialogText = stringResource(R.string.add_dialog_delete_template_text, templateToDelete?.name.orEmpty()),
        icon = Icons.Filled.Delete,
        confirmText = stringResource(R.string.add_btn_delete),
        dismissText = stringResource(R.string.lbl_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )

    // The draft lives in the ViewModel, so the sheet and what was typed survive rotation
    ModalBottomSheetDialog(
        showDialog = state.templateDraft != null,
        onDismissRequest = { viewModel.onEvent(AddScreenEvents.OnCloseTemplateEditor) },
    ) {
        state.templateDraft?.let { draft ->
            TemplateEditor(
                draft = draft,
                canSave = state.hasLoadedTemplates && !state.isLoading,
                onChange = { viewModel.onEvent(AddScreenEvents.OnTemplateDraftChanged(it)) },
                onSave = { viewModel.onEvent(AddScreenEvents.OnSaveTemplateDraft) },
            )
        }
    }

    AllowanceTemplatesContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onEvent = viewModel::onEvent,
        onDeleteClick = { templateToDelete = it }
    )
}

@Composable
fun AllowanceTemplatesContent(
    modifier: Modifier = Modifier,
    state: AddScreenState,
    onEvent: (AddScreenEvents) -> Unit,
    onDeleteClick: (AllowanceTemplateModel) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.add_templates_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { onEvent(AddScreenEvents.OnOpenTemplateEditor(null)) },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_btn_new_template))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(state.allowanceTemplates) { template ->
            AllowanceTemplateCard(
                template = template,
                onClick = { onEvent(AddScreenEvents.OnOpenTemplateEditor(template)) },
                onDeleteClick = { onDeleteClick(template) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // First load can take a moment (new doc, no cache yet), so show a spinner instead of a blank list
        if (!state.hasLoadedTemplates) {
            item {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                    CircularProgressIndicator()
                }
            }
        }

        if (state.hasLoadedTemplates && state.allowanceTemplates.isEmpty()) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                ) {
                    Text(stringResource(R.string.add_templates_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.add_templates_empty_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Preview
@Composable
private fun AllowanceTemplatesPreview() {
    DenuSpendTheme {
        Surface {
            AllowanceTemplatesContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(
                    hasLoadedTemplates = true,
                    allowanceTemplates = listOf(
                        AllowanceTemplateModel(
                            id = "1",
                            name = "Weekday",
                            items = listOf(
                                AllowanceTemplateItem("a", "Food", 30000),
                                AllowanceTemplateItem("b", "Transportation", 15000),
                            )
                        )
                    )
                ),
                onEvent = {}
            )
        }
    }
}
