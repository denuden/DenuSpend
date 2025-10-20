package com.gmail.denuelle42.denuspend.utils

import com.gmail.denuelle42.denuspend.data.remote.error.ErrorData


fun handleInputError(errorData: ErrorData): String {
    val result = StringBuilder()

    errorData.q?.let { result.append("Query: ${it.joinToString(", ")}\n") }
    errorData.type?.let { result.append("Type: ${it.joinToString(", ")}\n") }
    errorData.score?.let { result.append("Score: ${it.joinToString(", ")}\n") }
    errorData.max_score?.let { result.append("Max Score: ${it.joinToString(", ")}\n") }
    errorData.min_score?.let { result.append("Min Score: ${it.joinToString(", ")}\n") }
    errorData.status?.let { result.append("Status: ${it.joinToString(", ")}\n") }
    errorData.rating?.let { result.append("Rating: ${it.joinToString(", ")}\n") }
    errorData.sfw?.let { result.append("SFW: ${it.joinToString(", ")}\n") }
    errorData.genres?.let { result.append("Genres: ${it.joinToString(", ")}\n") }
    errorData.order_by?.let { result.append("Order By: ${it.joinToString(", ")}\n") }
    errorData.sort?.let { result.append("Sort: ${it.joinToString(", ")}\n") }
    errorData.start_date?.let { result.append("Start Date: ${it.joinToString(", ")}\n") }
    errorData.end_date?.let { result.append("End Date: ${it.joinToString(", ")}\n") }

    return result.toString().trim()
}
