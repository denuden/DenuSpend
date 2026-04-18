package com.gmail.vondenuelle.denuspend.ui.add.components

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
import androidx.compose.ui.graphics.Color

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
