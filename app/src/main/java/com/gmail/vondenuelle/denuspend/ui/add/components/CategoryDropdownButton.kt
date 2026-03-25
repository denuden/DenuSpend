package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


object CategoryDropdownButtonModel {
    val list = listOf(
        "",
        "Food",// to Color(0xFFFF8A80) Light Red
        "Entertainment",// to Color(0xFFE1BEE7) Lavender
        "Household",// to Color(0xFF90CAF9) Light Blue
        "Transportation", // to Color(0xFFFFCC80) Light Orange
        "Work/Education", // to Color(0xFF9FA8DA) Soft Indigo
        "Healthcare",// to Color(0xFF80CBC4) Aqua Teal
        "Personal", // to Color(0xFFF48FB1) Soft Pink
        "Family",// to Color(0xFFA5D6A7) Light Green
        "Others", // to Color(0xFFBDBDBD) Light Gray
    )

    val icons = listOf(
        Icons.Filled.Restaurant,        // Food
        Icons.Filled.Movie,             // Entertainment
        Icons.Filled.Home,              // Household
        Icons.Filled.DirectionsCar,     // Transportation
        Icons.Filled.School,            // Work/Education
        Icons.Filled.LocalHospital,     // Healthcare
        Icons.Filled.Person,            // Personal
        Icons.Filled.Group,             // Family
        Icons.AutoMirrored.Filled.More          // Others
    )

    val colors = listOf(
        Color(0xFFFF8A80),
        Color(0xFFE1BEE7),
        Color(0xFF90CAF9),
        Color(0xFFFFCC80),
        Color(0xFF9FA8DA),
        Color(0xFF80CBC4),
        Color(0xFFF48FB1),
        Color(0xFFA5D6A7),
        Color(0xFFBDBDBD),
    )
    val selectedColors = listOf(
        Color(0xFFD32F2F), // darker red
        Color(0xFF8E24AA), // deeper lavender
        Color(0xFF1976D2), // strong blue
        Color(0xFFF57C00), // deeper orange
        Color(0xFF3949AB), // deeper indigo
        Color(0xFF00897B), // deeper teal
        Color(0xFFC2185B), // deeper pink
        Color(0xFF388E3C), // deeper green
        Color(0xFF616161), // darker gray
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdownButton(modifier: Modifier = Modifier, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val list = remember {
        CategoryDropdownButtonModel.list
    }
    val textFieldState = rememberTextFieldState(list[0])

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.TopStart)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                readOnly = true,
                state = textFieldState,
                lineLimits = TextFieldLineLimits.SingleLine,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },

                placeholder = { Text("Select Category") },
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors().copy(
                    unfocusedIndicatorColor = outlinedColor,
                    disabledIndicatorColor = outlinedColor,
                    errorIndicatorColor = outlinedColor
                ),
                textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                scrollState = scrollState
            ) {
                list.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            expanded = false
                            textFieldState.setTextAndPlaceCursorAtEnd(option)
                            onSelected(option)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }

        }

        LaunchedEffect(expanded) {
            if (expanded) {
                // Scroll to show the bottom menu items.
                scrollState.scrollTo(scrollState.maxValue)
            }
        }
    }
}