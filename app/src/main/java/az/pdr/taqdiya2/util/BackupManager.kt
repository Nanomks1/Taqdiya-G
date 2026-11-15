package az.pdr.taqdiya2.util

import android.content.Context
import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.PassingExpenseEntity
import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.data.local.VendorPaymentEntity
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class FullBackupPayload(
    val exportedAt: Long,
    val vendors: List<VendorEntity>,
    val rounds: List<ShoppingRoundEntity>,
    val shoppingItems: List<ShoppingItemEntity>,
    val vendorPayments: List<VendorPaymentEntity>,
    val passingExpenses: List<PassingExpenseEntity>,
    val dictionaryItems: List<DictionaryItemEntity>,
    val priceHistory: List<PriceHistoryEntity>,
    val appSettings: AppSettingsEntity
)

data class AutoBackupInfo(
    val lastSavedAtMillis: Long,
    val hasPreviousSnapshot: Boolean,
    val sizeBytes: Long
)

object BackupManager {
    private const val BACKUP_FORMAT_TAG = "TAQDIYA_LEDGER_BACKUP_V1"
    private const val AUTO_BACKUP_DIR = "backups"
    private const val AUTO_BACKUP_LATEST_FILE = "auto_backup_latest.json"
    private const val AUTO_BACKUP_PREVIOUS_FILE = "auto_backup_previous.json"

    fun saveAutoBackup(context: Context, json: String) {
        runCatching {
            val dir = File(context.filesDir, AUTO_BACKUP_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val latestFile = File(dir, AUTO_BACKUP_LATEST_FILE)
            val previousFile = File(dir, AUTO_BACKUP_PREVIOUS_FILE)
            if (latestFile.exists() && latestFile.length() > 0L) {
                latestFile.copyTo(previousFile, overwrite = true)
            }
            latestFile.writeText(json, Charsets.UTF_8)
        }
    }

    fun getAutoBackupInfo(context: Context): AutoBackupInfo? {
        return runCatching {
            val dir = File(context.filesDir, AUTO_BACKUP_DIR)
            val latestFile = File(dir, AUTO_BACKUP_LATEST_FILE)
            val previousFile = File(dir, AUTO_BACKUP_PREVIOUS_FILE)
            if (!latestFile.exists() || latestFile.length() <= 0L) {
                null
            } else {
                AutoBackupInfo(
                    lastSavedAtMillis = latestFile.lastModified(),
                    hasPreviousSnapshot = previousFile.exists() && previousFile.length() > 0L,
                    sizeBytes = latestFile.length()
                )
            }
        }.getOrNull()
    }

    fun readLatestAutoBackupJson(context: Context): String? {
        return runCatching {
            val dir = File(context.filesDir, AUTO_BACKUP_DIR)
            val latestFile = File(dir, AUTO_BACKUP_LATEST_FILE)
            val previousFile = File(dir, AUTO_BACKUP_PREVIOUS_FILE)
            when {
                latestFile.exists() && latestFile.length() > 0L -> latestFile.readText(Charsets.UTF_8)
                previousFile.exists() && previousFile.length() > 0L -> previousFile.readText(Charsets.UTF_8)
                else -> null
            }
        }.getOrNull()
    }

    fun encodeToJson(payload: FullBackupPayload): String {
        val root = JSONObject()
        root.put("format", BACKUP_FORMAT_TAG)
        root.put("exportedAt", payload.exportedAt)

        // 1. Settings (PIN is always hashed with SHA-256 before exporting)
        val normalizedFont = when (payload.appSettings.fontFamilyKey) {
            "VAZIRMATN", "BALOO", "SYSTEM" -> payload.appSettings.fontFamilyKey
            else -> "IBM_PLEX"
        }
        val settingsObj = JSONObject().apply {
            put("themeMode", payload.appSettings.themeMode)
            put("showRial", payload.appSettings.showRial)
            put("uiScaleMode", payload.appSettings.uiScaleMode)
            put("privacyMaskAmounts", payload.appSettings.privacyMaskAmounts)
            put("privacyMaskPocketOnly", payload.appSettings.privacyMaskPocketOnly)
            put("pinCode", PinSecurityUtil.normalizeForStorage(payload.appSettings.pinCode))
            put("colorPalette", payload.appSettings.colorPalette)
            put("animationMode", payload.appSettings.animationMode)
            put("fontFamilyKey", normalizedFont)
        }
        root.put("appSettings", settingsObj)

        // 2. Vendors
        val vendorsArr = JSONArray()
        payload.vendors.forEach { v ->
            vendorsArr.put(
                JSONObject().apply {
                    put("id", v.id)
                    put("name", v.name)
                    put("specialty", v.specialty)
                    put("iconKey", v.iconKey)
                    put("sortOrder", v.sortOrder)
                    put("pendingBalanceDirhams", CurrencyFormatter.roundMoney(v.pendingBalanceDirhams))
                    put("isDeleted", v.isDeleted)
                    if (v.deletedAt != null) put("deletedAt", v.deletedAt)
                }
            )
        }
        root.put("vendors", vendorsArr)

        // 3. Shopping Rounds
        val roundsArr = JSONArray()
        payload.rounds.forEach { r ->
            roundsArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("initialPocketDirhams", CurrencyFormatter.roundMoney(r.initialPocketDirhams))
                    put("createdAt", r.createdAt)
                    put("isActive", r.isActive)
                    put("isArchived", r.isArchived)
                    if (r.completedAt != null) put("completedAt", r.completedAt)
                    put("archivedTotalSpentDirhams", CurrencyFormatter.roundMoney(r.archivedTotalSpentDirhams))
                    put("archivedSavingsDirhams", CurrencyFormatter.roundMoney(r.archivedSavingsDirhams))
                    put("archivedItemsCount", r.archivedItemsCount)
                }
            )
        }
        root.put("rounds", roundsArr)

        // 4. Shopping Items
        val itemsArr = JSONArray()
        payload.shoppingItems.forEach { item ->
            itemsArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("roundId", item.roundId)
                    put("vendorId", item.vendorId)
                    put("name", item.name)
                    put("note", item.note)
                    put("isPurchased", item.isPurchased)
                    put("purchaseMode", item.purchaseMode)
                    put("unitPriceDirhams", CurrencyFormatter.roundMoney(item.unitPriceDirhams))
                    put("quantity", item.quantity)
                    put("cleaningFeeDirhams", CurrencyFormatter.roundMoney(item.cleaningFeeDirhams))
                    put("totalDirhams", CurrencyFormatter.roundMoney(item.totalDirhams))
                    put("createdAt", item.createdAt)
                    put("isDeleted", item.isDeleted)
                    if (item.deletedAt != null) put("deletedAt", item.deletedAt)
                }
            )
        }
        root.put("shoppingItems", itemsArr)

        // 5. Vendor Payments
        val paymentsArr = JSONArray()
        payload.vendorPayments.forEach { p ->
            paymentsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("roundId", p.roundId)
                    put("vendorId", p.vendorId)
                    put("calculatedTotalDirhams", CurrencyFormatter.roundMoney(p.calculatedTotalDirhams))
                    put("actualPaidDirhams", CurrencyFormatter.roundMoney(p.actualPaidDirhams))
                    put("forgivenDiscountDirhams", CurrencyFormatter.roundMoney(p.forgivenDiscountDirhams))
                    put("recordedPendingChangeDirhams", CurrencyFormatter.roundMoney(p.recordedPendingChangeDirhams))
                    put("paidAt", p.paidAt)
                }
            )
        }
        root.put("vendorPayments", paymentsArr)

        // 6. Passing Expenses
        val expensesArr = JSONArray()
        payload.passingExpenses.forEach { e ->
            expensesArr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("roundId", e.roundId)
                    put("title", e.title)
                    put("amountDirhams", CurrencyFormatter.roundMoney(e.amountDirhams))
                    put("createdAt", e.createdAt)
                    put("isDeleted", e.isDeleted)
                    if (e.deletedAt != null) put("deletedAt", e.deletedAt)
                }
            )
        }
        root.put("passingExpenses", expensesArr)

        // 7. Dictionary Items
        val dictArr = JSONArray()
        payload.dictionaryItems.forEach { d ->
            dictArr.put(
                JSONObject().apply {
                    put("id", d.id)
                    put("name", d.name)
                    put("normalizedName", d.normalizedName)
                    put("defaultVendorId", d.defaultVendorId)
                    put("defaultNote", d.defaultNote)
                    put("customUnitsCsv", d.customUnitsCsv)
                    put("usageCount", d.usageCount)
                    put("lastPriceDirhams", CurrencyFormatter.roundMoney(d.lastPriceDirhams))
                    put("lastUpdated", d.lastUpdated)
                }
            )
        }
        root.put("dictionaryItems", dictArr)

        // 8. Price History
        val historyArr = JSONArray()
        payload.priceHistory.forEach { h ->
            historyArr.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("itemName", h.itemName)
                    put("normalizedName", h.normalizedName)
                    put("vendorId", h.vendorId)
                    put("vendorName", h.vendorName)
                    put("purchaseMode", h.purchaseMode)
                    put("unitPriceDirhams", CurrencyFormatter.roundMoney(h.unitPriceDirhams))
                    put("quantity", h.quantity)
                    put("totalPaidDirhams", CurrencyFormatter.roundMoney(h.totalPaidDirhams))
                    if (h.shoppingItemId != null) put("shoppingItemId", h.shoppingItemId)
                    put("recordedAt", h.recordedAt)
                }
            )
        }
        root.put("priceHistory", historyArr)

        return root.toString(2)
    }

    fun decodeFromJson(rawJson: String): Result<FullBackupPayload> {
        return try {
            val clean = rawJson.trim()
            if (clean.isEmpty()) {
                return Result.failure(IllegalArgumentException("ملف أو نص النسخة الاحتياطية فارغ"))
            }
            val root = JSONObject(clean)
            val format = root.optString("format", "")
            if (format != BACKUP_FORMAT_TAG) {
                return Result.failure(
                    IllegalArgumentException("هذا الملف ليس نسخة احتياطية صالحة لتطبيق كناش التقدية")
                )
            }

            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            val settingsObj = root.optJSONObject("appSettings")
            val appSettings = if (settingsObj != null) {
                AppSettingsEntity(
                    id = 1,
                    themeMode = settingsObj.optString("themeMode", "LIGHT"),
                    showRial = settingsObj.optBoolean("showRial", true),
                    uiScaleMode = settingsObj.optString("uiScaleMode", "NORMAL"),
                    privacyMaskAmounts = settingsObj.optBoolean("privacyMaskAmounts", false),
                    privacyMaskPocketOnly = settingsObj.optBoolean("privacyMaskPocketOnly", false),
                    pinCode = PinSecurityUtil.normalizeForStorage(settingsObj.optString("pinCode", "")),
                    colorPalette = settingsObj.optString("colorPalette", "TERRACOTTA"),
                    animationMode = settingsObj.optString("animationMode", "LIGHT"),
                    fontFamilyKey = when (val rawFont = settingsObj.optString("fontFamilyKey", "IBM_PLEX")) {
                        "VAZIRMATN", "BALOO", "SYSTEM" -> rawFont
                        else -> "IBM_PLEX"
                    }
                )
            } else {
                AppSettingsEntity()
            }

            val vendors = buildList {
                val arr = root.optJSONArray("vendors") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VendorEntity(
                            id = o.getLong("id"),
                            name = o.getString("name"),
                            specialty = o.optString("specialty", ""),
                            iconKey = o.optString("iconKey", "store"),
                            sortOrder = o.optInt("sortOrder", i + 1),
                            pendingBalanceDirhams = CurrencyFormatter.roundMoney(o.optDouble("pendingBalanceDirhams", 0.0)),
                            isDeleted = o.optBoolean("isDeleted", false),
                            deletedAt = if (o.has("deletedAt") && !o.isNull("deletedAt")) o.getLong("deletedAt") else null
                        )
                    )
                }
            }

            val rounds = buildList {
                val arr = root.optJSONArray("rounds") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ShoppingRoundEntity(
                            id = o.getLong("id"),
                            title = o.optString("title", "جولة التقدية"),
                            initialPocketDirhams = CurrencyFormatter.roundMoney(o.optDouble("initialPocketDirhams", 0.0)),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            isActive = o.optBoolean("isActive", false),
                            isArchived = o.optBoolean("isArchived", false),
                            completedAt = if (o.has("completedAt") && !o.isNull("completedAt")) o.getLong("completedAt") else null,
                            archivedTotalSpentDirhams = CurrencyFormatter.roundMoney(o.optDouble("archivedTotalSpentDirhams", 0.0)),
                            archivedSavingsDirhams = CurrencyFormatter.roundMoney(o.optDouble("archivedSavingsDirhams", 0.0)),
                            archivedItemsCount = o.optInt("archivedItemsCount", 0)
                        )
                    )
                }
            }

            val shoppingItems = buildList {
                val arr = root.optJSONArray("shoppingItems") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ShoppingItemEntity(
                            id = o.getLong("id"),
                            roundId = o.getLong("roundId"),
                            vendorId = o.getLong("vendorId"),
                            name = o.getString("name"),
                            note = o.optString("note", ""),
                            isPurchased = o.optBoolean("isPurchased", false),
                            purchaseMode = o.optString("purchaseMode", "WEIGHT"),
                            unitPriceDirhams = CurrencyFormatter.roundMoney(o.optDouble("unitPriceDirhams", 0.0)),
                            quantity = o.optDouble("quantity", 1.0),
                            cleaningFeeDirhams = CurrencyFormatter.roundMoney(o.optDouble("cleaningFeeDirhams", 0.0)),
                            totalDirhams = CurrencyFormatter.roundMoney(o.optDouble("totalDirhams", 0.0)),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            isDeleted = o.optBoolean("isDeleted", false),
                            deletedAt = if (o.has("deletedAt") && !o.isNull("deletedAt")) o.getLong("deletedAt") else null
                        )
                    )
                }
            }

            val vendorPayments = buildList {
                val arr = root.optJSONArray("vendorPayments") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VendorPaymentEntity(
                            id = o.getLong("id"),
                            roundId = o.getLong("roundId"),
                            vendorId = o.getLong("vendorId"),
                            calculatedTotalDirhams = CurrencyFormatter.roundMoney(o.optDouble("calculatedTotalDirhams", 0.0)),
                            actualPaidDirhams = CurrencyFormatter.roundMoney(o.optDouble("actualPaidDirhams", 0.0)),
                            forgivenDiscountDirhams = CurrencyFormatter.roundMoney(o.optDouble("forgivenDiscountDirhams", 0.0)),
                            recordedPendingChangeDirhams = CurrencyFormatter.roundMoney(o.optDouble("recordedPendingChangeDirhams", 0.0)),
                            paidAt = o.optLong("paidAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val passingExpenses = buildList {
                val arr = root.optJSONArray("passingExpenses") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        PassingExpenseEntity(
                            id = o.getLong("id"),
                            roundId = o.getLong("roundId"),
                            title = o.optString("title", "مصروف عابر"),
                            amountDirhams = CurrencyFormatter.roundMoney(o.optDouble("amountDirhams", 0.0)),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            isDeleted = o.optBoolean("isDeleted", false),
                            deletedAt = if (o.has("deletedAt") && !o.isNull("deletedAt")) o.getLong("deletedAt") else null
                        )
                    )
                }
            }

            val dictionaryItems = buildList {
                val arr = root.optJSONArray("dictionaryItems") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.getString("name")
                    add(
                        DictionaryItemEntity(
                            id = o.getLong("id"),
                            name = name,
                            normalizedName = o.optString("normalizedName", CurrencyFormatter.normalizeArabicName(name)),
                            defaultVendorId = o.getLong("defaultVendorId"),
                            defaultNote = o.optString("defaultNote", ""),
                            customUnitsCsv = o.optString("customUnitsCsv", ""),
                            usageCount = o.optInt("usageCount", 1),
                            lastPriceDirhams = CurrencyFormatter.roundMoney(o.optDouble("lastPriceDirhams", 0.0)),
                            lastUpdated = o.optLong("lastUpdated", System.currentTimeMillis())
                        )
                    )
                }
            }

            val priceHistory = buildList {
                val arr = root.optJSONArray("priceHistory") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val itemName = o.getString("itemName")
                    add(
                        PriceHistoryEntity(
                            id = o.getLong("id"),
                            itemName = itemName,
                            normalizedName = o.optString("normalizedName", CurrencyFormatter.normalizeArabicName(itemName)),
                            vendorId = o.getLong("vendorId"),
                            vendorName = o.optString("vendorName", "البائع"),
                            purchaseMode = o.optString("purchaseMode", "WEIGHT"),
                            unitPriceDirhams = CurrencyFormatter.roundMoney(o.optDouble("unitPriceDirhams", 0.0)),
                            quantity = o.optDouble("quantity", 1.0),
                            totalPaidDirhams = CurrencyFormatter.roundMoney(o.optDouble("totalPaidDirhams", 0.0)),
                            shoppingItemId = if (o.has("shoppingItemId") && !o.isNull("shoppingItemId")) o.getLong("shoppingItemId") else null,
                            recordedAt = o.optLong("recordedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            Result.success(
                FullBackupPayload(
                    exportedAt = exportedAt,
                    vendors = vendors,
                    rounds = rounds,
                    shoppingItems = shoppingItems,
                    vendorPayments = vendorPayments,
                    passingExpenses = passingExpenses,
                    dictionaryItems = dictionaryItems,
                    priceHistory = priceHistory,
                    appSettings = appSettings
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("تعذر قراءة النسخة الاحتياطية: تأكد من صحة الملف أو النص المنسوخ")
            )
        }
    }
}
