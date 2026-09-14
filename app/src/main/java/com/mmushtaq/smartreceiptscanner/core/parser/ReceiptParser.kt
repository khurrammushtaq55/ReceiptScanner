package com.mmushtaq.smartreceiptscanner.core.parser

import com.mmushtaq.smartreceiptscanner.core.data.Categories
import java.time.*
import java.util.Locale
import kotlin.math.pow

data class ParsedReceipt(
    val merchant: String? = null,
    val dateEpochMs: Long? = null,
    val currency: String? = null,
    val totalMinor: Long? = null,
    val taxMinor: Long? = null,
    val suggestedCategory: String? = null,
    /** Per-field confidence in [0f, 1f], keyed by [ReceiptParser.Field]. Missing key = not attempted. */
    val confidence: Map<String, Float> = emptyMap()
)

object ReceiptParser {

    /** Keys used in [ParsedReceipt.confidence]. */
    object Field {
        const val MERCHANT = "merchant"
        const val DATE = "date"
        const val CURRENCY = "currency"
        const val TOTAL = "total"
        const val TAX = "tax"
    }

    /** Fields scoring below this in Review should be visually flagged for the user to double-check. */
    const val LOW_CONFIDENCE_THRESHOLD = 0.5f

    private val monthMap = mapOf(
        "JAN" to 1, "FEB" to 2, "MAR" to 3, "APR" to 4, "MAY" to 5, "JUN" to 6,
        "JUL" to 7, "AUG" to 8, "SEP" to 9, "SEPT" to 9, "OCT" to 10, "NOV" to 11, "DEC" to 12
    )

    // Rs, ₨, $, €, £, AED, SAR, USD, PKR, GBP
    private val currencyRegex = Regex("""(PKR|Rs\.?|₨|USD|\$|AED|SAR|EUR|€|GBP|£)""", RegexOption.IGNORE_CASE)

    // Money pattern: 1,234.56 | 1234.56 | 1234 | Rs 1,234.00 etc.
    private val moneyRegex = Regex("""(?<![A-Za-z0-9])(?:PKR|USD|AED|SAR|GBP|EUR|Rs\.?|₨|\$|€|£)?\s*([0-9]{1,3}(?:[,\s][0-9]{3})*|[0-9]+)(?:\.(\d{1,2}))?(?![A-Za-z0-9])""")

    private val likelyTotalHints = listOf("grand total", "total", "amount due", "balance due", "net total")
    private val taxHints = listOf("tax", "gst", "vat", "sales tax")

    // Generic receipt/label words only — NOT business-type words like "mart", "store",
    // "restaurant", "pharmacy", since those very commonly appear *inside* real merchant
    // names (e.g. "AL-FAISAL MART", "City Pharmacy") and would wrongly reject them.
    private val merchantNoise = listOf("invoice", "receipt", "bill")

    fun parse(raw: String, defaultCurrency: String = "PKR", tz: ZoneId = ZoneId.systemDefault()): ParsedReceipt {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val detectedCurrency = detectCurrency(raw)
        val currency = detectedCurrency ?: defaultCurrency
        // Explicit symbol/code found in the text vs. silently falling back to the app default.
        val currencyConfidence = if (detectedCurrency != null) 0.9f else 0.3f

        val hintedTotal = candidateTotals(lines, currency).maxByOrNull { it.second }?.second
        val totalMinor = hintedTotal ?: lastBlockMax(lines, currency) // fallback: max of last lines
        val totalConfidence = when {
            hintedTotal != null -> 0.9f      // matched a "total"/"grand total"/etc. hint line
            totalMinor != null -> 0.4f       // guessed from the largest amount near the bottom
            else -> 0f
        }

        val taxMinor = lines.firstNotNullOfOrNull { line ->
            if (taxHints.any { line.contains(it, ignoreCase = true) }) {
                extractMinor(line, currency)
            } else null
        }
        val taxConfidence = if (taxMinor != null) 0.85f else 0f

        val dateMs = parseAnyDate(lines, tz)
        val dateConfidence = if (dateMs != null) 0.8f else 0f

        val merchant = guessMerchant(lines)
        val merchantConfidence = if (merchant != null) 0.7f else 0f

        val suggestedCategory = Categories.guessFromMerchant(merchant)?.id

        val confidence = mapOf(
            Field.MERCHANT to merchantConfidence,
            Field.DATE to dateConfidence,
            Field.CURRENCY to currencyConfidence,
            Field.TOTAL to totalConfidence,
            Field.TAX to taxConfidence
        )

        return ParsedReceipt(
            merchant = merchant,
            dateEpochMs = dateMs,
            currency = currency,
            totalMinor = totalMinor,
            taxMinor = taxMinor,
            suggestedCategory = suggestedCategory,
            confidence = confidence
        )
    }

    // --- helpers ---

    private fun detectCurrency(text: String): String? {
        val hit = currencyRegex.find(text)?.value?.uppercase(Locale.ROOT) ?: return null
        return when {
            hit.startsWith("PKR") || hit.startsWith("RS") || hit.startsWith("₨") -> "PKR"
            hit == "$" || hit.contains("USD") -> "USD"
            hit == "€" || hit.contains("EUR") -> "EUR"
            hit == "£" || hit.contains("GBP") -> "GBP"
            hit.contains("AED") -> "AED"
            hit.contains("SAR") -> "SAR"
            else -> null
        }
    }

    private fun candidateTotals(lines: List<String>, currency: String): List<Pair<String, Long>> =
        lines.flatMap { l ->
            if (likelyTotalHints.any { l.contains(it, ignoreCase = true) }) {
                extractMinor(l, currency)?.let { listOf(l to it) } ?: emptyList()
            } else emptyList()
        }

    private fun lastBlockMax(lines: List<String>, currency: String): Long? =
        lines.takeLast(8).mapNotNull { extractMinor(it, currency) }.maxOrNull()

    private fun extractMinor(line: String, currency: String): Long? {
        val m = moneyRegex.find(line) ?: return null
        val whole = m.groups[1]?.value?.replace("[,\\s]".toRegex(), "") ?: return null
        val frac = m.groups[2]?.value ?: "00"
        val scale = currencyMinorUnits(currency)
        return try {
            // normalize fractional digits to 'scale'
            val normFrac = when {
                scale == 0 -> "0"
                frac.length >= scale -> frac.take(scale)
                else -> frac.padEnd(scale, '0')
            }
            whole.toLong() * (10.0.pow(scale)).toLong() + normFrac.toLong()
        } catch (_: NumberFormatException) { null }
    }

    private fun currencyMinorUnits(ccy: String): Int = when (ccy.uppercase(Locale.ROOT)) {
        "JPY" -> 0
        else -> 2 // PKR, USD, AED, SAR, EUR, GBP all 2
    }

    private fun Double.pow(exp: Int): Double = this.pow(exp.toDouble())

    private fun parseAnyDate(lines: List<String>, zone: ZoneId): Long? {
        // Try common numeric formats quickly
        lines.forEach { line ->
            parseDateNumeric(line, zone)?.let { return it }
        }
        // Try textual month formats
        lines.forEach { line ->
            parseDateTextual(line, zone)?.let { return it }
        }
        return null
    }

    private fun parseDateNumeric(text: String, zone: ZoneId): Long? {
        val rxNumeric = Regex("""\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""")
        val m = rxNumeric.find(text) ?: return null
        val (a, b, yRaw) = m.destructured
        val d = a.toIntOrNull() ?: return null
        val mo = b.toIntOrNull() ?: return null
        val y = yRaw.toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: return null
        return safeEpoch(y, mo, d, zone)
    }


    private fun parseDateTextual(text: String, zone: ZoneId): Long? {
        // 12 Aug 2025 / Aug 12, 2025
        val rxA = Regex("""\b(\d{1,2})\s+([A-Za-z]{3,9})[,]?\s+(\d{2,4})\b""")
        val rxB = Regex("""\b([A-Za-z]{3,9})\s+(\d{1,2})[,]?\s+(\d{2,4})\b""")

        rxA.find(text)?.let { m ->
            val (dRaw, monRaw, yRaw) = m.destructured
            val d = dRaw.toIntOrNull() ?: return null
            val mo = monthMap[monRaw.take(3).uppercase(Locale.ROOT)] ?: return null
            val y = yRaw.toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: return null
            return safeEpoch(y, mo, d, zone)
        }
        rxB.find(text)?.let { m ->
            val (monRaw, dRaw, yRaw) = m.destructured
            val d = dRaw.toIntOrNull() ?: return null
            val mo = monthMap[monRaw.take(3).uppercase(Locale.ROOT)] ?: return null
            val y = yRaw.toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: return null
            return safeEpoch(y, mo, d, zone)
        }
        return null
    }

    private fun safeEpoch(y: Int, m: Int, d: Int, zone: ZoneId): Long? = try {
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()
    } catch (_: DateTimeException) { null }

    private fun guessMerchant(lines: List<String>): String? {
        // Use first 1–3 top lines that look like a name (no digits, not a common keyword)
        val top = lines.take(5)
        val cand = top.firstOrNull { line ->
            !line.any(Char::isDigit) &&
                    merchantNoise.none { kw -> line.contains(kw, ignoreCase = true) } &&
                    line.length in 3..40
        }
        return cand?.replace(Regex("[^A-Za-z0-9 &'-]"), "")?.trim()
    }
}