package az.pdr.taqdiya2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.util.CurrencyFormatter
import org.junit.Assert.assertEquals
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
}
