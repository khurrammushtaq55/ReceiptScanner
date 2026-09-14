package com.mmushtaq.smartreceiptscanner.core.data

import androidx.compose.ui.graphics.Color

/** A user-facing spend category. `id` is what's persisted on [com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity]. */
data class ReceiptCategory(
    val id: String,
    val label: String,
    val color: Color
)

object Categories {
    val Groceries = ReceiptCategory("groceries", "Groceries", Color(0xFF66BB6A))
    val Fuel = ReceiptCategory("fuel", "Fuel", Color(0xFFFFA726))
    val Dining = ReceiptCategory("dining", "Dining", Color(0xFFEF5350))
    val Utilities = ReceiptCategory("utilities", "Utilities", Color(0xFF42A5F5))
    val Shopping = ReceiptCategory("shopping", "Shopping", Color(0xFFAB47BC))
    val Health = ReceiptCategory("health", "Health", Color(0xFF26A69A))
    val Other = ReceiptCategory("other", "Other", Color(0xFF9E9E9E))

    /** Order here is also the display order in pickers/filters. */
    val all = listOf(Groceries, Fuel, Dining, Utilities, Shopping, Health, Other)

    fun byId(id: String?): ReceiptCategory = all.firstOrNull { it.id == id } ?: Other

    // Keyword -> category hints, checked against the (lowercased) merchant name.
    // First match wins; order matters for overlapping keywords.
    private val keywordHints: List<Pair<String, ReceiptCategory>> = listOf(
        "supermarket" to Groceries, "grocer" to Groceries, "mart" to Groceries, "bakery" to Groceries,
        "petrol" to Fuel, "fuel" to Fuel, "gas station" to Fuel, "shell" to Fuel, "total parco" to Fuel, "pso " to Fuel, "attock" to Fuel,
        "cafe" to Dining, "coffee" to Dining, "restaurant" to Dining, "kitchen" to Dining, "pizza" to Dining, "diner" to Dining, "bbq" to Dining, "food" to Dining,
        "electric" to Utilities, "wapda" to Utilities, "power" to Utilities, "gas company" to Utilities, "sui " to Utilities, "telecom" to Utilities, "internet" to Utilities, "ptcl" to Utilities,
        "pharmacy" to Health, "clinic" to Health, "hospital" to Health, "medical" to Health, "drug" to Health, "chemist" to Health,
        "mall" to Shopping, "fashion" to Shopping, "clothing" to Shopping, "electronics" to Shopping, "outfitters" to Shopping
    )

    /** Best-effort guess only — always editable by the user, never authoritative. */
    fun guessFromMerchant(merchant: String?): ReceiptCategory? {
        if (merchant.isNullOrBlank()) return null
        val m = merchant.lowercase()
        return keywordHints.firstOrNull { (kw, _) -> m.contains(kw) }?.second
    }
}
