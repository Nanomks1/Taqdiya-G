package az.pdr.taqdiya2.data.repository

import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.TaqdiyaDao
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.util.CurrencyFormatter
import kotlinx.coroutines.flow.Flow

class TaqdiyaRepository(private val dao: TaqdiyaDao) {

    val activeVendors: Flow<List<VendorEntity>> = dao.observeActiveVendors()
    val activeRound: Flow<ShoppingRoundEntity?> = dao.observeActiveRound()
    val allActiveItems: Flow<List<ShoppingItemEntity>> = dao.observeAllActiveItems()
    val dictionaryItems: Flow<List<DictionaryItemEntity>> = dao.observeDictionaryItems()

    suspend fun ensureInitialized() {
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

            val seedDictionary = listOf(
                // الخضرة
                Triple("مطيشة", khodraId, 12),
                Triple("بطاطا", khodraId, 12),
                Triple("بصلة", khodraId, 11),
                Triple("ربيع ونعناع", khodraId, 10),
                Triple("خيزو", khodraId, 8),
                Triple("فلفلة", khodraId, 7),
                Triple("قرعة خضراء", khodraId, 6),
                // العطار / مول الحانوت
                Triple("لوبيا", attarId, 10),
                Triple("لوبيا بيضاء", attarId, 4),
                Triple("عدس", attarId, 9),
                Triple("حمص", attarId, 8),
                Triple("كمون", attarId, 9),
                Triple("إبزار", attarId, 7),
                Triple("زيت المائدة", attarId, 10),
                Triple("أتاي", attarId, 11),
                Triple("سكر", attarId, 10),
                Triple("دقيق", attarId, 8),
                // الكزار والدجاج والبيض
                Triple("دجاج", gazzarId, 11),
                Triple("بيض", gazzarId, 12),
                Triple("لحم غنمي", gazzarId, 7),
                Triple("كفتة", gazzarId, 8),
                // الفواكه
                Triple("بنان", fawakihId, 9),
                Triple("تفاح", fawakihId, 8),
                Triple("ليمون", fawakihId, 8),
                // الدروݣري والدوزان والمواعن
                Triple("جافيل", drogriId, 8),
                Triple("مسحوق الغسيل", drogriId, 7),
                Triple("صابون", drogriId, 6),
                Triple("براغي", dozanId, 4),
                Triple("كيسان أتاي", mwaenId, 3)
            ).map { (itemName, vId, count) ->
                DictionaryItemEntity(
                    name = itemName,
                    normalizedName = CurrencyFormatter.normalizeArabicName(itemName),
                    defaultVendorId = vId,
                    usageCount = count
                )
            }
            dao.insertDictionaryItems(seedDictionary)
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

        // Save latest reference price in Smart Dictionary
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
    }

    suspend fun unmarkItemPurchased(item: ShoppingItemEntity) {
        dao.updateShoppingItem(
            item.copy(
                isPurchased = false,
                totalDirhams = 0.0
            )
        )
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
}
