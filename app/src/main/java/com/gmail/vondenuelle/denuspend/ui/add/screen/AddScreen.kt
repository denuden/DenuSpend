package com.gmail.vondenuelle.denuspend.ui.add.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.components.section.AddButtonsSection
import com.gmail.vondenuelle.denuspend.ui.add.components.section.TodayIncomeAndExpensesSection
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun AddScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
) {

    AddScreenContent()
}

@Composable
fun AddScreenContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        TodayIncomeAndExpensesSection(
            modifier = Modifier.fillMaxWidth(),
            progress = 0.65f,
            expense = "₱1,456.00",
            totalBudget = "₱3,000.00"
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddButtonsSection(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(24.dp))
        Text("Last Added", fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(5) {
                TransactionItem()
            }
        }
    }

}

@Preview
@Composable
private fun AddScreenPreview() {
    DenuSpendTheme {
        Surface {
            AddScreenContent(modifier = Modifier.fillMaxSize())
        }
    }
}