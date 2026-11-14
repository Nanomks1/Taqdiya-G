package az.pdr.taqdiya2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "vendors")
data class VendorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val specialty: String = "",
    val iconKey: String = "store",
    val sortOrder: Int = 0,
    // Positive (> 0): "بقا ليا عندو" (Vendor owes me change)
    // Negative (< 0): "بقا ليه عندي" (I owe the vendor money/change)
    val pendingBalanceDirhams: Double = 0.0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "shopping_rounds")
data class ShoppingRoundEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val initialPocketDirhams: Double = 200.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val completedAt: Long? = null,
    val archivedTotalSpentDirhams: Double = 0.0,
    val archivedSavingsDirhams: Double = 0.0,
    val archivedItemsCount: Int = 0
)

@Entity(
    tableName = "shopping_items",
    indices = [Index("roundId"), Index("vendorId")]
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val vendorId: Long,
    val name: String,
    val note: String = "",
    val isPurchased: Boolean = false,
    val purchaseMode: String = "WEIGHT",
    val unitPriceDirhams: Double = 0.0,
    val quantity: Double = 1.0,
    val cleaningFeeDirhams: Double = 0.0,
    val totalDirhams: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "vendor_payments",
    indices = [Index(value = ["roundId", "vendorId"], unique = true)]
)
data class VendorPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val vendorId: Long,
    val calculatedTotalDirhams: Double,
    val actualPaidDirhams: Double,
    val forgivenDiscountDirhams: Double = 0.0,
    val recordedPendingChangeDirhams: Double = 0.0,
    val paidAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "passing_expenses",
    indices = [Index("roundId")]
)
data class PassingExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundId: Long,
    val title: String,
    val amountDirhams: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "dictionary_items",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class DictionaryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val defaultVendorId: Long,
    val defaultNote: String = "",
    val customUnitsCsv: String = "",
    val usageCount: Int = 1,
    val lastPriceDirhams: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "price_history",
    indices = [Index("normalizedName"), Index("shoppingItemId")]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val normalizedName: String,
    val vendorId: Long,
    val vendorName: String,
    val purchaseMode: String = "WEIGHT",
    val unitPriceDirhams: Double,
    val quantity: Double = 1.0,
    val totalPaidDirhams: Double,
    val shoppingItemId: Long? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    // "LIGHT" (نهاري ساطع للسوق), "DARK" (ليلي مريح), "SYSTEM" (تلقائي)
    val themeMode: String = "LIGHT",
    // إظهار الريال بجانب الدرهم (1 درهم = 20 ريال)
    val showRial: Boolean = true,
    // "NORMAL" (100%), "LARGE" (110%), "EXTRA_LARGE" (120%)
    val uiScaleMode: String = "NORMAL",
    // عين الخصوصية: إخفاء المبالغ المالية برمز (***)
    val privacyMaskAmounts: Boolean = false,
    // نطاق عين الخصوصية: true = إخفاء المبلغ في الجيب فقط، false = إخفاء كل المبالغ
    val privacyMaskPocketOnly: Boolean = false,
    // رمز قفل التطبيق السري (4 أرقام، فارغ يعني بدون قفل)
    val pinCode: String = ""
)


