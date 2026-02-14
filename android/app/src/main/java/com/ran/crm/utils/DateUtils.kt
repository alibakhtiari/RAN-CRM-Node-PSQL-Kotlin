package com.ran.crm.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Thread-safe date formatting utilities.
 * Uses ThreadLocal to avoid SimpleDateFormat thread-safety issues.
 */
object DateUtils {

    private val isoFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    /** Formats a Date to ISO 8601 UTC string. */
    fun formatIso(date: Date = Date()): String = isoFormatter.get()!!.format(date)

    /** Parses an ISO 8601 UTC string to Date, or null if invalid. */
    fun parseIso(dateString: String): Date? {
        return try {
            isoFormatter.get()!!.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /** Formats a millisecond timestamp to ISO 8601 UTC string. */
    fun formatIso(millis: Long): String = formatIso(Date(millis))
}
