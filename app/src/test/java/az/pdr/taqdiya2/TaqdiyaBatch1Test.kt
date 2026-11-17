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

    @Test
    fun `verify flexible arabic search composable unit modifiers multi vendor and vendor icons`() {
        // 1. Flexible Arabic search ("اتاي" == "أتاي" == "الأتاي", "مطيشه" == "مطيشة", "دروغري" == "دروݣري")
        assertTrue(CurrencyFormatter.matchesArabicQuery("أتاي", "اتاي"))
        assertTrue(CurrencyFormatter.matchesArabicQuery("اتاي", "أتاي"))
        assertTrue(CurrencyFormatter.matchesArabicQuery("أتاي", "الأتاي"))
        assertTrue(CurrencyFormatter.matchesArabicQuery("مطيشة", "مطيشه"))
        assertTrue(CurrencyFormatter.matchesArabicQuery("الدروݣري ومواد النظافة", "دروغري"))

        // 2. Composable unit + size/count modifiers ("علبة" + "كبيرة" + "×2" -> "2 علبة كبيرة")
        var note = CurrencyFormatter.applySmartBaseUnit("", "علبة")
        assertEquals("علبة", note)
        note = CurrencyFormatter.applySmartNoteModifier(note, "كبيرة", "أتاي")
        assertEquals("علبة كبيرة", note)
        note = CurrencyFormatter.applySmartNoteModifier(note, "×2", "أتاي")
        assertEquals("2 علبة كبيرة", note)
        // Switching size from "كبيرة" to "صغيرة" keeps "2 علبة" intact
        note = CurrencyFormatter.applySmartNoteModifier(note, "صغيرة", "أتاي")
        assertEquals("2 علبة صغيرة", note)

        // 3. Multi-vendor CSV encoding and matching (e.g. جافيل in both العطار=1 and الدروݣري=5)
        val csv = CurrencyFormatter.encodeVendorIdsCsv(listOf(1L, 5L), primaryVendorId = 5L)
        assertEquals("1", csv)
        assertTrue(CurrencyFormatter.dictItemMatchesVendor(5L, csv, 5L))
        assertTrue(CurrencyFormatter.dictItemMatchesVendor(5L, csv, 1L))
        assertFalse(CurrencyFormatter.dictItemMatchesVendor(5L, csv, 2L))

        // 4. Vendor icon inference for Moroccan vendors
        assertEquals("nutrition", az.pdr.taqdiya2.ui.components.VendorIconMapper.inferIconKeyFromName("مول الفواكه"))
        assertEquals("set_meal", az.pdr.taqdiya2.ui.components.VendorIconMapper.inferIconKeyFromName("الكزار ومول الدجاج"))
        assertEquals("kitchen", az.pdr.taqdiya2.ui.components.VendorIconMapper.inferIconKeyFromName("مول المواعن"))
        assertEquals("egg", az.pdr.taqdiya2.ui.components.VendorIconMapper.inferIconKeyFromName("مول البيض"))

        // 5. Verify full Arabic Rubik variable font is bundled in assets/fonts/rubik_var.ttf and uses 1.0f default scale
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rubikBytes = context.assets.open("fonts/rubik_var.ttf").use { it.readBytes() }
        assertTrue("rubik_var.ttf must be the full variable font (>200KB)", rubikBytes.size > 200_000)
        val rubikAscii = String(rubikBytes, Charsets.ISO_8859_1)
        assertTrue("rubik_var.ttf must contain fvar table", rubikAscii.contains("fvar"))
        val rubikFamily = az.pdr.taqdiya2.ui.theme.loadAssetFontFamilyOrFallback(context, "RUBIK")
        assertTrue(rubikFamily != androidx.compose.ui.text.font.FontFamily.SansSerif)
        val defaultTypo = az.pdr.taqdiya2.ui.theme.buildAppTypography(rubikFamily, rubikFamily, 0.91f)
        assertEquals(15.47f, defaultTypo.bodyLarge.fontSize.value, 0.01f)
        assertEquals(85, az.pdr.taqdiya2.ui.theme.resolveUiScalePercent("85"))
        assertEquals(100, az.pdr.taqdiya2.ui.theme.resolveUiScalePercent("NORMAL"))
        assertEquals(112, az.pdr.taqdiya2.ui.theme.resolveUiScalePercent("LARGE"))
        assertEquals(122, az.pdr.taqdiya2.ui.theme.resolveUiScalePercent("EXTRA_LARGE"))
        assertEquals(85, az.pdr.taqdiya2.ui.theme.resolveUiScalePercent("70"))
        assertEquals(122, az.pdr.taqdiya2.ui.theme.resolveUiScalePercent("140"))
    }

    @Test
    fun `verify smart note intent parser and price trend unit suffixes`() {
        // 1. Monetary note ("التحميرة" with "5 دراهم") -> FIXED mode + 5.0 DH
        val tahmiraMoney = CurrencyFormatter.parseShoppingNoteIntent("التحميرة", "5 دراهم")
        assertEquals("FIXED", tahmiraMoney.inferredMode)
        assertEquals(5.0, tahmiraMoney.explicitMoneyDirhams ?: 0.0, 0.001)
        assertEquals("", tahmiraMoney.unitSuffix)

        // 2. Monetary note in Rials ("كمون" with "100 ريال") -> FIXED mode + 5.0 DH
        val kamounRials = CurrencyFormatter.parseShoppingNoteIntent("كمون", "100 ريال")
        assertEquals("FIXED", kamounRials.inferredMode)
        assertEquals(5.0, kamounRials.explicitMoneyDirhams ?: 0.0, 0.001)

        // 3. Spice with explicit weight ("التحميرة" with "رابعة") -> WEIGHT mode + 0.25 kg
        val tahmiraWeight = CurrencyFormatter.parseShoppingNoteIntent("التحميرة", "رابعة")
        assertEquals("WEIGHT", tahmiraWeight.inferredMode)
        assertEquals(0.25, tahmiraWeight.explicitWeightKg ?: 0.0, 0.001)
        assertEquals("/كغ", tahmiraWeight.unitSuffix)

        // 4. Container count note ("أتاي" with "2 علبة كبيرة") -> COUNT mode + 2 + "العلبة"
        val atayBox = CurrencyFormatter.parseShoppingNoteIntent("أتاي", "2 علبة كبيرة")
        assertEquals("COUNT", atayBox.inferredMode)
        assertEquals(2, atayBox.explicitCount)
        assertEquals("العلبة", atayBox.unitNoun)
        assertEquals("/علبة", atayBox.unitSuffix)

        // 5. Bottle note ("جافيل" with "قرعة") -> COUNT mode + 1 + "القرعة"
        val javelBottle = CurrencyFormatter.parseShoppingNoteIntent("جافيل", "قرعة")
        assertEquals("COUNT", javelBottle.inferredMode)
        assertEquals(1, javelBottle.explicitCount)
        assertEquals("القرعة", javelBottle.unitNoun)
        assertEquals("/قرعة", javelBottle.unitSuffix)

        // 6. Zucchini ("قرعة خضراء" with "نص كيلو") -> WEIGHT mode + 0.5 kg (not confused with bottle!)
        val zucchini = CurrencyFormatter.parseShoppingNoteIntent("قرعة خضراء", "نص كيلو")
        assertEquals("WEIGHT", zucchini.inferredMode)
        assertEquals(0.5, zucchini.explicitWeightKg ?: 0.0, 0.001)

        // 7. Verify PriceTrendAnalyzer never labels FIXED history as "/كغ"
        val fixedHistory = listOf(
            az.pdr.taqdiya2.data.local.PriceHistoryEntity(
                id = 1L,
                itemName = "التحميرة",
                normalizedName = CurrencyFormatter.normalizeArabicName("التحميرة"),
                vendorId = 1L,
                vendorName = "العطار",
                purchaseMode = "FIXED",
                unitPriceDirhams = 5.0,
                quantity = 1.0,
                totalPaidDirhams = 5.0
            )
        )
        val evalFixed = az.pdr.taqdiya2.util.PriceTrendAnalyzer.evaluateItemPrice(
            currentUnitPriceDirhams = 5.0,
            purchaseMode = "FIXED",
            itemHistory = fixedHistory,
            fallbackLastPriceDirhams = 5.0
        )
        assertFalse("FIXED evaluation must not contain /كغ", evalFixed.recommendationText.contains("/كغ"))

        val evalPreview = az.pdr.taqdiya2.util.PriceTrendAnalyzer.evaluateItemPrice(
            currentUnitPriceDirhams = 0.0,
            purchaseMode = "FIXED",
            itemHistory = fixedHistory,
            fallbackLastPriceDirhams = 5.0
        )
        assertEquals("المعتاد: 5 درهم", evalPreview.badgeLabel)
    }

    @Test
    fun `verify batch 2 custom unit to tab mapping and rule toggles and backup`() {
        // 1. Any monetary amount ("50 درهم", "200 ريال") maps to FIXED when autoLinkMoneyToFixed is enabled
        val fiftyDh = CurrencyFormatter.parseShoppingNoteIntent("تقدية متنوعة", "50 درهم")
        assertEquals("FIXED", fiftyDh.inferredMode)
        assertEquals(50.0, fiftyDh.explicitMoneyDirhams ?: 0.0, 0.001)

        // 2. Disabling autoLinkMoneyToFixed stops automatic money-to-FIXED linking and uses defaultPricingTab
        val disabledMoneySettings = AppSettingsEntity(
            autoLinkMoneyToFixed = false,
            defaultPricingTab = "COUNT"
        )
        val fiftyDhDisabled = CurrencyFormatter.parseShoppingNoteIntent(
            itemName = "تقدية متنوعة",
            note = "50 درهم",
            fallbackPurchaseMode = disabledMoneySettings.defaultPricingTab,
            settings = disabledMoneySettings
        )
        assertEquals(null, fiftyDhDisabled.explicitMoneyDirhams)
        assertEquals("COUNT", fiftyDhDisabled.inferredMode)

        // 3. Adding a custom unit ("طاسة") to COUNT tab maps "2 طاسة" -> COUNT mode with unitNoun "الطاسة"
        val customTabSettings = AppSettingsEntity(
            countUnitsCsv = "${AppSettingsEntity.DEFAULT_COUNT_UNITS_CSV},طاسة",
            weightUnitsCsv = "${AppSettingsEntity.DEFAULT_WEIGHT_UNITS_CSV},قفة كبيرة",
            fixedUnitsCsv = "${AppSettingsEntity.DEFAULT_FIXED_UNITS_CSV},حفنة"
        )
        val tassaIntent = CurrencyFormatter.parseShoppingNoteIntent(
            itemName = "لبن",
            note = "2 طاسة",
            settings = customTabSettings
        )
        assertEquals("COUNT", tassaIntent.inferredMode)
        assertEquals(2, tassaIntent.explicitCount)
        assertEquals("الطاسة", tassaIntent.unitNoun)
        assertEquals("/طاسة", tassaIntent.unitSuffix)

        val quffaWeightIntent = CurrencyFormatter.parseShoppingNoteIntent(
            itemName = "بطاطا",
            note = "قفة كبيرة",
            settings = customTabSettings
        )
        assertEquals("WEIGHT", quffaWeightIntent.inferredMode)

        val hafnaFixedIntent = CurrencyFormatter.parseShoppingNoteIntent(
            itemName = "زريعة",
            note = "حفنة",
            settings = customTabSettings
        )
        assertEquals("FIXED", hafnaFixedIntent.inferredMode)

        // 4. Verify BackupManager preserves all Batch 2 settings
        val payload = FullBackupPayload(
            exportedAt = 1700000000000L,
            vendors = emptyList(),
            rounds = emptyList(),
            shoppingItems = emptyList(),
            vendorPayments = emptyList(),
            passingExpenses = emptyList(),
            dictionaryItems = emptyList(),
            priceHistory = emptyList(),
            appSettings = customTabSettings.copy(
                defaultPricingTab = "COUNT",
                enableQuickFixedBuyButton = false,
                autoKeepScreenAwakeInMarket = false,
                defaultPricingCurrencyInRials = true,
                showCleaningFeeOption = false,
                showPriceTrendBadges = false
            )
        )
        val json = BackupManager.encodeToJson(payload)
        val restored = BackupManager.decodeFromJson(json).getOrThrow().appSettings
        assertEquals("COUNT", restored.defaultPricingTab)
        assertFalse(restored.enableQuickFixedBuyButton)
        assertFalse(restored.autoKeepScreenAwakeInMarket)
        assertTrue(restored.defaultPricingCurrencyInRials)
        assertFalse(restored.showCleaningFeeOption)
        assertFalse(restored.showPriceTrendBadges)
        assertTrue(restored.countUnitsCsv.contains("طاسة"))
        assertTrue(restored.weightUnitsCsv.contains("قفة كبيرة"))
        assertTrue(restored.fixedUnitsCsv.contains("حفنة"))
    }
}

