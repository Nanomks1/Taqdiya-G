package az.pdr.taqdiya2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.util.BackupManager
import az.pdr.taqdiya2.util.CurrencyFormatter
import az.pdr.taqdiya2.util.FullBackupPayload
import az.pdr.taqdiya2.util.PinSecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaqdiyaBatch1Test {

    @Test
    fun `verify app name and currency conversions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("التقدية", context.getString(R.string.app_name))
        assertEquals("كناش التقدية", context.getString(R.string.app_full_title))
        assertEquals(350L, CurrencyFormatter.dirhamsToRials(17.5))
        assertEquals(17.5, CurrencyFormatter.rialsToDirhams(350.0), 0.001)
    }

    @Test
    fun `verify pocket only privacy mask hides pocket balance while keeping item prices visible`() {
        CurrencyFormatter.showRialState = true
        CurrencyFormatter.privacyMaskState = true
        CurrencyFormatter.privacyMaskPocketOnlyState = true

        assertEquals("*** درهم", CurrencyFormatter.formatPocketDirhams(188.5))
        assertEquals("*** ريال", CurrencyFormatter.formatPocketRials(188.5))
        assertEquals("15 درهم", CurrencyFormatter.formatDirhams(15.0))
        assertEquals("300 ريال", CurrencyFormatter.formatRials(15.0))

        // Reset states
        CurrencyFormatter.privacyMaskState = false
        CurrencyFormatter.privacyMaskPocketOnlyState = false
    }

    @Test
    fun `verify whatsapp share text formatting`() {
        val vendors = listOf(
            VendorEntity(id = 1L, name = "مول الخضرة", sortOrder = 1)
        )
        val items = listOf(
            ShoppingItemEntity(id = 10L, roundId = 1L, vendorId = 1L, name = "مطيشة", note = "نص كيلو")
        )
        val text = CurrencyFormatter.buildShareText(vendors, items)
        assertTrue(text.contains("مول الخضرة"))
        assertTrue(text.contains("مطيشة (نص كيلو)"))
    }

    @Test
    fun `verify sha256 pin hashing and legacy plain text migration`() {
        val hashed = PinSecurityUtil.hashPin("1234")
        assertTrue(PinSecurityUtil.isHashed(hashed))
        assertTrue(hashed.startsWith("sha256:"))
        assertFalse(hashed.contains("1234"))
        assertTrue(PinSecurityUtil.verifyPin("1234", hashed))
        assertFalse(PinSecurityUtil.verifyPin("9999", hashed))

        // Verify legacy plain-text PIN is automatically normalized to SHA-256 hash
        val migrated = PinSecurityUtil.normalizeForStorage("5678")
        assertEquals(PinSecurityUtil.hashPin("5678"), migrated)
        assertTrue(PinSecurityUtil.verifyPin("5678", migrated))
    }

    @Test
    fun `verify automatic rolling backup and zero default pocket budget`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val defaultRound = ShoppingRoundEntity(title = "جولة التقدية الحالية")
        assertEquals(0.0, defaultRound.initialPocketDirhams, 0.001)

        val payload = FullBackupPayload(
            exportedAt = 1700000000000L,
            vendors = listOf(VendorEntity(id = 1L, name = "العطار / مول الحانوت", sortOrder = 1)),
            rounds = listOf(defaultRound),
            shoppingItems = emptyList(),
            vendorPayments = emptyList(),
            passingExpenses = emptyList(),
            dictionaryItems = emptyList(),
            priceHistory = emptyList(),
            appSettings = AppSettingsEntity(pinCode = "2580", fontFamilyKey = "CUSTOM")
        )
        val json = BackupManager.encodeToJson(payload)
        assertFalse(json.contains("\"pinCode\":\"2580\""))
        assertTrue(json.contains("sha256:"))

        BackupManager.saveAutoBackup(context, json)
        val info = BackupManager.getAutoBackupInfo(context)
        assertTrue(info != null && info.sizeBytes > 0L)
        val restoredRaw = BackupManager.readLatestAutoBackupJson(context)
        assertTrue(!restoredRaw.isNullOrBlank())

        val decoded = BackupManager.decodeFromJson(restoredRaw!!).getOrThrow()
        assertEquals("IBM_PLEX", decoded.appSettings.fontFamilyKey)
        assertTrue(PinSecurityUtil.verifyPin("2580", decoded.appSettings.pinCode))
    }
}
