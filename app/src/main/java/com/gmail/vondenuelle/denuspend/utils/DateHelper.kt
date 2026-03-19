package com.gmail.vondenuelle.denuspend.utils

import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


enum class DateRangeOption {
    PAST_ONLY,
    FUTURE_ONLY,
    ALL_DATES,
    ONLY_CURRENT_YEAR
}


/**
 * LEGACY
 * @param context context
 * @param timeFormat format of time to return
 * @param onTimeSelected callback with selected time
 */
fun showTimePickerDialog(
    context: Context,
    timeFormat: String = "hh:mm a",
    onTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)

            val formattedTime =
                SimpleDateFormat(timeFormat, Locale.US).format(calendar.time)

            onTimeSelected(formattedTime)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false // 24-hour format (set false for AM/PM)
    )

    timePickerDialog.show()
}

/**
 * converter of 12 hour to 24 hour
 */
fun convert12HourTo24HourOrNull(
    time12Hour: String,
    inputFormat: String = "hh:mm a",
    outputFormat: String = "HH:mm"
): String? {
    return try {
        val parser = SimpleDateFormat(inputFormat, Locale.US)
        val formatter = SimpleDateFormat(outputFormat, Locale.US)
        formatter.format(parser.parse(time12Hour)!!)
    } catch (e: Exception) {
        null
    }
}

/**
 * converter of 24 hour to 12 hour
 */
fun convert24HourTo12HourOrNull(
    time24Hour: String,
    inputFormat: String = "HH:mm:ss",
    outputFormat: String = "hh:mm a"
): String? {
    return try {
        val parser = SimpleDateFormat(inputFormat, Locale.US)
        val formatter = SimpleDateFormat(outputFormat, Locale.US)
        formatter.format(parser.parse(time24Hour)!!)
    } catch (e: Exception) {
        null
    }
}


// Function to format the timestamp as "MMM dd, yyyy"
fun formatTimestampAsLongDateShortenedMonth(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(Date(timestamp))
}

fun getTimeNow() : String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss") // Use "hh" for 12-hour format
    val formattedTime = current.format(formatter)
    return formattedTime
}
fun getDateNow(format : String = "dd MMM yyyy") : String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern(format) // Use "hh" for 12-hour format
    val formattedTime = current.format(formatter)
    return formattedTime
}
// ======================= Counterpart Functions of Timestamp to ISO 8601 Date Formats ==================
fun formatIsoDate(
    isoDate: String?,
    customMessage: String = "Invalid Date",
    format: String = "MMM dd, yyyy"
): String {
    if (isoDate == null) return customMessage
    return try {
        // Parse ISO 8601 Date in UTC
        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoDateFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Convert to Philippine Time (UTC+8)
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila") // Set to PHT

        val date = isoDateFormat.parse(isoDate)
        if (date != null) dateFormat.format(date) else customMessage
    } catch (e: Exception) {
        customMessage // Return custom message if parsing fails
    }
}


fun formatFirebaseDate(
    date: Date?,
    customMessage: String = "Invalid Date",
    format: String = "MMM dd, yyyy"
): String {
    if (date == null) return customMessage
    return try {
        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        dateFormat.format(date)
    } catch (e: Exception) {
        customMessage
    }
}

fun getCurrentIsoDate(): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date())
}