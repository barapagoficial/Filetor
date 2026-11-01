package com.filetor.app.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object Format {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun size(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val group = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(group.toDouble())
        return if (group == 0) "$bytes B"
        else String.format(Locale.getDefault(), if (value < 10) "%.2f %s" else if (value < 100) "%.1f %s" else "%.0f %s", value, units[group])
    }

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFmt = SimpleDateFormat("d MMM", Locale.getDefault())
    private val fullFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    fun date(millis: Long): String {
        if (millis <= 0) return ""
        val now = Calendar.getInstance()
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return when {
            now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR) ->
                "Hoy, " + timeFmt.format(Date(millis))
            now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) - 1 == c.get(Calendar.DAY_OF_YEAR) ->
                "Ayer, " + timeFmt.format(Date(millis))
            now.get(Calendar.YEAR) == c.get(Calendar.YEAR) -> dayFmt.format(Date(millis))
            else -> fullFmt.format(Date(millis))
        }
    }

    fun dateTime(millis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

    fun items(n: Int): String = when (n) {
        0 -> "Vacía"
        1 -> "1 elemento"
        else -> "$n elementos"
    }

    fun percent(part: Long, total: Long): Float = if (total <= 0) 0f else (part.toDouble() / total).toFloat().coerceIn(0f, 1f)
}
