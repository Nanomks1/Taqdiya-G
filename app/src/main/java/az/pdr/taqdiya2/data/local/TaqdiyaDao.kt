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

    @Query("SELECT COUNT(*) FROM vendors")
    suspend fun getVendorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: VendorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendors(vendors: List<VendorEntity>): List<Long>

    @Update
    suspend fun updateVendor(vendor: VendorEntity)

    @Query("UPDATE vendors SET isDeleted = 1, deletedAt = :timestamp WHERE id = :vendorId")
    suspend fun softDeleteVendor(vendorId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE vendors SET isDeleted = 0, deletedAt = NULL WHERE id = :vendorId")
    suspend fun restoreVendor(vendorId: Long)

    // --- Shopping Rounds ---
    @Query("SELECT * FROM shopping_rounds WHERE isActive = 1 AND isArchived = 0 ORDER BY id DESC LIMIT 1")
    fun observeActiveRound(): Flow<ShoppingRoundEntity?>

    @Query("SELECT * FROM shopping_rounds WHERE isActive = 1 AND isArchived = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveRoundOnce(): ShoppingRoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: ShoppingRoundEntity): Long

    @Update
    suspend fun updateRound(round: ShoppingRoundEntity)

    @Query("UPDATE shopping_rounds SET initialPocketDirhams = :pocketDirhams WHERE id = :roundId")
    suspend fun updateInitialPocket(roundId: Long, pocketDirhams: Double)

    // --- Shopping Items ---
    @Query("SELECT * FROM shopping_items WHERE roundId = :roundId AND isDeleted = 0 ORDER BY isPurchased ASC, createdAt DESC")
    fun observeItemsForRound(roundId: Long): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE isDeleted = 0 ORDER BY isPurchased ASC, createdAt DESC")
    fun observeAllActiveItems(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isDeleted = 1, deletedAt = :timestamp WHERE id = :itemId")
    suspend fun softDeleteShoppingItem(itemId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_items SET isDeleted = 0, deletedAt = NULL WHERE id = :itemId")
    suspend fun restoreShoppingItem(itemId: Long)

    @Query("UPDATE shopping_items SET name = :newName, vendorId = :newVendorId WHERE LOWER(TRIM(name)) = LOWER(TRIM(:oldName))")
    suspend fun updateShoppingItemsNameAndVendor(oldName: String, newName: String, newVendorId: Long)

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
        newDefaultNote: String
    ) {
        val current = getDictionaryItemById(itemId) ?: return
        val existingCollision = getDictionaryItemByNormalizedName(newNormalizedName)
        if (existingCollision != null && existingCollision.id != itemId) {
            // If the new name collides with another dictionary item, merge into it
            mergeDictionaryItemsInternal(source = current, target = existingCollision.copy(
                name = newName.trim(),
                defaultVendorId = newVendorId,
                defaultNote = newDefaultNote.trim()
            ))
        } else {
            val updated = current.copy(
                name = newName.trim(),
                normalizedName = newNormalizedName,
                defaultVendorId = newVendorId,
                defaultNote = newDefaultNote.trim(),
                lastUpdated = System.currentTimeMillis()
            )
            updateDictionaryItem(updated)
            updateShoppingItemsNameAndVendor(
                oldName = current.name,
                newName = newName.trim(),
                newVendorId = newVendorId
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

        val updatedTarget = target.copy(
            usageCount = mergedUsage,
            lastPriceDirhams = mergedPrice,
            defaultNote = mergedNote,
            lastUpdated = System.currentTimeMillis()
        )
        updateDictionaryItem(updatedTarget)
        updateShoppingItemsNameAndVendor(
            oldName = source.name,
            newName = target.name,
            newVendorId = target.defaultVendorId
        )
        deleteDictionaryItemById(source.id)
    }
}
