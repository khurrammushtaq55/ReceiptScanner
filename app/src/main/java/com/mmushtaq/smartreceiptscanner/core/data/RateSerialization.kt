package com.mmushtaq.smartreceiptscanner.core.data

/** Pure string (de)serialization for the rates map — no Android dependencies. */
object RateSerialization {

    fun serialize(rates: Map<String, Double>): String =
        rates.entries.joinToString(";") { (code, rate) -> "$code=$rate" }

    fun deserialize(raw: String?): Map<String, Double> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size != 2) return@mapNotNull null
                val code = parts[0].trim().uppercase()
                val value = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
                code to value
            }
            .toMap()
    }
}
