package com.gmail.vondenuelle.denuspend.ui.common.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.valentinilk.shimmer.shimmer

@Composable
fun SkeletonTransactionList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        Row(
        ) {
            Box(
                modifier = Modifier
                    .shimmer()
                    .width(60.dp)
                    .height(60.dp)
                    .background(color = Color.LightGray, shape = CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .shimmer()
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(color = Color.LightGray, shape = MaterialTheme.shapes.small)
                )
                Box(
                    modifier = Modifier
                        .shimmer()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .padding(end = 30.dp)
                        .height(12.dp)
                        .background(color = Color.LightGray, shape = MaterialTheme.shapes.small)
                )
            }
            Box(
                modifier = Modifier
                    .shimmer()
                    .padding(horizontal = 8.dp)
                    .weight(1f)

                    .height(18.dp)
                    .background(color = Color.LightGray, shape = MaterialTheme.shapes.small)
            )
        }
        Box(
            modifier = Modifier
                .shimmer()
                .align(Alignment.CenterHorizontally)
                .padding( top = 6.dp)
                .fillMaxWidth(0.5f)
                .height(18.dp)
                .background(color = Color.LightGray, shape = MaterialTheme.shapes.small)
        )
    }
}

@Preview
@Composable
private fun SkeletonAnimeListPreview() {
    DenuSpendTheme() {
        Surface(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
        ) {
            SkeletonTransactionList()
        }
    }
}