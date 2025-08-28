package com.mmushtaq.smartreceiptscanner.core.util

import java.util.Locale

fun Long.formatMinor(currency: String?): String {
    val code = currency?.uppercase(Locale.ROOT) ?: "PKR"
    val scale = when (code) { "JPY" -> 0; else -> 2 }
    return if (scale == 0) this.toString() + " " + code
    else String.format(Locale.getDefault(), "%,.2f %s", this / 100.0, code)
}

fun parseAmountInputToMinor(text: String, currency: String?): Long? {
    val code = currency?.uppercase(Locale.ROOT) ?: "PKR"
    val scale = when (code) { "JPY" -> 0; else -> 2 }
    val cleaned = text.replace("[^0-9\\.]".toRegex(), "")
    if (cleaned.isBlank()) return null
    return try {
        if (scale == 0) cleaned.toLong()
        else {
            val parts = cleaned.split('.')
            when (parts.size) {
                1 -> parts[0].toLong() * 100
                else -> parts[0].toLong() * 100 + parts[1].padEnd(2, '0').take(2).toLong()
            }
        }
    } catch (_: NumberFormatException) { null }
}
