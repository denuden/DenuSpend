package com.gmail.vondenuelle.denuspend.ui.add.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun RecentTransactions(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack : () -> Unit,
) {
    RecentTransactionsContent(
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun RecentTransactionsContent(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ){
        items(3){
            TransactionItem()
        }
    }
}

@Preview
@Composable
private fun RecentTransactionsPreview() {
    DenuSpendTheme {
        Surface {
            RecentTransactionsContent(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}