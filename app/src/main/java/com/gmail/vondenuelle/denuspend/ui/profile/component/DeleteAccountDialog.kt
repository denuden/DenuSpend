package com.gmail.vondenuelle.denuspend.ui.profile.component

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.components.AppTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import kotlin.math.roundToInt

/**
 * Step 2 of deleting the account (step 1 is the info dialog in ProfileScreen).
 * The password re-logs the user in, which Firebase requires before deleting, and the slider is the final confirm.
 */
@Composable
fun DeleteAccountSheet(
    modifier: Modifier = Modifier,
    email: String,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
    ) {
        Text(
            stringResource(R.string.profile_delete_sheet_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.profile_delete_sheet_text, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        AppTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.login_lbl_password),
            placeholder = stringResource(R.string.login_hint_password),
            leadingIcon = Icons.Filled.Password,
            isPassword = true,
            imeAction = ImeAction.Done,
        )

        Spacer(modifier = Modifier.height(24.dp))
        SlideToConfirm(
            text = stringResource(R.string.profile_slide_to_delete),
            enabled = password.isNotBlank(),
            isLoading = isLoading,
            onConfirm = onConfirm
        )
    }
}

/**
 * Drag the thumb to the end to confirm. Letting go past 80% confirms, otherwise it springs back.
 * A filled bar trails the thumb, and while loading the thumb stays at the end with a spinner.
 * The drag offset is plain state updated synchronously, so nothing can cancel the release check.
 */
@Composable
private fun SlideToConfirm(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onConfirm: () -> Unit,
) {
    val thumbSize = 56.dp
    val thumbSizePx = with(LocalDensity.current) { thumbSize.toPx() }
    val haptic = LocalHapticFeedback.current
    var offset by remember { mutableFloatStateOf(0f) }
    var maxOffset by remember { mutableFloatStateOf(0f) }
    val progress = if (maxOffset > 0f) offset / maxOffset else 0f
    val canDrag = enabled && !isLoading

    // Springs back once loading ends without leaving (e.g. wrong password)
    LaunchedEffect(isLoading) {
        if (!isLoading && offset > 0f) {
            animate(offset, 0f) { value, _ -> offset = value }
        }
    }

    val fillColor = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbSize)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .onSizeChanged { maxOffset = (it.width - thumbSizePx).coerceAtLeast(0f) },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(1f - progress)
        )

        // Trailing fill, from the start of the track to the thumb's right edge
        Box(
            modifier = Modifier
                .width(with(LocalDensity.current) { (offset + thumbSizePx).toDp() })
                .fillMaxHeight()
                .clip(CircleShape)
                .background(fillColor)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(offset.roundToInt(), 0) }
                .size(thumbSize)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = canDrag,
                    state = rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(0f, maxOffset)
                    },
                    onDragStopped = {
                        if (offset >= maxOffset * 0.8f) {
                            offset = maxOffset
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm()
                        } else {
                            animate(offset, 0f) { value, _ -> offset = value }
                        }
                    }
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = fillColor
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = fillColor
                )
            }
        }
    }
}

@Preview
@Composable
private fun DeleteAccountSheetPreview() {
    DenuSpendTheme {
        Surface {
            DeleteAccountSheet(
                email = "user@mail.com",
                password = "secret",
                onPasswordChange = {},
                isLoading = false,
                onConfirm = {},
            )
        }
    }
}
