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
    val completedAt: Long? = null
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
    tableName = "dictionary_items",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class DictionaryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val defaultVendorId: Long,
    val defaultNote: String = "",
    val usageCount: Int = 1,
    val lastPriceDirhams: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
