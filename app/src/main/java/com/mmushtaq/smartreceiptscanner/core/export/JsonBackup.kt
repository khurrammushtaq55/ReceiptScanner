package com.mmushtaq.smartreceiptscanner.core.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mmushtaq.smartreceiptscanner.core.data.db.MerchantPatternEntity
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val receipts: List<ReceiptEntity>,
    val merchantPatterns: List<MerchantPatternEntity>
)

/**
 * Local backup/restore only (no cloud/network involved). Deliberately hand-rolled JSON via
 * org.json (bundled with Android) rather than pulling in a serialization library for this
 * small, stable schema.
 */
object JsonBackup {

    private const val SCHEMA_VERSION = 1
    private const val AUTHORITY_SUFFIX = ".fp"

    fun export(
        context: Context,
        receipts: List<ReceiptEntity>,
        merchantPatterns: List<MerchantPatternEntity>,
        fileName: String = defaultFileName()
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(toJsonString(receipts, merchantPatterns))

        return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    }

    /** Pure JSON generation — no Context/Uri involved, safe to unit test directly. */
    fun toJsonString(receipts: List<ReceiptEntity>, merchantPatterns: List<MerchantPatternEntity>): String {
        val root = JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("receipts", JSONArray().apply { receipts.forEach { put(receiptToJson(it)) } })
            put("merchantPatterns", JSONArray().apply { merchantPatterns.forEach { put(patternToJson(it)) } })
        }
        return root.toString(2)
    }

    fun parse(jsonText: String): BackupPayload {
        val root = JSONObject(jsonText)
        val receiptsArr = root.optJSONArray("receipts") ?: JSONArray()
        val patternsArr = root.optJSONArray("merchantPatterns") ?: JSONArray()

        val receipts = (0 until receiptsArr.length()).map { receiptFromJson(receiptsArr.getJSONObject(it)) }
        val patterns = (0 until patternsArr.length()).map { patternFromJson(patternsArr.getJSONObject(it)) }

        return BackupPayload(receipts, patterns)
    }

    fun defaultFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "receipts_backup_$stamp.json"
    }

    private fun receiptToJson(r: ReceiptEntity) = JSONObject().apply {
        put("id", r.id)
        put("imageUri", r.imageUri)
        put("rawText", r.rawText)
        put("createdAt", r.createdAt)
        putOpt("dateEpochMs", r.dateEpochMs)
        putOpt("merchant", r.merchant)
        putOpt("totalMinor", r.totalMinor)
        putOpt("currency", r.currency)
        putOpt("category", r.category)
    }

    private fun receiptFromJson(o: JSONObject) = ReceiptEntity(
        id = o.getString("id"),
        imageUri = o.getString("imageUri"),
        rawText = o.getString("rawText"),
        createdAt = o.getLong("createdAt"),
        dateEpochMs = o.optLongOrNull("dateEpochMs"),
        merchant = o.optStringOrNull("merchant"),
        totalMinor = o.optLongOrNull("totalMinor"),
        currency = o.optStringOrNull("currency"),
        category = o.optStringOrNull("category")
    )

    private fun patternToJson(p: MerchantPatternEntity) = JSONObject().apply {
        put("merchantKey", p.merchantKey)
        putOpt("category", p.category)
        putOpt("currency", p.currency)
        put("updatedAt", p.updatedAt)
    }

    private fun patternFromJson(o: JSONObject) = MerchantPatternEntity(
        merchantKey = o.getString("merchantKey"),
        category = o.optStringOrNull("category"),
        currency = o.optStringOrNull("currency"),
        updatedAt = o.optLong("updatedAt")
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null
}
