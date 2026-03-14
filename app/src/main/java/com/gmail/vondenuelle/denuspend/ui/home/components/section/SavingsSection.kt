package com.gmail.vondenuelle.denuspend.ui.home.components.section

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.ui.home.components.SavingsMiniItem
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavingsSection(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp)
        ),
        colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Savings", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    //TODO
                }, modifier = Modifier.height(32.dp)) {
                    Text("See all", color = MaterialTheme.colorScheme.primary)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("₱11,000.52", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLargeEmphasized)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "out of",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("₱20,000.00", fontWeight = FontWeight.Normal,style = MaterialTheme.typography.labelLargeEmphasized)

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.horizontalScroll(scrollState)
                    ) {
                        repeat(4) {
                            //TODO
                            SavingsMiniItem(
                                icon = Icons.Filled.DirectionsCar,
                                item = "gwgw",
                                progress = 0.56f,
                                modifier = Modifier
                                    .size(68.dp)
                            ) {}
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                ElevatedCard(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    onClick = {}
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null
                        )
                    }
                }

            }

        }
    }
}

@Preview
@Composable
private fun SavingsSectionPreview() {
    DenuSpendTheme {
        Surface {
            SavingsSection(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}