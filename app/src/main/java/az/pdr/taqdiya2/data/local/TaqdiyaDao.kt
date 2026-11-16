package az.pdr.taqdiya2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaqdiyaDao {

    // --- Vendors ---
    @Query("SELECT * FROM vendors WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    fun observeActiveVendors(): Flow<List<VendorEntity>>

    @Query("SELECT * FROM vendors WHERE isDeleted = 0 ORDER BY sortOrder ASC, id ASC")
    suspend fun getActiveVendorsOnce(): List<VendorEntity>

    @Query("SELECT * FROM vendors WHERE id = :vendorId LIMIT 1")
    suspend fun getVendorById(vendorId: Long): VendorEntity?

    @Query("SELECT COUNT(*) FROM vendors")
    suspend fun getVendorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: VendorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendors(vendors: List<VendorEntity>): List<Long>

    @Update
    suspend fun updateVendor(vendor: VendorEntity)

    @Query("UPDATE vendors SET pendingBalanceDirhams = :pendingBalanceDirhams WHERE id = :vendorId")
    suspend fun updateVendorPendingBalance(vendorId: Long, pendingBalanceDirhams: Double)

    @Query("UPDATE vendors SET isDeleted = 1, deletedAt = :timestamp WHERE id = :vendorId")
    suspend fun softDeleteVendor(vendorId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vendors SET isDeleted = 0, deletedAt = NULL WHERE id = :vendorId")
    suspend fun restoreVendor(vendorId: Long)

    // --- Shopping Rounds ---
    @Query("SELECT * FROM shopping_rounds WHERE isActive = 1 AND isArchived = 0 ORDER BY id DESC LIMIT 1")
    fun observeActiveRound(): Flow<ShoppingRoundEntity?>

    @Query("SELECT * FROM shopping_rounds WHERE isActive = 1 AND isArchived = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveRoundOnce(): ShoppingRoundEntity?

    @Query("SELECT * FROM shopping_rounds WHERE isArchived = 1 ORDER BY completedAt DESC, id DESC")
    fun observeArchivedRounds(): Flow<List<ShoppingRoundEntity>>

    @Query("SELECT * FROM shopping_rounds WHERE id = :roundId LIMIT 1")
    suspend fun getRoundById(roundId: Long): ShoppingRoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: ShoppingRoundEntity): Long

    @Update
    suspend fun updateRound(round: ShoppingRoundEntity)

    @Query("UPDATE shopping_rounds SET initialPocketDirhams = :pocketDirhams WHERE id = :roundId")
    suspend fun updateInitialPocket(roundId: Long, pocketDirhams: Double)

    @Query("DELETE FROM shopping_rounds WHERE id = :roundId")
    suspend fun deleteRoundById(roundId: Long)

    // --- Shopping Items ---
    @Query("SELECT * FROM shopping_items WHERE roundId = :roundId AND isDeleted = 0 ORDER BY isPurchased ASC, createdAt DESC")
    fun observeItemsForRound(roundId: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE isDeleted = 0 ORDER BY isPurchased ASC, createdAt DESC")
    fun observeAllActiveItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE roundId = :roundId AND isDeleted = 0 ORDER BY isPurchased DESC, createdAt ASC")
    suspend fun getItemsForRoundOnce(roundId: Long): List<ShoppingItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItemEntity>): List<Long>

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isDeleted = 1, deletedAt = :timestamp WHERE id = :itemId")
    suspend fun softDeleteShoppingItem(itemId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_items SET isDeleted = 0, deletedAt = NULL WHERE id = :itemId")
    suspend fun restoreShoppingItem(itemId: Long)

    @Query("UPDATE shopping_items SET name = :newName, vendorId = :newVendorId WHERE LOWER(TRIM(name)) = LOWER(TRIM(:oldName))")
    suspend fun updateShoppingItemsNameAndVendor(oldName: String, newName: String, newVendorId: Long)

    // --- Vendor Payments (Confirmed Payments per Round) ---
    @Query("SELECT * FROM vendor_payments ORDER BY paidAt DESC")
    fun observeAllVendorPayments(): Flow<List<VendorPaymentEntity>>

    @Query("SELECT * FROM vendor_payments WHERE roundId = :roundId AND vendorId = :vendorId LIMIT 1")
    suspend fun getVendorPayment(roundId: Long, vendorId: Long): VendorPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVendorPayment(payment: VendorPaymentEntity): Long

    @Query("DELETE FROM vendor_payments WHERE roundId = :roundId AND vendorId = :vendorId")
    suspend fun deleteVendorPayment(roundId: Long, vendorId: Long)

    // --- Passing Expenses (مصاريف عابرة) ---
    @Query("SELECT * FROM passing_expenses WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun observeAllActivePassingExpenses(): Flow<List<PassingExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassingExpense(expense: PassingExpenseEntity): Long

    @Query("UPDATE passing_expenses SET isDeleted = 1, deletedAt = :timestamp WHERE id = :expenseId")
    suspend fun softDeletePassingExpense(expenseId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE passing_expenses SET isDeleted = 0, deletedAt = NULL WHERE id = :expenseId")
    suspend fun restorePassingExpense(expenseId: Long)

    // --- Price History (سجل تتبع الأثمنة ومؤشر الغلاء والرخاء) ---
    @Query("SELECT * FROM price_history ORDER BY recordedAt DESC, id DESC")
    fun observeAllPriceHistory(): Flow<List<PriceHistoryEntity>>

    @Query("SELECT COUNT(*) FROM price_history")
    suspend fun getPriceHistoryCount(): Int

    @Query("SELECT * FROM price_history WHERE shoppingItemId = :shoppingItemId ORDER BY id DESC LIMIT 1")
    suspend fun getPriceHistoryForShoppingItem(shoppingItemId: Long): PriceHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(record: PriceHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistoryList(records: List<PriceHistoryEntity>)

    @Query("DELETE FROM price_history WHERE id = :id")
    suspend fun deletePriceHistoryById(id: Long)

    @Query("DELETE FROM price_history WHERE shoppingItemId = :shoppingItemId")
    suspend fun deletePriceHistoryForShoppingItem(shoppingItemId: Long)

    @Query("UPDATE price_history SET itemName = :newName, normalizedName = :newNormalizedName WHERE normalizedName = :oldNormalizedName")
    suspend fun updatePriceHistoryItemName(oldNormalizedName: String, newName: String, newNormalizedName: String)

    // --- Smart Dictionary ---
    @Query("SELECT * FROM dictionary_items ORDER BY usageCount DESC, lastUpdated DESC")
    fun observeDictionaryItems(): Flow<List<DictionaryItemEntity>>

    @Query("SELECT * FROM dictionary_items WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getDictionaryItemByNormalizedName(normalizedName: String): DictionaryItemEntity?

    @Query("SELECT * FROM dictionary_items WHERE id = :id LIMIT 1")
    suspend fun getDictionaryItemById(id: Long): DictionaryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryItem(item: DictionaryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDictionaryItems(items: List<DictionaryItemEntity>)

    @Update
    suspend fun updateDictionaryItem(item: DictionaryItemEntity)

    @Query("DELETE FROM dictionary_items WHERE id = :id")
    suspend fun deleteDictionaryItemById(id: Long)

    @Transaction
    suspend fun renameOrUpdateDictionaryItem(
        itemId: Long,
        newName: String,
        newNormalizedName: String,
        newVendorId: Long,
        newDefaultNote: String,
        newSecondaryVendorIdsCsv: String = ""
    ) {
        val current = getDictionaryItemById(itemId) ?: return
        val existingCollision = getDictionaryItemByNormalizedName(newNormalizedName)
        if (existingCollision != null && existingCollision.id != itemId) {
            mergeDictionaryItemsInternal(
                source = current,
                target = existingCollision.copy(
                    name = newName.trim(),
                    defaultVendorId = newVendorId,
                    defaultNote = newDefaultNote.trim(),
                    secondaryVendorIdsCsv = newSecondaryVendorIdsCsv
                )
            )
        } else {
            val updated = current.copy(
                name = newName.trim(),
                normalizedName = newNormalizedName,
                defaultVendorId = newVendorId,
                defaultNote = newDefaultNote.trim(),
                secondaryVendorIdsCsv = newSecondaryVendorIdsCsv,
                lastUpdated = System.currentTimeMillis()
            )
            updateDictionaryItem(updated)
            updateShoppingItemsNameAndVendor(
                oldName = current.name,
                newName = newName.trim(),
                newVendorId = newVendorId
            )
            updatePriceHistoryItemName(
                oldNormalizedName = current.normalizedName,
                newName = newName.trim(),
                newNormalizedName = newNormalizedName
            )
        }
    }

    @Transaction
    suspend fun mergeDictionaryItems(sourceId: Long, targetId: Long) {
        if (sourceId == targetId) return
        val source = getDictionaryItemById(sourceId) ?: return
        val target = getDictionaryItemById(targetId) ?: return
        mergeDictionaryItemsInternal(source, target)
    }

    suspend fun mergeDictionaryItemsInternal(
        source: DictionaryItemEntity,
        target: DictionaryItemEntity
    ) {
        val mergedUsage = source.usageCount + target.usageCount
        val mergedPrice = if (target.lastPriceDirhams > 0.0) target.lastPriceDirhams else source.lastPriceDirhams
        val mergedNote = if (target.defaultNote.isNotBlank()) target.defaultNote else source.defaultNote
        val allVendors = (
            listOf(target.defaultVendorId, source.defaultVendorId) +
                source.secondaryVendorIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() } +
                target.secondaryVendorIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            ).filter { it > 0L && it != target.defaultVendorId }.distinct().joinToString(",")

        val updatedTarget = target.copy(
            usageCount = mergedUsage,
            lastPriceDirhams = mergedPrice,
            defaultNote = mergedNote,
            secondaryVendorIdsCsv = allVendors,
            lastUpdated = System.currentTimeMillis()
        )
        updateDictionaryItem(updatedTarget)
        updateShoppingItemsNameAndVendor(
            oldName = source.name,
            newName = target.name,
            newVendorId = target.defaultVendorId
        )
        updatePriceHistoryItemName(
            oldNormalizedName = source.normalizedName,
            newName = target.name,
            newNormalizedName = target.normalizedName
        )
        deleteDictionaryItemById(source.id)
    }

    // --- Batch 5: App Settings & Privacy Preferences ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getAppSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppSettings(settings: AppSettingsEntity)

    // --- Batch 5: Trash Bin (سلة المحذوفات - حفظ 30 يوماً) ---
    @Query("SELECT * FROM shopping_items WHERE isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun observeDeletedShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM passing_expenses WHERE isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun observeDeletedPassingExpenses(): Flow<List<PassingExpenseEntity>>

    @Query("SELECT * FROM vendors WHERE isDeleted = 1 ORDER BY deletedAt DESC, id DESC")
    fun observeDeletedVendors(): Flow<List<VendorEntity>>

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun permanentlyDeleteShoppingItem(itemId: Long)

    @Query("DELETE FROM passing_expenses WHERE id = :expenseId")
    suspend fun permanentlyDeletePassingExpense(expenseId: Long)

    @Query("DELETE FROM vendors WHERE id = :vendorId")
    suspend fun permanentlyDeleteVendor(vendorId: Long)

    @Query("DELETE FROM shopping_items WHERE isDeleted = 1")
    suspend fun emptyTrashShoppingItems()

    @Query("DELETE FROM passing_expenses WHERE isDeleted = 1")
    suspend fun emptyTrashPassingExpenses()

    @Query("DELETE FROM vendors WHERE isDeleted = 1")
    suspend fun emptyTrashVendors()

    @Query("DELETE FROM shopping_items WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrashShoppingItems(cutoffTimestamp: Long)

    @Query("DELETE FROM passing_expenses WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrashPassingExpenses(cutoffTimestamp: Long)

    @Query("DELETE FROM vendors WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrashVendors(cutoffTimestamp: Long)

    // --- Batch 6: Full Backup & Restore (النسخ الاحتياطي الشامل واسترجاع البيانات) ---
    @Query("SELECT * FROM vendors ORDER BY id ASC")
    suspend fun getAllVendorsForBackup(): List<VendorEntity>

    @Query("SELECT * FROM shopping_rounds ORDER BY id ASC")
    suspend fun getAllRoundsForBackup(): List<ShoppingRoundEntity>

    @Query("SELECT * FROM shopping_items ORDER BY id ASC")
    suspend fun getAllShoppingItemsForBackup(): List<ShoppingItemEntity>

    @Query("SELECT * FROM vendor_payments ORDER BY id ASC")
    suspend fun getAllVendorPaymentsForBackup(): List<VendorPaymentEntity>

    @Query("SELECT * FROM passing_expenses ORDER BY id ASC")
    suspend fun getAllPassingExpensesForBackup(): List<PassingExpenseEntity>

    @Query("SELECT * FROM dictionary_items ORDER BY id ASC")
    suspend fun getAllDictionaryItemsForBackup(): List<DictionaryItemEntity>

    @Query("SELECT * FROM price_history ORDER BY id ASC")
    suspend fun getAllPriceHistoryForBackup(): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRounds(rounds: List<ShoppingRoundEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendorPayments(payments: List<VendorPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassingExpenses(expenses: List<PassingExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDictionaryItemsReplace(items: List<DictionaryItemEntity>)

    @Query("DELETE FROM vendors")
    suspend fun clearAllVendors()

    @Query("DELETE FROM shopping_rounds")
    suspend fun clearAllRounds()

    @Query("DELETE FROM shopping_items")
    suspend fun clearAllShoppingItems()

    @Query("DELETE FROM vendor_payments")
    suspend fun clearAllVendorPayments()

    @Query("DELETE FROM passing_expenses")
    suspend fun clearAllPassingExpenses()

    @Query("DELETE FROM dictionary_items")
    suspend fun clearAllDictionaryItems()

    @Query("DELETE FROM price_history")
    suspend fun clearAllPriceHistory()

    @Transaction
    suspend fun restoreFullDatabaseBackup(
        vendors: List<VendorEntity>,
        rounds: List<ShoppingRoundEntity>,
        shoppingItems: List<ShoppingItemEntity>,
        vendorPayments: List<VendorPaymentEntity>,
        passingExpenses: List<PassingExpenseEntity>,
        dictionaryItems: List<DictionaryItemEntity>,
        priceHistory: List<PriceHistoryEntity>,
        appSettings: AppSettingsEntity
    ) {
        clearAllShoppingItems()
        clearAllVendorPayments()
        clearAllPassingExpenses()
        clearAllPriceHistory()
        clearAllDictionaryItems()
        clearAllRounds()
        clearAllVendors()

        if (vendors.isNotEmpty()) insertVendors(vendors)
        if (rounds.isNotEmpty()) insertRounds(rounds)
        if (shoppingItems.isNotEmpty()) insertShoppingItems(shoppingItems)
        if (vendorPayments.isNotEmpty()) insertVendorPayments(vendorPayments)
        if (passingExpenses.isNotEmpty()) insertPassingExpenses(passingExpenses)
        if (dictionaryItems.isNotEmpty()) insertDictionaryItemsReplace(dictionaryItems)
        if (priceHistory.isNotEmpty()) insertPriceHistoryList(priceHistory)
        upsertAppSettings(appSettings.copy(id = 1))
    }
}


