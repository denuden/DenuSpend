package com.gmail.vondenuelle.denuspend.ui.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun BackButton(modifier: Modifier = Modifier, onClick : () -> Unit) {
    IconButton(
        onClick = { onClick() },
        colors = IconButtonDefaults.iconButtonColors().copy(containerColor = Color.White),
        modifier = modifier
    ) { Icon(Icons.Filled.ArrowBackIosNew, null) }
}

@Preview
@Composable
private fun BackButtonPreview() {
    DenuSpendTheme() {
        Surface() {
            BackButton(){}
        }
    }
}