package com.gmail.denuelle42.denuspend.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


// Function to get the current timestamp based on the phone's time zone
fun getCurrentTimestamp(): Long {
    return System.currentTimeMillis()
}

// Function to format the timestamp as "yyyy-MM-dd HH:mm:ss"
fun formatTimestampAsDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault() // Use the phone's time zone
    return dateFormat.format(Date(timestamp))
}

// Function to format the timestamp as "MMMM dd, yyyy"
fun formatTimestampAsLongDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(Date(timestamp))
}

// Function to format the timestamp as "MMMM dd, yyyy hh:mm a"
fun formatTimestampAsLongDateTime(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(Date(timestamp))
}

// Function to format the timesptamp as "MMMM dd"
fun formatTimestampAsMonthDayNamed(timestamp: Long) : String {
    val dateFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(Date(timestamp))
}

// ======================= Counterpart Functions of Timestamp to ISO 8601 Date Formats ==================
fun formatIsoDateAsDateTime(isoDate: String?, customMessage : String = "Invalid Date"): String {
    if (isoDate == null) return customMessage
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    val date = isoDateFormat.parse(isoDate)
    return if (date != null) dateFormat.format(date) else customMessage
}

fun formatIsoDateAsLongDate(isoDate: String?, customMessage : String = "Invalid Date"): String {
    if (isoDate == null) return customMessage
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    val date = isoDateFormat.parse(isoDate)
    return if (date != null) dateFormat.format(date) else customMessage
}

fun formatIsoDateAsLongDateTime(isoDate: String?, customMessage : String = "Invalid Date"): String {
    if (isoDate == null) return customMessage
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    val date = isoDateFormat.parse(isoDate)
    return if (date != null) dateFormat.format(date) else customMessage
}

fun formatIsoDateAsMonthDayNamed(isoDate: String?, customMessage : String = "Invalid Date"): String {
    if (isoDate == null) return customMessage
    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val dateFormat = SimpleDateFormat("MMMM dd", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    val date = isoDateFormat.parse(isoDate)
    return if (date != null) dateFormat.format(date) else customMessage
}

