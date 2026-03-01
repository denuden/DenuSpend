package com.gmail.vondenuelle.denuspend.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun SavingsMiniItem(
    modifier: Modifier = Modifier,
    icon : ImageVector,
    item : String,
    progress : Float,
    onClick : () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center
        ,modifier= modifier

    ) {
        CircularProgressIndicator(
            progress = {
                progress
            },
            modifier = Modifier
                .size(68.dp)
        )
        ElevatedCard(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            onClick = {}
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ){
                Icon(imageVector = icon, contentDescription = item)
            }
        }
    }

}

@Preview
@Composable
private fun SavingsMiniItemPreview() {
    DenuSpendTheme {
        Surface {
            SavingsMiniItem(
                icon = Icons.Filled.DirectionsCar,
                item = "kssy977",
                progress = 0.56f,
                onClick = {

                }
            )
        }
    }
}