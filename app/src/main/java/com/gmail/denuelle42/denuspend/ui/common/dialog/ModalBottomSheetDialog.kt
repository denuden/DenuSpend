package com.gmail.denuelle42.denuboilerplate.ui.common.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    rightSideContent : @Composable (Modifier) -> Unit = {} ,
    leftSideContent : @Composable (Modifier) -> Unit = {},
    content: @Composable () -> Unit
) {
    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
            dragHandle = {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 16.dp)) {
                    leftSideContent(Modifier.align(Alignment.CenterStart))
                    BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.Center))
                    rightSideContent(Modifier.align(Alignment.CenterEnd))
                }
            },
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun ModalBottomSheetDialogPreview() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        ModalBottomSheetDialog(showDialog = true, onDismissRequest = { }) {
            Text("Sample content")
        }
    }

}