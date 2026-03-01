package com.gmail.vondenuelle.denuspend.ui.home.components.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.room.Transaction
import com.gmail.vondenuelle.denuspend.ui.home.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun RecentTab(
    modifier: Modifier = Modifier
) {

    val state = rememberLazyListState()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.height(6.dp)
                    .width(60.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(color = Color.Gray, shape = MaterialTheme.shapes.extraLarge)
            )

            TextButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.height(34.dp)
                    .align(Alignment.End)
            ) { Text("View all", color = MaterialTheme.colorScheme.onSurface) }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                state = state
            ) {
                items(5){
                    TransactionItem()
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecentTabSectionPreview() {
    DenuSpendTheme {
        Surface {
            RecentTab(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}