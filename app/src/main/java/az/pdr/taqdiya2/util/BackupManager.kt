package az.pdr.taqdiya2.util

import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.PassingExpenseEntity
import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.data.local.VendorPaymentEntity
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

object BackupManager {
    private const val BACKUP_FORMAT_TAG = "TAQDIYA_LEDGER_BACKUP_V1"

    fun encodeToJson(payload: FullBackupPayload): String {
        val root = JSONObject()
        root.put("format", BACKUP_FORMAT_TAG)
        root.put("exportedAt", payload.exportedAt)

        // 1. Settings
        val settingsObj = JSONObject().apply {
            put("themeMode", payload.appSettings.themeMode)
            put("showRial", payload.appSettings.showRial)
            put("uiScaleMode", payload.appSettings.uiScaleMode)
            put("privacyMaskAmounts", payload.appSettings.privacyMaskAmounts)
            put("privacyMaskPocketOnly", payload.appSettings.privacyMaskPocketOnly)
            put("pinCode", payload.appSettings.pinCode)
            put("colorPalette", payload.appSettings.colorPalette)
            put("animationMode", payload.appSettings.animationMode)
            put("fontFamilyKey", payload.appSettings.fontFamilyKey)
            put("customFontRegularName", payload.appSettings.customFontRegularName)
            put("customFontMediumName", payload.appSettings.customFontMediumName)
            put("customFontBoldName", payload.appSettings.customFontBoldName)
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
                    put("pendingBalanceDirhams", v.pendingBalanceDirhams)
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
                    put("initialPocketDirhams", r.initialPocketDirhams)
                    put("createdAt", r.createdAt)
                    put("isActive", r.isActive)
                    put("isArchived", r.isArchived)
                    if (r.completedAt != null) put("completedAt", r.completedAt)
                    put("archivedTotalSpentDirhams", r.archivedTotalSpentDirhams)
                    put("archivedSavingsDirhams", r.archivedSavingsDirhams)
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
                    put("unitPriceDirhams", item.unitPriceDirhams)
                    put("quantity", item.quantity)
                    put("cleaningFeeDirhams", item.cleaningFeeDirhams)
                    put("totalDirhams", item.totalDirhams)
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
                    put("calculatedTotalDirhams", p.calculatedTotalDirhams)
                    put("actualPaidDirhams", p.actualPaidDirhams)
                    put("forgivenDiscountDirhams", p.forgivenDiscountDirhams)
                    put("recordedPendingChangeDirhams", p.recordedPendingChangeDirhams)
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
                    put("amountDirhams", e.amountDirhams)
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
                    put("lastPriceDirhams", d.lastPriceDirhams)
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
                    put("unitPriceDirhams", h.unitPriceDirhams)
                    put("quantity", h.quantity)
                    put("totalPaidDirhams", h.totalPaidDirhams)
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
                    pinCode = settingsObj.optString("pinCode", ""),
                    colorPalette = settingsObj.optString("colorPalette", "TERRACOTTA"),
                    animationMode = settingsObj.optString("animationMode", "LIGHT"),
                    fontFamilyKey = when (val rawFont = settingsObj.optString("fontFamilyKey", "IBM_PLEX")) {
                        "CAIRO", "TAJAWAL", "" -> "IBM_PLEX"
                        else -> rawFont
                    },
                    customFontRegularName = settingsObj.optString("customFontRegularName", ""),
                    customFontMediumName = settingsObj.optString("customFontMediumName", ""),
                    customFontBoldName = settingsObj.optString("customFontBoldName", "")
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
                            pendingBalanceDirhams = o.optDouble("pendingBalanceDirhams", 0.0),
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
                            initialPocketDirhams = o.optDouble("initialPocketDirhams", 200.0),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            isActive = o.optBoolean("isActive", false),
                            isArchived = o.optBoolean("isArchived", false),
                            completedAt = if (o.has("completedAt") && !o.isNull("completedAt")) o.getLong("completedAt") else null,
                            archivedTotalSpentDirhams = o.optDouble("archivedTotalSpentDirhams", 0.0),
                            archivedSavingsDirhams = o.optDouble("archivedSavingsDirhams", 0.0),
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
                            unitPriceDirhams = o.optDouble("unitPriceDirhams", 0.0),
                            quantity = o.optDouble("quantity", 1.0),
                            cleaningFeeDirhams = o.optDouble("cleaningFeeDirhams", 0.0),
                            totalDirhams = o.optDouble("totalDirhams", 0.0),
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
                            calculatedTotalDirhams = o.optDouble("calculatedTotalDirhams", 0.0),
                            actualPaidDirhams = o.optDouble("actualPaidDirhams", 0.0),
                            forgivenDiscountDirhams = o.optDouble("forgivenDiscountDirhams", 0.0),
                            recordedPendingChangeDirhams = o.optDouble("recordedPendingChangeDirhams", 0.0),
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
                            amountDirhams = o.optDouble("amountDirhams", 0.0),
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
                            lastPriceDirhams = o.optDouble("lastPriceDirhams", 0.0),
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
                            unitPriceDirhams = o.optDouble("unitPriceDirhams", 0.0),
                            quantity = o.optDouble("quantity", 1.0),
                            totalPaidDirhams = o.optDouble("totalPaidDirhams", 0.0),
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
