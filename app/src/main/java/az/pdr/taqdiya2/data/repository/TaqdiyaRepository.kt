package az.pdr.taqdiya2.data.repository

import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.PassingExpenseEntity
import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.TaqdiyaDao
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.data.local.VendorPaymentEntity
import az.pdr.taqdiya2.util.BackupManager
import az.pdr.taqdiya2.util.CurrencyFormatter
import az.pdr.taqdiya2.util.FullBackupPayload
import kotlinx.coroutines.flow.Flow

class TaqdiyaRepository(private val dao: TaqdiyaDao) {

    val activeVendors: Flow<List<VendorEntity>> = dao.observeActiveVendors()
    val activeRound: Flow<ShoppingRoundEntity?> = dao.observeActiveRound()
    val archivedRounds: Flow<List<ShoppingRoundEntity>> = dao.observeArchivedRounds()
    val allActiveItems: Flow<List<ShoppingItemEntity>> = dao.observeAllActiveItems()
    val allVendorPayments: Flow<List<VendorPaymentEntity>> = dao.observeAllVendorPayments()
    val allPassingExpenses: Flow<List<PassingExpenseEntity>> = dao.observeAllActivePassingExpenses()
    val dictionaryItems: Flow<List<DictionaryItemEntity>> = dao.observeDictionaryItems()
    val allPriceHistory: Flow<List<PriceHistoryEntity>> = dao.observeAllPriceHistory()
    val appSettings: Flow<AppSettingsEntity?> = dao.observeAppSettings()
    val deletedShoppingItems: Flow<List<ShoppingItemEntity>> = dao.observeDeletedShoppingItems()
    val deletedPassingExpenses: Flow<List<PassingExpenseEntity>> = dao.observeDeletedPassingExpenses()
    val deletedVendors: Flow<List<VendorEntity>> = dao.observeDeletedVendors()

    private data class SeedDictEntry(
        val name: String,
        val vendorId: Long,
        val usageCount: Int,
        val referencePriceDh: Double,
        val mode: String = "WEIGHT"
    )

    suspend fun ensureInitialized() {
        // Initialize default AppSettings if not present
        if (dao.getAppSettingsOnce() == null) {
            dao.upsertAppSettings(AppSettingsEntity())
        }

        // Auto-purge Trash Bin items older than 30 days
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
        dao.purgeExpiredTrashShoppingItems(thirtyDaysAgo)
        dao.purgeExpiredTrashPassingExpenses(thirtyDaysAgo)
        dao.purgeExpiredTrashVendors(thirtyDaysAgo)
        val count = dao.getVendorCount()
        if (count == 0) {
            val defaultVendors = listOf(
                VendorEntity(
                    name = "العطار / مول الحانوت",
                    specialty = "القطاني كاللوبيا والعدس، التوابل، وأغراض المائدة",
                    iconKey = "store",
                    sortOrder = 1
                ),
                VendorEntity(
                    name = "مول الخضرة",
                    specialty = "الخضر، الربيع، النعناع، والطماطم",
                    iconKey = "eco",
                    sortOrder = 2
                ),
                VendorEntity(
                    name = "مول الفواكه",
                    specialty = "الديسير والفواكه الموسمية",
                    iconKey = "nutrition",
                    sortOrder = 3
                ),
                VendorEntity(
                    name = "الكزار ومول الدجاج والبيض",
                    specialty = "اللحوم، الدجاج، والبيض",
                    iconKey = "set_meal",
                    sortOrder = 4
                ),
                VendorEntity(
                    name = "الدروݣري ومواد النظافة",
                    specialty = "مواد التنظيف ومستلزمات الصيانة المنزلية",
                    iconKey = "cleaning",
                    sortOrder = 5
                ),
                VendorEntity(
                    name = "مول الدوزان",
                    specialty = "البراغي، المقص، الأدوات، ولوازم السوق الأسبوعي",
                    iconKey = "handyman",
                    sortOrder = 6
                ),
                VendorEntity(
                    name = "مول المواعن",
                    specialty = "أواني ومستلزمات المطبخ",
                    iconKey = "kitchen",
                    sortOrder = 7
                ),
                VendorEntity(
                    name = "مول الحوايج",
                    specialty = "الملابس والأنسجة في السوق",
                    iconKey = "checkroom",
                    sortOrder = 8
                )
            )
            val insertedIds = dao.insertVendors(defaultVendors)
            val attarId = insertedIds.getOrElse(0) { 1L }
            val khodraId = insertedIds.getOrElse(1) { 2L }
            val fawakihId = insertedIds.getOrElse(2) { 3L }
            val gazzarId = insertedIds.getOrElse(3) { 4L }
            val drogriId = insertedIds.getOrElse(4) { 5L }
            val dozanId = insertedIds.getOrElse(5) { 6L }
            val mwaenId = insertedIds.getOrElse(6) { 7L }

            val seedEntries = listOf(
                // الخضرة
                SeedDictEntry("مطيشة", khodraId, 12, 6.0, "WEIGHT"),
                SeedDictEntry("بطاطا", khodraId, 12, 5.0, "WEIGHT"),
                SeedDictEntry("بصلة", khodraId, 11, 5.0, "WEIGHT"),
                SeedDictEntry("ربيع ونعناع", khodraId, 10, 3.0, "FIXED"),
                SeedDictEntry("خيزو", khodraId, 8, 5.0, "WEIGHT"),
                SeedDictEntry("فلفلة", khodraId, 7, 7.0, "WEIGHT"),
                SeedDictEntry("قرعة خضراء", khodraId, 6, 6.0, "WEIGHT"),
                // العطار / مول الحانوت
                SeedDictEntry("لوبيا", attarId, 10, 18.0, "WEIGHT"),
                SeedDictEntry("لوبيا بيضاء", attarId, 4, 18.0, "WEIGHT"),
                SeedDictEntry("عدس", attarId, 9, 16.0, "WEIGHT"),
                SeedDictEntry("حمص", attarId, 8, 20.0, "WEIGHT"),
                SeedDictEntry("كمون", attarId, 9, 5.0, "FIXED"),
                SeedDictEntry("إبزار", attarId, 7, 5.0, "FIXED"),
                SeedDictEntry("زيت المائدة", attarId, 10, 18.0, "COUNT"),
                SeedDictEntry("أتاي", attarId, 11, 15.0, "COUNT"),
                SeedDictEntry("سكر", attarId, 10, 10.0, "WEIGHT"),
                SeedDictEntry("دقيق", attarId, 8, 6.0, "WEIGHT"),
                // الكزار والدجاج والبيض
                SeedDictEntry("دجاج", gazzarId, 11, 20.0, "WEIGHT"),
                SeedDictEntry("بيض", gazzarId, 12, 1.5, "COUNT"),
                SeedDictEntry("لحم غنمي", gazzarId, 7, 100.0, "WEIGHT"),
                SeedDictEntry("كفتة", gazzarId, 8, 90.0, "WEIGHT"),
                // الفواكه
                SeedDictEntry("بنان", fawakihId, 9, 12.0, "WEIGHT"),
                SeedDictEntry("تفاح", fawakihId, 8, 14.0, "WEIGHT"),
                SeedDictEntry("ليمون", fawakihId, 8, 7.0, "WEIGHT"),
                // الدروݣري والدوزان والمواعن
                SeedDictEntry("جافيل", drogriId, 8, 6.0, "COUNT"),
                SeedDictEntry("مسحوق الغسيل", drogriId, 7, 15.0, "COUNT"),
                SeedDictEntry("صابون", drogriId, 6, 5.0, "COUNT"),
                SeedDictEntry("براغي", dozanId, 4, 5.0, "FIXED"),
                SeedDictEntry("كيسان أتاي", mwaenId, 3, 25.0, "COUNT")
            )

            val seedDictionary = seedEntries.map { entry ->
                DictionaryItemEntity(
                    name = entry.name,
                    normalizedName = CurrencyFormatter.normalizeArabicName(entry.name),
                    defaultVendorId = entry.vendorId,
                    usageCount = entry.usageCount,
                    lastPriceDirhams = entry.referencePriceDh
                )
            }
            dao.insertDictionaryItems(seedDictionary)
        }

        // Ensure initial price history records exist so price indicator works immediately
        if (dao.getPriceHistoryCount() == 0) {
            val vendors = dao.getActiveVendorsOnce()
            val khodra = vendors.firstOrNull { it.name.contains("الخضرة") } ?: vendors.firstOrNull()
            val attar = vendors.firstOrNull { it.name.contains("العطار") } ?: vendors.firstOrNull()
            val gazzar = vendors.firstOrNull { it.name.contains("الكزار") } ?: vendors.firstOrNull()
            val fawakih = vendors.firstOrNull { it.name.contains("الفواكه") } ?: vendors.firstOrNull()

            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val initialHistory = buildList {
                if (khodra != null) {
                    // مطيشة: 7 DH (2 weeks ago), 6 DH (1 week ago), 5.5 DH (3 days ago)
                    add(
                        PriceHistoryEntity(
                            itemName = "مطيشة",
                            normalizedName = CurrencyFormatter.normalizeArabicName("مطيشة"),
                            vendorId = khodra.id,
                            vendorName = khodra.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 7.0,
                            quantity = 1.0,
                            totalPaidDirhams = 7.0,
                            recordedAt = now - 14 * dayMs
                        )
                    )
                    add(
                        PriceHistoryEntity(
                            itemName = "مطيشة",
                            normalizedName = CurrencyFormatter.normalizeArabicName("مطيشة"),
                            vendorId = khodra.id,
                            vendorName = khodra.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 6.0,
                            quantity = 1.0,
                            totalPaidDirhams = 6.0,
                            recordedAt = now - 7 * dayMs
                        )
                    )
                    // بطاطا: 5.5 DH, 5.0 DH
                    add(
                        PriceHistoryEntity(
                            itemName = "بطاطا",
                            normalizedName = CurrencyFormatter.normalizeArabicName("بطاطا"),
                            vendorId = khodra.id,
                            vendorName = khodra.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 5.5,
                            quantity = 1.0,
                            totalPaidDirhams = 5.5,
                            recordedAt = now - 12 * dayMs
                        )
                    )
                    add(
                        PriceHistoryEntity(
                            itemName = "بطاطا",
                            normalizedName = CurrencyFormatter.normalizeArabicName("بطاطا"),
                            vendorId = khodra.id,
                            vendorName = khodra.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 5.0,
                            quantity = 1.0,
                            totalPaidDirhams = 5.0,
                            recordedAt = now - 5 * dayMs
                        )
                    )
                    // بصلة: 5.0 DH
                    add(
                        PriceHistoryEntity(
                            itemName = "بصلة",
                            normalizedName = CurrencyFormatter.normalizeArabicName("بصلة"),
                            vendorId = khodra.id,
                            vendorName = khodra.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 5.0,
                            quantity = 1.0,
                            totalPaidDirhams = 5.0,
                            recordedAt = now - 6 * dayMs
                        )
                    )
                }
                if (gazzar != null) {
                    // دجاج: 21 DH, 19 DH
                    add(
                        PriceHistoryEntity(
                            itemName = "دجاج",
                            normalizedName = CurrencyFormatter.normalizeArabicName("دجاج"),
                            vendorId = gazzar.id,
                            vendorName = gazzar.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 21.0,
                            quantity = 2.0,
                            totalPaidDirhams = 45.0,
                            recordedAt = now - 10 * dayMs
                        )
                    )
                    add(
                        PriceHistoryEntity(
                            itemName = "دجاج",
                            normalizedName = CurrencyFormatter.normalizeArabicName("دجاج"),
                            vendorId = gazzar.id,
                            vendorName = gazzar.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 19.0,
                            quantity = 2.0,
                            totalPaidDirhams = 41.0,
                            recordedAt = now - 4 * dayMs
                        )
                    )
                    // بيض: 1.5 DH per egg
                    add(
                        PriceHistoryEntity(
                            itemName = "بيض",
                            normalizedName = CurrencyFormatter.normalizeArabicName("بيض"),
                            vendorId = gazzar.id,
                            vendorName = gazzar.name,
                            purchaseMode = "COUNT",
                            unitPriceDirhams = 1.5,
                            quantity = 30.0,
                            totalPaidDirhams = 45.0,
                            recordedAt = now - 8 * dayMs
                        )
                    )
                }
                if (attar != null) {
                    // لوبيا: 18 DH
                    add(
                        PriceHistoryEntity(
                            itemName = "لوبيا",
                            normalizedName = CurrencyFormatter.normalizeArabicName("لوبيا"),
                            vendorId = attar.id,
                            vendorName = attar.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 18.0,
                            quantity = 1.0,
                            totalPaidDirhams = 18.0,
                            recordedAt = now - 9 * dayMs
                        )
                    )
                    // عدس: 16 DH
                    add(
                        PriceHistoryEntity(
                            itemName = "عدس",
                            normalizedName = CurrencyFormatter.normalizeArabicName("عدس"),
                            vendorId = attar.id,
                            vendorName = attar.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 16.0,
                            quantity = 1.0,
                            totalPaidDirhams = 16.0,
                            recordedAt = now - 9 * dayMs
                        )
                    )
                }
                if (fawakih != null) {
                    // بنان: 12 DH
                    add(
                        PriceHistoryEntity(
                            itemName = "بنان",
                            normalizedName = CurrencyFormatter.normalizeArabicName("بنان"),
                            vendorId = fawakih.id,
                            vendorName = fawakih.name,
                            purchaseMode = "WEIGHT",
                            unitPriceDirhams = 12.0,
                            quantity = 1.0,
                            totalPaidDirhams = 12.0,
                            recordedAt = now - 6 * dayMs
                        )
                    )
                }
            }
            if (initialHistory.isNotEmpty()) {
                dao.insertPriceHistoryList(initialHistory)
            }
        }

        val currentRound = dao.getActiveRoundOnce()
        if (currentRound == null) {
            dao.insertRound(
                ShoppingRoundEntity(
                    title = "جولة التقدية الحالية",
                    initialPocketDirhams = 200.0,
                    isActive = true,
                    isArchived = false
                )
            )
        }
    }

    suspend fun updateInitialPocket(roundId: Long, pocketDirhams: Double) {
        dao.updateInitialPocket(roundId, pocketDirhams.coerceAtLeast(0.0))
    }

    suspend fun addShoppingItemAndLearn(
        roundId: Long,
        name: String,
        note: String,
        vendorId: Long
    ): ShoppingItemEntity {
        val cleanName = name.trim()
        val cleanNote = note.trim()
        val normalized = CurrencyFormatter.normalizeArabicName(cleanName)

        val existingDict = dao.getDictionaryItemByNormalizedName(normalized)
        if (existingDict != null) {
            dao.updateDictionaryItem(
                existingDict.copy(
                    defaultVendorId = vendorId,
                    defaultNote = if (cleanNote.isNotBlank()) cleanNote else existingDict.defaultNote,
                    usageCount = existingDict.usageCount + 1,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertDictionaryItem(
                DictionaryItemEntity(
                    name = cleanName,
                    normalizedName = normalized,
                    defaultVendorId = vendorId,
                    defaultNote = cleanNote,
                    usageCount = 1
                )
            )
        }

        val item = ShoppingItemEntity(
            roundId = roundId,
            vendorId = vendorId,
            name = cleanName,
            note = cleanNote,
            unitPriceDirhams = existingDict?.lastPriceDirhams ?: 0.0
        )
        val newId = dao.insertShoppingItem(item)
        return item.copy(id = newId)
    }

    suspend fun confirmItemPurchase(
        item: ShoppingItemEntity,
        purchaseMode: String,
        unitPriceDirhams: Double,
        quantity: Double,
        cleaningFeeDirhams: Double,
        totalDirhams: Double
    ) {
        val roundedUnit = CurrencyFormatter.roundMoney(unitPriceDirhams)
        val roundedCleaning = CurrencyFormatter.roundMoney(cleaningFeeDirhams)
        val roundedTotal = CurrencyFormatter.roundMoney(totalDirhams)

        val updatedItem = item.copy(
            isPurchased = true,
            purchaseMode = purchaseMode,
            unitPriceDirhams = roundedUnit,
            quantity = quantity,
            cleaningFeeDirhams = roundedCleaning,
            totalDirhams = roundedTotal
        )
        dao.updateShoppingItem(updatedItem)

        val normalized = CurrencyFormatter.normalizeArabicName(item.name)
        val existingDict = dao.getDictionaryItemByNormalizedName(normalized)
        val priceToRemember = if (purchaseMode == "FIXED") roundedTotal else roundedUnit
        if (existingDict != null && priceToRemember > 0.0) {
            dao.updateDictionaryItem(
                existingDict.copy(
                    lastPriceDirhams = priceToRemember,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }

        // Record in PriceHistoryEntity (replacing any previous record for the same shoppingItemId)
        if (priceToRemember > 0.0) {
            dao.deletePriceHistoryForShoppingItem(item.id)
            val vendorName = dao.getVendorById(item.vendorId)?.name ?: "البائع"
            dao.insertPriceHistory(
                PriceHistoryEntity(
                    itemName = item.name.trim(),
                    normalizedName = normalized,
                    vendorId = item.vendorId,
                    vendorName = vendorName,
                    purchaseMode = purchaseMode,
                    unitPriceDirhams = priceToRemember,
                    quantity = quantity,
                    totalPaidDirhams = roundedTotal,
                    shoppingItemId = item.id,
                    recordedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun unmarkItemPurchased(item: ShoppingItemEntity) {
        dao.updateShoppingItem(
            item.copy(
                isPurchased = false,
                totalDirhams = 0.0
            )
        )
        dao.deletePriceHistoryForShoppingItem(item.id)
    }

    suspend fun revertConfirmedPurchase(previousSnapshot: ShoppingItemEntity) {
        dao.updateShoppingItem(previousSnapshot)
        if (!previousSnapshot.isPurchased) {
            dao.deletePriceHistoryForShoppingItem(previousSnapshot.id)
        }
    }

    // --- Price History Manual Management ---
    suspend fun addManualPriceRecord(
        itemName: String,
        vendorId: Long,
        vendorName: String,
        purchaseMode: String,
        unitPriceDirhams: Double
    ): PriceHistoryEntity {
        val cleanName = itemName.trim()
        val normalized = CurrencyFormatter.normalizeArabicName(cleanName)
        val roundedPrice = CurrencyFormatter.roundMoney(unitPriceDirhams)

        val existingDict = dao.getDictionaryItemByNormalizedName(normalized)
        if (existingDict != null) {
            dao.updateDictionaryItem(
                existingDict.copy(
                    lastPriceDirhams = roundedPrice,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }

        val record = PriceHistoryEntity(
            itemName = cleanName,
            normalizedName = normalized,
            vendorId = vendorId,
            vendorName = vendorName,
            purchaseMode = purchaseMode,
            unitPriceDirhams = roundedPrice,
            quantity = 1.0,
            totalPaidDirhams = roundedPrice,
            shoppingItemId = null,
            recordedAt = System.currentTimeMillis()
        )
        val id = dao.insertPriceHistory(record)
        return record.copy(id = id)
    }

    suspend fun deletePriceHistoryRecord(recordId: Long) {
        dao.deletePriceHistoryById(recordId)
    }

    suspend fun restorePriceHistoryRecord(record: PriceHistoryEntity) {
        dao.insertPriceHistory(record)
    }

    suspend fun updateShoppingItem(item: ShoppingItemEntity) {
        dao.updateShoppingItem(item)
        val normalized = CurrencyFormatter.normalizeArabicName(item.name)
        val existingDict = dao.getDictionaryItemByNormalizedName(normalized)
        if (existingDict != null) {
            dao.updateDictionaryItem(
                existingDict.copy(
                    defaultVendorId = item.vendorId,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun softDeleteShoppingItem(itemId: Long) {
        dao.softDeleteShoppingItem(itemId)
    }

    suspend fun restoreShoppingItem(itemId: Long) {
        dao.restoreShoppingItem(itemId)
    }

    // --- Vendor Payment & Pending Change (تأكيد الدفع للبائع والصرف العالق) ---
    suspend fun confirmVendorPayment(
        roundId: Long,
        vendorId: Long,
        calculatedTotalDirhams: Double,
        actualPaidDirhams: Double,
        forgivenDiscountDirhams: Double,
        newVendorPendingBalanceDirhams: Double
    ): VendorPaymentEntity {
        val existing = dao.getVendorPayment(roundId, vendorId)
        val payment = VendorPaymentEntity(
            id = existing?.id ?: 0L,
            roundId = roundId,
            vendorId = vendorId,
            calculatedTotalDirhams = CurrencyFormatter.roundMoney(calculatedTotalDirhams),
            actualPaidDirhams = CurrencyFormatter.roundMoney(actualPaidDirhams),
            forgivenDiscountDirhams = CurrencyFormatter.roundMoney(forgivenDiscountDirhams),
            recordedPendingChangeDirhams = CurrencyFormatter.roundMoney(newVendorPendingBalanceDirhams),
            paidAt = System.currentTimeMillis()
        )
        val id = dao.upsertVendorPayment(payment)
        dao.updateVendorPendingBalance(
            vendorId = vendorId,
            pendingBalanceDirhams = CurrencyFormatter.roundMoney(newVendorPendingBalanceDirhams)
        )
        return payment.copy(id = id)
    }

    suspend fun revertVendorPayment(
        roundId: Long,
        vendorId: Long,
        previousPayment: VendorPaymentEntity?,
        previousVendorPendingBalanceDirhams: Double
    ) {
        if (previousPayment != null) {
            dao.upsertVendorPayment(previousPayment)
        } else {
            dao.deleteVendorPayment(roundId, vendorId)
        }
        dao.updateVendorPendingBalance(vendorId, previousVendorPendingBalanceDirhams)
    }

    suspend fun updateVendorPendingBalance(vendorId: Long, pendingBalanceDirhams: Double) {
        dao.updateVendorPendingBalance(
            vendorId = vendorId,
            pendingBalanceDirhams = CurrencyFormatter.roundMoney(pendingBalanceDirhams)
        )
    }

    // --- Passing Expenses (مصاريف عابرة خارج البائعين) ---
    suspend fun addPassingExpense(roundId: Long, title: String, amountDirhams: Double): PassingExpenseEntity {
        val expense = PassingExpenseEntity(
            roundId = roundId,
            title = title.trim().ifEmpty { "مصروف عابر" },
            amountDirhams = CurrencyFormatter.roundMoney(amountDirhams)
        )
        val id = dao.insertPassingExpense(expense)
        return expense.copy(id = id)
    }

    suspend fun softDeletePassingExpense(expenseId: Long) {
        dao.softDeletePassingExpense(expenseId)
    }

    suspend fun restorePassingExpense(expenseId: Long) {
        dao.restorePassingExpense(expenseId)
    }

    // --- Archive Round & Rollover Unpurchased Items ---
    suspend fun finishAndArchiveCurrentRound(
        currentRound: ShoppingRoundEntity,
        newPocketDirhams: Double,
        newRoundTitle: String,
        rolloverUnpurchasedItems: Boolean,
        unpurchasedItems: List<ShoppingItemEntity>,
        totalSpentDirhams: Double,
        totalSavingsDirhams: Double,
        purchasedItemsCount: Int
    ): ShoppingRoundEntity {
        val now = System.currentTimeMillis()
        val archivedRound = currentRound.copy(
            isActive = false,
            isArchived = true,
            completedAt = now,
            archivedTotalSpentDirhams = CurrencyFormatter.roundMoney(totalSpentDirhams),
            archivedSavingsDirhams = CurrencyFormatter.roundMoney(totalSavingsDirhams),
            archivedItemsCount = purchasedItemsCount
        )
        dao.updateRound(archivedRound)

        val nextRound = ShoppingRoundEntity(
            title = newRoundTitle.trim().ifEmpty { "جولة التقدية الحالية" },
            initialPocketDirhams = CurrencyFormatter.roundMoney(newPocketDirhams),
            createdAt = now,
            isActive = true,
            isArchived = false
        )
        val newRoundId = dao.insertRound(nextRound)

        if (rolloverUnpurchasedItems && unpurchasedItems.isNotEmpty()) {
            val rolledOverItems = unpurchasedItems.map { oldItem ->
                oldItem.copy(
                    id = 0L,
                    roundId = newRoundId,
                    isPurchased = false,
                    totalDirhams = 0.0,
                    createdAt = now,
                    isDeleted = false,
                    deletedAt = null
                )
            }
            dao.insertShoppingItems(rolledOverItems)
        }
        return nextRound.copy(id = newRoundId)
    }

    suspend fun getItemsForRoundOnce(roundId: Long): List<ShoppingItemEntity> {
        return dao.getItemsForRoundOnce(roundId)
    }

    suspend fun deleteArchivedRound(roundId: Long) {
        dao.deleteRoundById(roundId)
    }

    suspend fun restoreArchivedRound(round: ShoppingRoundEntity) {
        dao.insertRound(round)
    }

    suspend fun addVendor(name: String, specialty: String, iconKey: String): VendorEntity {
        val currentVendors = dao.getActiveVendorsOnce()
        val nextSortOrder = (currentVendors.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val vendor = VendorEntity(
            name = name.trim(),
            specialty = specialty.trim(),
            iconKey = iconKey,
            sortOrder = nextSortOrder
        )
        val id = dao.insertVendor(vendor)
        return vendor.copy(id = id)
    }

    suspend fun updateVendor(vendor: VendorEntity) {
        dao.updateVendor(vendor)
    }

    suspend fun moveVendorOrder(vendorId: Long, moveUp: Boolean) {
        val vendors = dao.getActiveVendorsOnce().sortedBy { it.sortOrder }
        val index = vendors.indexOfFirst { it.id == vendorId }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in vendors.indices) return

        val current = vendors[index]
        val neighbor = vendors[targetIndex]
        dao.updateVendor(current.copy(sortOrder = neighbor.sortOrder))
        dao.updateVendor(neighbor.copy(sortOrder = current.sortOrder))
    }

    suspend fun softDeleteVendor(vendorId: Long) {
        dao.softDeleteVendor(vendorId)
    }

    suspend fun restoreVendor(vendorId: Long) {
        dao.restoreVendor(vendorId)
    }

    suspend fun addOrUpdateDictionaryEntry(
        name: String,
        vendorId: Long,
        defaultNote: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val normalized = CurrencyFormatter.normalizeArabicName(cleanName)
        val existing = dao.getDictionaryItemByNormalizedName(normalized)
        if (existing != null) {
            dao.updateDictionaryItem(
                existing.copy(
                    name = cleanName,
                    defaultVendorId = vendorId,
                    defaultNote = defaultNote.trim(),
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertDictionaryItem(
                DictionaryItemEntity(
                    name = cleanName,
                    normalizedName = normalized,
                    defaultVendorId = vendorId,
                    defaultNote = defaultNote.trim(),
                    usageCount = 1
                )
            )
        }
    }

    suspend fun renameDictionaryEntry(
        itemId: Long,
        newName: String,
        newVendorId: Long,
        newDefaultNote: String
    ) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        val normalized = CurrencyFormatter.normalizeArabicName(cleanName)
        dao.renameOrUpdateDictionaryItem(
            itemId = itemId,
            newName = cleanName,
            newNormalizedName = normalized,
            newVendorId = newVendorId,
            newDefaultNote = newDefaultNote
        )
    }

    suspend fun mergeDictionaryEntries(sourceId: Long, targetId: Long) {
        dao.mergeDictionaryItems(sourceId = sourceId, targetId = targetId)
    }

    suspend fun deleteDictionaryEntry(item: DictionaryItemEntity) {
        dao.deleteDictionaryItemById(item.id)
    }

    suspend fun restoreDictionaryEntry(item: DictionaryItemEntity) {
        dao.insertDictionaryItem(item)
    }

    // --- Custom Units per Item (مقادير خاصة لكل سلعة) ---
    suspend fun addCustomUnitForItem(
        itemName: String,
        vendorId: Long,
        customUnit: String
    ) {
        val cleanName = itemName.trim()
        val cleanUnit = customUnit.trim()
        if (cleanName.isEmpty() || cleanUnit.isEmpty()) return
        val normalized = CurrencyFormatter.normalizeArabicName(cleanName)
        val existing = dao.getDictionaryItemByNormalizedName(normalized)
        if (existing != null) {
            val updatedUnits = CurrencyFormatter.parseCustomUnits(existing.customUnitsCsv) + cleanUnit
            dao.updateDictionaryItem(
                existing.copy(
                    customUnitsCsv = CurrencyFormatter.encodeCustomUnits(updatedUnits),
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertDictionaryItem(
                DictionaryItemEntity(
                    name = cleanName,
                    normalizedName = normalized,
                    defaultVendorId = vendorId,
                    defaultNote = cleanUnit,
                    customUnitsCsv = CurrencyFormatter.encodeCustomUnits(listOf(cleanUnit)),
                    usageCount = 1
                )
            )
        }
    }

    suspend fun removeCustomUnitForItem(
        itemName: String,
        customUnit: String
    ) {
        val cleanName = itemName.trim()
        val cleanUnit = customUnit.trim()
        if (cleanName.isEmpty() || cleanUnit.isEmpty()) return
        val normalized = CurrencyFormatter.normalizeArabicName(cleanName)
        val existing = dao.getDictionaryItemByNormalizedName(normalized) ?: return
        val updatedUnits = CurrencyFormatter.parseCustomUnits(existing.customUnitsCsv)
            .filterNot { it == cleanUnit }
        dao.updateDictionaryItem(
            existing.copy(
                customUnitsCsv = CurrencyFormatter.encodeCustomUnits(updatedUnits),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun cancelVendorPaymentAndOptionallyResetBasket(
        roundId: Long,
        vendorId: Long,
        itemsToUnmark: List<ShoppingItemEntity>
    ) {
        dao.deleteVendorPayment(roundId, vendorId)
        for (item in itemsToUnmark) {
            if (item.isPurchased) {
                unmarkItemPurchased(item)
            }
        }
    }

    // --- Batch 5: App Settings & Privacy Preferences ---
    suspend fun getAppSettingsOnce(): AppSettingsEntity {
        return dao.getAppSettingsOnce() ?: AppSettingsEntity().also { dao.upsertAppSettings(it) }
    }

    suspend fun updateAppSettings(transform: (AppSettingsEntity) -> AppSettingsEntity): AppSettingsEntity {
        val current = getAppSettingsOnce()
        val updated = transform(current).copy(id = 1)
        dao.upsertAppSettings(updated)
        return updated
    }

    // --- Batch 5: Trash Bin (سلة المحذوفات) ---
    suspend fun permanentlyDeleteShoppingItem(itemId: Long) {
        dao.deletePriceHistoryForShoppingItem(itemId)
        dao.permanentlyDeleteShoppingItem(itemId)
    }

    suspend fun permanentlyDeletePassingExpense(expenseId: Long) {
        dao.permanentlyDeletePassingExpense(expenseId)
    }

    suspend fun permanentlyDeleteVendor(vendorId: Long) {
        dao.permanentlyDeleteVendor(vendorId)
    }

    suspend fun emptyAllTrash() {
        dao.emptyTrashShoppingItems()
        dao.emptyTrashPassingExpenses()
        dao.emptyTrashVendors()
    }

    // --- Batch 6: Full Backup & Restore (تصدير واستيراد نسخة احتياطية شاملة) ---
    suspend fun exportFullBackupJson(): String {
        val payload = FullBackupPayload(
            exportedAt = System.currentTimeMillis(),
            vendors = dao.getAllVendorsForBackup(),
            rounds = dao.getAllRoundsForBackup(),
            shoppingItems = dao.getAllShoppingItemsForBackup(),
            vendorPayments = dao.getAllVendorPaymentsForBackup(),
            passingExpenses = dao.getAllPassingExpensesForBackup(),
            dictionaryItems = dao.getAllDictionaryItemsForBackup(),
            priceHistory = dao.getAllPriceHistoryForBackup(),
            appSettings = getAppSettingsOnce()
        )
        return BackupManager.encodeToJson(payload)
    }

    suspend fun importFullBackupJson(rawJson: String): Result<FullBackupPayload> {
        val parsedResult = BackupManager.decodeFromJson(rawJson)
        return parsedResult.onSuccess { payload ->
            dao.restoreFullDatabaseBackup(
                vendors = payload.vendors,
                rounds = payload.rounds,
                shoppingItems = payload.shoppingItems,
                vendorPayments = payload.vendorPayments,
                passingExpenses = payload.passingExpenses,
                dictionaryItems = payload.dictionaryItems,
                priceHistory = payload.priceHistory,
                appSettings = payload.appSettings
            )
            // Ensure an active round exists if backup didn't contain one
            ensureInitialized()
        }
    }
}


