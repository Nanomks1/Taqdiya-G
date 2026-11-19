package az.pdr.taqdiya2.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {
    const val RIALS_PER_DIRHAM = 20.0

    var showRialState by mutableStateOf(true)
    var privacyMaskState by mutableStateOf(false)
    var privacyMaskPocketOnlyState by mutableStateOf(false)
    var currentNoteMappingSettings by mutableStateOf(AppSettingsEntity())

    fun dirhamsToRials(dirhams: Double): Long {
        return (dirhams * RIALS_PER_DIRHAM).roundToLong()
    }

    fun rialsToDirhams(rials: Double): Double {
        return ((rials / RIALS_PER_DIRHAM) * 100.0).roundToLong() / 100.0
    }

    fun roundMoney(amount: Double): Double {
        return ((amount * 100.0).roundToLong()) / 100.0
    }

    fun formatAmountPlain(amount: Double): String {
        val rounded = roundMoney(amount)
        return if (abs(rounded - rounded.roundToLong()) < 0.001) {
            String.format(Locale.US, "%d", rounded.roundToLong())
        } else {
            String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
    }

    fun formatPlainNumber(amount: Double): String {
        return formatAmountPlain(amount)
    }

    fun formatDirhams(dirhams: Double, allowMask: Boolean = true): String {
        val shouldMask = allowMask && privacyMaskState && !privacyMaskPocketOnlyState
        if (shouldMask) {
            return "*** درهم"
        }
        return "${formatAmountPlain(dirhams)} درهم"
    }

    fun formatRials(dirhams: Double, allowMask: Boolean = true): String {
        val shouldMask = allowMask && privacyMaskState && !privacyMaskPocketOnlyState
        if (!showRialState) {
            return if (shouldMask) "*** د.م" else "${formatAmountPlain(dirhams)} د.م"
        }
        if (shouldMask) {
            return "*** ريال"
        }
        val rials = dirhamsToRials(dirhams)
        return "$rials ريال"
    }

    fun formatPocketDirhams(dirhams: Double, allowMask: Boolean = true): String {
        if (allowMask && privacyMaskState) {
            return "*** درهم"
        }
        return "${formatAmountPlain(dirhams)} درهم"
    }

    fun formatPocketRials(dirhams: Double, allowMask: Boolean = true): String {
        if (!showRialState) {
            return if (allowMask && privacyMaskState) "*** د.م" else "${formatAmountPlain(dirhams)} د.م"
        }
        if (allowMask && privacyMaskState) {
            return "*** ريال"
        }
        val rials = dirhamsToRials(dirhams)
        return "$rials ريال"
    }

    fun formatItemsCount(count: Int): String {
        val n = abs(count)
        return when {
            n == 0 -> "0 أغراض"
            n == 1 -> "غرض واحد"
            n == 2 -> "غرضان"
            n % 100 in 3..10 -> "$n أغراض"
            n % 100 == 0 -> "$n غرض"
            else -> "$n غرضاً"
        }
    }

    fun formatRegisteredItemsCount(count: Int): String {
        val n = abs(count)
        return when {
            n == 0 -> "لا توجد أغراض مسجلة"
            n == 1 -> "غرض واحد مسجل"
            n == 2 -> "غرضان مسجلان"
            n % 100 in 3..10 -> "$n أغراض مسجلة"
            n % 100 == 0 -> "$n غرض مسجل"
            else -> "$n غرضاً مسجلاً"
        }
    }

    fun formatPurchasedItemsCount(count: Int): String {
        val n = abs(count)
        return when {
            n == 0 -> "0 أغراض مشتراة"
            n == 1 -> "غرض واحد مشترى"
            n == 2 -> "غرضان مشتريان"
            n % 100 in 3..10 -> "$n أغراض مشتراة"
            n % 100 == 0 -> "$n غرض مشترى"
            else -> "$n غرضاً مشتراً"
        }
    }

    fun formatMoroccanWeightLabel(kg: Double): String {
        return when {
            abs(kg - 1.0) < 0.001 -> "كيلو (1 كغ)"
            abs(kg - 0.5) < 0.001 -> "نص كيلو (0.5 كغ)"
            abs(kg - 0.75) < 0.001 -> "3 رابعات (0.75 كغ)"
            abs(kg - 0.25) < 0.001 -> "رابعة (0.25 كغ)"
            abs(kg - 0.125) < 0.001 -> "نص رابعة (0.125 كغ)"
            abs(kg - 2.0) < 0.001 -> "2 كيلو"
            kg < 1.0 -> "${(kg * 1000).roundToLong()} غرام"
            else -> "${formatAmountPlain(kg)} كغ"
        }
    }

    fun formatPurchaseDetails(item: ShoppingItemEntity): String {
        val intent = parseShoppingNoteIntent(item.name, item.note, item.purchaseMode)
        return when (item.purchaseMode) {
            "WEIGHT" -> {
                val weightText = formatMoroccanWeightLabel(item.quantity)
                val baseText = "$weightText × ${formatDirhams(item.unitPriceDirhams)}/كغ"
                if (item.cleaningFeeDirhams > 0.0) {
                    "$baseText (+ ${formatDirhams(item.cleaningFeeDirhams)} تنقية)"
                } else {
                    baseText
                }
            }
            "FIXED" -> {
                if (item.note.isNotBlank()) {
                    "مبلغ مقطوع (${item.note.trim()})"
                } else {
                    "شراء بمبلغ مقطوع مباشر"
                }
            }
            "COUNT" -> {
                val countInt = item.quantity.roundToLong()
                val singularNoun = intent.unitNoun.removePrefix("ال")
                val countLabel = when {
                    countInt == 30L && (item.name.contains("بيض") || item.note.contains("بلاطو")) -> "بلاطو (30 بيضة)"
                    countInt == 15L && (item.name.contains("بيض") || item.note.contains("بلاطو")) -> "نص بلاطو (15 بيضة)"
                    singularNoun != "حبة/القطعة" && singularNoun.isNotBlank() -> {
                        when (countInt) {
                            1L -> "$singularNoun واحدة"
                            2L -> "2 $singularNoun"
                            else -> "$countInt ${intent.unitPluralLabel}"
                        }
                    }
                    countInt == 1L -> "قطعة واحدة"
                    countInt == 2L -> "قطعتان (2)"
                    else -> "$countInt قطع/حبات"
                }
                "$countLabel × ${formatDirhams(item.unitPriceDirhams)}"
            }
            else -> ""
        }
    }

    data class ShoppingNoteIntent(
        val inferredMode: String,
        val explicitMoneyDirhams: Double? = null,
        val explicitWeightKg: Double? = null,
        val explicitCount: Int? = null,
        val unitNoun: String = "الحبة/القطعة",
        val unitPluralLabel: String = "قطع/حبات",
        val unitSuffix: String = "/حبة"
    )

    private val DIRHAM_NOTE_REGEX = Regex(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:دراهم|درهم|د\\.م|دم|dh|dhs)",
        RegexOption.IGNORE_CASE
    )
    private val RIAL_NOTE_REGEX = Regex(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:ريال|أريال|اريال)"
    )
    private val KILO_NOTE_REGEX = Regex(
        "(\\d+(?:[.,]\\d+)?)\\s*(?:كيلو|كغ|kg)",
        RegexOption.IGNORE_CASE
    )
    private val GRAM_NOTE_REGEX = Regex(
        "(\\d+)\\s*(?:غرام|غ|g)(?:\\b|$)",
        RegexOption.IGNORE_CASE
    )
    private val EXPLICIT_EGG_COUNT_REGEX = Regex(
        "(\\d+)\\s*(?:بيضات|بيضة|بيضه|حبات|حبة|حبه|قطع|قطعة|قطعه)"
    )
    private val LEADING_COUNT_REGEX = Regex("^(?:[×x]?\\s*(\\d+)|(\\d+)\\s*[×x]?)\\b")

    private fun noteContainsConfiguredUnit(normNote: String, rawUnit: String): Boolean {
        val normUnit = normalizeArabicName(rawUnit)
        if (normUnit.isEmpty() || normNote.isEmpty()) return false
        if (normNote == normUnit) return true
        val tokens = normNote.split(" ")
        val unitTokens = normUnit.split(" ")
        if (unitTokens.size == 1) {
            val u = unitTokens[0]
            return tokens.any { token ->
                token == u ||
                    token == "ال$u" ||
                    (u == "علبه" && (token == "علب" || token == "علبتين")) ||
                    (u == "قرعه" && (token == "قراعي" || token == "قرعتين")) ||
                    (u == "باكيه" && (token == "باكيات" || token == "باكيتين")) ||
                    (u == "خنيشه" && (token == "خنيشات" || token == "خنيشتين")) ||
                    (u == "حبه" && (token == "حبات" || token == "حبتين")) ||
                    (u == "قطعه" && (token == "قطع" || token == "قطعتين")) ||
                    (u == "ربطه" && (token == "ربطات" || token == "ربطتين"))
            }
        }
        return normNote.contains(normUnit)
    }

    private fun resolveCountNounAndPlural(matchedUnitRaw: String, normNote: String, isEggItem: Boolean): Triple<String, String, String> {
        val normU = normalizeArabicName(matchedUnitRaw)
        return when {
            normU.contains("علبه") || normU.contains("علب") -> Triple("العلبة", "علب", "/علبة")
            normU.contains("قرعه") || normU.contains("قراعي") || normNote.contains("لتر") -> Triple("القرعة", "قراعي", "/قرعة")
            normU.contains("باكيه") || normU.contains("باكيات") -> Triple("الباكية", "باكيات", "/باكية")
            normU.contains("خنيشه") || normU.contains("خنيشات") -> Triple("الخنيشة", "خنيشات", "/خنيشة")
            normU.contains("كيس") -> Triple("الكيس", "أكياس", "/كيس")
            normU.contains("حك") -> Triple("الحك", "حكوك", "/حك")
            normU.contains("قنينه") -> Triple("القنينة", "قنينات", "/قنينة")
            normU.contains("شبكه") -> Triple("الشبكة", "شبكات", "/شبكة")
            normU.contains("طقم") -> Triple("الطقم", "أطقم", "/طقم")
            isEggItem || normU.contains("بلاطو") || normU.contains("بيض") -> Triple("البيضة", "بيضات", "/بيضة")
            normU.contains("حبه") || normU.contains("قطعه") || normU.isBlank() -> Triple("الحبة/القطعة", "قطع/حبات", "/حبة")
            else -> {
                val clean = matchedUnitRaw.trim().removePrefix("ال")
                Triple("ال$clean", clean, "/$clean")
            }
        }
    }

    private fun extractExplicitCountFromNote(cleanNote: String, normNote: String, hasEggTray: Boolean): Int {
        return when {
            normNote.contains("نص بلاطو") || normNote.contains("نصف بلاطو") -> 15
            hasEggTray -> 30
            else -> {
                val eggMatch = EXPLICIT_EGG_COUNT_REGEX.find(cleanNote)
                val leadingMatch = LEADING_COUNT_REGEX.find(cleanNote)
                val leadVal = leadingMatch?.let { m ->
                    (m.groupValues[1].ifEmpty { m.groupValues[2] }).toIntOrNull()
                }
                when {
                    eggMatch != null -> eggMatch.groupValues[1].toIntOrNull() ?: 1
                    leadVal != null && leadVal > 0 -> leadVal
                    normNote.contains("علبتين") || normNote.contains("قرعتين") ||
                        normNote.contains("باكيتين") || normNote.contains("حبتين") ||
                        normNote.contains("ربطتين") -> 2
                    else -> 1
                }
            }
        }
    }

    /**
     * Analyzes the item's name and note to infer the best calculator purchaseMode
     * ("WEIGHT", "FIXED", or "COUNT") and extract any explicit monetary amount, weight, or count.
     */
    fun parseShoppingNoteIntent(
        itemName: String,
        note: String,
        fallbackPurchaseMode: String = "WEIGHT",
        settings: AppSettingsEntity = currentNoteMappingSettings
    ): ShoppingNoteIntent {
        val cleanNote = note.trim()
        val normNote = normalizeArabicName(cleanNote)
        val normName = normalizeArabicName(itemName)

        val weightUnits = parseCustomUnits(settings.weightUnitsCsv)
        val fixedUnits = parseCustomUnits(settings.fixedUnitsCsv)
        val countUnits = parseCustomUnits(settings.countUnitsCsv)

        val safeDefaultTab = when (settings.defaultPricingTab) {
            "FIXED", "COUNT", "WEIGHT" -> settings.defaultPricingTab
            else -> "WEIGHT"
        }

        // 1. Check for explicit monetary amount in note (e.g. "5 دراهم", "50 درهم", "100 ريال")
        if (cleanNote.isNotEmpty() && settings.autoLinkMoneyToFixed) {
            val dhMatch = DIRHAM_NOTE_REGEX.find(cleanNote)
            if (dhMatch != null) {
                val dhVal = dhMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
                if (dhVal != null && dhVal > 0.0) {
                    return ShoppingNoteIntent(
                        inferredMode = "FIXED",
                        explicitMoneyDirhams = roundMoney(dhVal),
                        unitNoun = "المبلغ",
                        unitPluralLabel = "",
                        unitSuffix = ""
                    )
                }
            }
            val rialMatch = RIAL_NOTE_REGEX.find(cleanNote)
            if (rialMatch != null) {
                val rialVal = rialMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
                if (rialVal != null && rialVal > 0.0) {
                    return ShoppingNoteIntent(
                        inferredMode = "FIXED",
                        explicitMoneyDirhams = rialsToDirhams(rialVal),
                        unitNoun = "المبلغ",
                        unitPluralLabel = "",
                        unitSuffix = ""
                    )
                }
            }
            if (normNote.contains("درهمين") || normNote.contains("جوج دراهم")) {
                return ShoppingNoteIntent(
                    inferredMode = "FIXED",
                    explicitMoneyDirhams = 2.0,
                    unitNoun = "المبلغ",
                    unitPluralLabel = "",
                    unitSuffix = ""
                )
            }
            val matchedFixedUnit = fixedUnits
                .sortedByDescending { it.length }
                .firstOrNull { noteContainsConfiguredUnit(normNote, it) }
            if (matchedFixedUnit != null) {
                return ShoppingNoteIntent(
                    inferredMode = "FIXED",
                    unitNoun = "المبلغ",
                    unitPluralLabel = "",
                    unitSuffix = ""
                )
            }
        }

        // 2. Check for explicit Weight in note (e.g., "نص كيلو", "رابعة", "نص رابعة", "3 رابعات", "2 كيلو", "250غ")
        if (cleanNote.isNotEmpty() && settings.autoLinkWeightToWeight) {
            val matchedWeightUnit = weightUnits
                .sortedByDescending { it.length }
                .firstOrNull { noteContainsConfiguredUnit(normNote, it) }
            val hasContainerInCount = countUnits.any { noteContainsConfiguredUnit(normNote, it) }

            val weightFromNote: Double? = when {
                (normNote.contains("نص رابعه") || normNote.contains("نصف رابعه")) &&
                    (matchedWeightUnit != null || weightUnits.any { normalizeArabicName(it).contains("رابعه") }) -> 0.125
                (normNote.contains("3 رابعات") || normNote.contains("ثلاث رابعات")) &&
                    (matchedWeightUnit != null || weightUnits.any { normalizeArabicName(it).contains("رابعه") }) -> 0.75
                normNote.contains("رابعه") &&
                    weightUnits.any { normalizeArabicName(it).contains("رابعه") } -> 0.25
                (normNote.contains("نص كيلو") || normNote.contains("نصف كيلو")) &&
                    weightUnits.any { normalizeArabicName(it).contains("كيلو") } -> 0.5
                else -> {
                    val kiloMatch = KILO_NOTE_REGEX.find(cleanNote)
                    val gramMatch = GRAM_NOTE_REGEX.find(cleanNote)
                    when {
                        kiloMatch != null && weightUnits.any { normalizeArabicName(it).contains("كيلو") } ->
                            kiloMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
                        normNote.contains("كيلو") && weightUnits.any { normalizeArabicName(it).contains("كيلو") } -> 1.0
                        gramMatch != null && !hasContainerInCount &&
                            weightUnits.any { normalizeArabicName(it).contains("غرام") } -> {
                            gramMatch.groupValues[1].toDoubleOrNull()?.let { it / 1000.0 }
                        }
                        matchedWeightUnit != null -> {
                            val leadingMatch = LEADING_COUNT_REGEX.find(cleanNote)
                            val leadVal = leadingMatch?.let { m ->
                                (m.groupValues[1].ifEmpty { m.groupValues[2] }).toDoubleOrNull()
                            }
                            leadVal ?: 1.0
                        }
                        else -> null
                    }
                }
            }
            if (weightFromNote != null && weightFromNote > 0.0) {
                return ShoppingNoteIntent(
                    inferredMode = "WEIGHT",
                    explicitWeightKg = weightFromNote,
                    unitNoun = "الكيلو",
                    unitPluralLabel = "كغ",
                    unitSuffix = "/كغ"
                )
            }
        }

        // 3. Check for Count / Container / Tray in note or item name
        if (settings.autoLinkCountToCount) {
            val matchedCountUnit = countUnits
                .sortedByDescending { it.length }
                .firstOrNull { noteContainsConfiguredUnit(normNote, it) }

            val hasEggTray = (normNote.contains("بلاطو") || normName.contains("بلاطو")) &&
                countUnits.any { normalizeArabicName(it).contains("بلاطو") }
            val isEggItem = normName.contains("بيض") || normNote.contains("بيض")
            val hasBottleInName = normName.contains("قرعه") &&
                !normName.contains("خضراء") && !normName.contains("حمراء") && !normName.contains("سلوي") &&
                countUnits.any { normalizeArabicName(it).contains("قرعه") }
            val hasLiterBottle = (normNote.contains("لتر") || normNote.contains("يترو")) &&
                (normName.contains("زيت") || normName.contains("حليب") || normName.contains("جافيل") ||
                    normName.contains("ماء") || normName.contains("عصير") || normName.contains("سائل"))

            if (matchedCountUnit != null || hasEggTray || (isEggItem && cleanNote.isNotEmpty()) || hasBottleInName || hasLiterBottle) {
                val effectiveUnitRaw = matchedCountUnit ?: when {
                    hasBottleInName || hasLiterBottle -> "قرعة"
                    hasEggTray || isEggItem -> "بيضة"
                    else -> "حبة"
                }
                val (unitNoun, unitPlural, unitSuffix) = resolveCountNounAndPlural(effectiveUnitRaw, normNote, isEggItem || hasEggTray)
                val explicitCount = extractExplicitCountFromNote(cleanNote, normNote, hasEggTray)

                return ShoppingNoteIntent(
                    inferredMode = "COUNT",
                    explicitCount = explicitCount,
                    unitNoun = unitNoun,
                    unitPluralLabel = unitPlural,
                    unitSuffix = unitSuffix
                )
            }
        }

        // 4. Check for bundle / herbs / spices defaulting to FIXED when no weight/count is specified
        if (settings.autoLinkMoneyToFixed) {
            val isHerbItem = (normName.contains("ربيع") || normName.contains("نعناع") || normName.contains("معدنوس") ||
                normName.contains("قزبور") || normName.contains("كرافس") || normName.contains("شيبه")) &&
                cleanNote.isEmpty()
            val isSpiceDefaultFixed = (normName.contains("كمون") || normName.contains("ابزار") ||
                normName.contains("تحميره") || normName.contains("خرقوم") || normName.contains("سكنجبير") ||
                normName.contains("راس الحانوت") || normName.contains("عطريه")) &&
                cleanNote.isEmpty()

            if (isHerbItem || isSpiceDefaultFixed) {
                return ShoppingNoteIntent(
                    inferredMode = "FIXED",
                    unitNoun = "المبلغ",
                    unitPluralLabel = "",
                    unitSuffix = ""
                )
            }
        }

        val effectiveFallbackMode = if (cleanNote.isEmpty()) {
            safeDefaultTab
        } else {
            when (fallbackPurchaseMode) {
                "FIXED", "COUNT", "WEIGHT" -> fallbackPurchaseMode
                else -> safeDefaultTab
            }
        }

        if (effectiveFallbackMode == "FIXED") {
            return ShoppingNoteIntent(
                inferredMode = "FIXED",
                unitNoun = "المبلغ",
                unitPluralLabel = "",
                unitSuffix = ""
            )
        }

        if (effectiveFallbackMode == "COUNT") {
            return ShoppingNoteIntent(
                inferredMode = "COUNT",
                explicitCount = 1,
                unitNoun = "الحبة/القطعة",
                unitPluralLabel = "قطع/حبات",
                unitSuffix = "/حبة"
            )
        }

        return ShoppingNoteIntent(
            inferredMode = "WEIGHT",
            unitNoun = "الكيلو",
            unitPluralLabel = "كغ",
            unitSuffix = "/كغ"
        )
    }

    private val ARABIC_DIACRITICS_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Normalizes Arabic item names so orthographic variations ("أتاي" vs "اتاي",
     * "إبزار" vs "ابزار", "مطيشة" vs "مطيشه", "لاڤوكا" vs "لافوكا", "دروݣري" vs "دروغري")
     * resolve to the same canonical form.
     */
    fun normalizeArabicName(input: String): String {
        return input.trim()
            .replace(ARABIC_DIACRITICS_REGEX, "")
            .replace(MULTI_SPACE_REGEX, " ")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .replace('ڤ', 'ف')
            .replace('گ', 'ك')
            .replace('ݣ', 'ك')
            .replace('ڨ', 'ك')
            .lowercase(Locale.ROOT)
    }

    private fun stripArabicDefiniteArticle(normalized: String): String {
        return normalized
            .split(" ")
            .joinToString(" ") { word ->
                if (word.length > 3 && word.startsWith("ال")) {
                    word.substring(2)
                } else {
                    word
                }
            }
    }

    /**
     * Flexible Arabic search matcher:
     * Matches "اتاي" with "أتاي" or "الأتاي", "مطيشه" with "مطيشة", "دروغري" with "دروݣري", etc.
     */
    fun matchesArabicQuery(target: String, query: String): Boolean {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return true
        val normTarget = normalizeArabicName(target)
        val normQuery = normalizeArabicName(cleanQuery)
        if (normTarget.contains(normQuery)) return true

        val strippedTarget = stripArabicDefiniteArticle(normTarget)
        val strippedQuery = stripArabicDefiniteArticle(normQuery)
        if (strippedTarget.contains(strippedQuery)) return true

        // Also handle Moroccan Gaf/Ghain variation ("دروغري" vs "دروكري")
        val ghainUnifiedTarget = strippedTarget.replace('غ', 'ك')
        val ghainUnifiedQuery = strippedQuery.replace('غ', 'ك')
        return ghainUnifiedTarget.contains(ghainUnifiedQuery)
    }

    // --- Multi-Vendor CSV Helpers for Dictionary Items ---
    fun parseVendorIdsCsv(csv: String): List<Long> {
        if (csv.isBlank()) return emptyList()
        return csv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .distinct()
    }

    fun encodeVendorIdsCsv(vendorIds: Collection<Long>, primaryVendorId: Long): String {
        return vendorIds
            .filter { it > 0L && it != primaryVendorId }
            .distinct()
            .joinToString(",")
    }

    fun getAllVendorIdsForDictItem(defaultVendorId: Long, secondaryVendorIdsCsv: String): List<Long> {
        return (listOf(defaultVendorId) + parseVendorIdsCsv(secondaryVendorIdsCsv)).distinct()
    }

    fun dictItemMatchesVendor(
        defaultVendorId: Long,
        secondaryVendorIdsCsv: String,
        vendorId: Long?
    ): Boolean {
        if (vendorId == null) return true
        if (defaultVendorId == vendorId) return true
        return parseVendorIdsCsv(secondaryVendorIdsCsv).contains(vendorId)
    }

    fun parseCustomUnits(csv: String): List<String> {
        if (csv.isBlank()) return emptyList()
        return csv.split('|', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun encodeCustomUnits(units: List<String>): String {
        return units.map { it.trim().replace("|", "").replace(",", "") }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("|")
    }

    /**
     * Returns smart base units tailored to the specific item name and vendor specialty,
     * preceded by any user-defined custom units saved for this item.
     */
    fun getSmartUnitsForItem(
        itemName: String,
        vendor: VendorEntity?,
        customUnitsCsv: String = ""
    ): List<String> {
        val custom = parseCustomUnits(customUnitsCsv)
        val norm = normalizeArabicName(itemName)
        val vendorNorm = normalizeArabicName(vendor?.name.orEmpty() + " " + vendor?.specialty.orEmpty())
        val iconKey = vendor?.iconKey.orEmpty()

        val smartDefaults: List<String> = when {
            // 1. Eggs
            norm.contains("بيض") -> listOf(
                "بلاطو", "نص بلاطو", "10 بيضات", "5 بيضات", "15 بيضة", "بلدي"
            )
            // 2. Cleaning & Liquids (Javel, soap, oil, milk, water, detergent)
            norm.contains("جافيل") || norm.contains("زيت") || norm.contains("حليب") ||
                norm.contains("ماء") || norm.contains("خل") || norm.contains("عصير") ||
                norm.contains("سائل") -> listOf(
                "قرعة", "لتر", "2 لتر", "5 لتر", "علبة", "خنيشة"
            )
            // 3. Tea, Coffee, Packaged Grocery, Detergent Powder, Cheese, Butter
            norm.contains("اتاي") || norm.contains("قهوه") || norm.contains("طون") ||
                norm.contains("فرماج") || norm.contains("جبن") || norm.contains("زبده") ||
                norm.contains("ياغورت") || norm.contains("مسحوق") || norm.contains("صابون") ||
                norm.contains("خميره") || norm.contains("بسكويت") || norm.contains("معجون") -> listOf(
                "علبة", "باكية", "خنيشة", "حبة", "رابعة", "نص كيلو", "كيلو"
            )
            // 4. Herbs & Greens (Rbi3, Na3na3, Karafs, Khoss)
            norm.contains("ربيع") || norm.contains("نعناع") || norm.contains("معدنوس") ||
                norm.contains("قزبور") || norm.contains("كرافس") || norm.contains("خس") ||
                norm.contains("شيبه") -> listOf(
                "ربطة", "2 ربطات", "3 دراهم", "5 دراهم", "طري"
            )
            // 5. Spices (Kamoun, Ibzar, Tahmira, Kharqoum, Skinjbir)
            norm.contains("كمون") || norm.contains("ابزار") || norm.contains("تحميره") ||
                norm.contains("خرقوم") || norm.contains("سكنجبير") || norm.contains("قرفه") ||
                norm.contains("راس الحانوت") || norm.contains("عطريه") -> listOf(
                "5 دراهم", "10 دراهم", "رابعة", "نص رابعة", "مطحون", "حبوب"
            )
            // 6. Cleaning / Droguerie Vendor
            iconKey == "cleaning" || vendorNorm.contains("درو") || vendorNorm.contains("نظافه") -> listOf(
                "قرعة", "علبة", "باكية", "لتر", "2 لتر", "خنيشة", "حبة"
            )
            // 7. Hardware / Dozan / Mwa3en / Clothes Vendor
            iconKey == "handyman" || iconKey == "kitchen" || iconKey == "checkroom" ||
                vendorNorm.contains("دوزان") || vendorNorm.contains("مواعن") || vendorNorm.contains("حوايج") -> listOf(
                "حبة", "علبة", "باكية", "طقم"
            )
            // 8. Butcher / Chicken / Meat
            iconKey == "set_meal" || vendorNorm.contains("كزار") || vendorNorm.contains("دجاج") ||
                norm.contains("دجاج") || norm.contains("لحم") || norm.contains("كفته") -> listOf(
                "كيلو", "نص كيلو", "رابعة", "2 كيلو", "دجاجة متوسطة", "مطحون"
            )
            // 9. Attar / Grocery (General: Legumes, Flour, Sugar + Packaged goods)
            iconKey == "store" || vendorNorm.contains("عطار") || vendorNorm.contains("حانوت") -> listOf(
                "علبة", "قرعة", "باكية", "كيلو", "نص كيلو", "رابعة", "2 كيلو", "خنيشة", "5 دراهم"
            )
            // 10. Vegetables & Fruits (Default Weight & Freshness notes)
            else -> listOf(
                "كيلو", "نص كيلو", "2 كيلو", "رابعة", "3 رابعات", "3 كيلو", "ربطة", "طايبة"
            )
        }

        return (custom + smartDefaults).distinct()
    }

    /**
     * Returns composable size/count/type modifiers (such as صغيرة, متوسطة, كبيرة, ×2, ×3, شعرة, مكركب)
     * that combine with the base unit (e.g., "علبة" + "كبيرة" -> "علبة كبيرة", + "×2" -> "2 علبة كبيرة")
     * instead of overwriting it.
     */
    fun getSmartUnitModifiersForItem(itemName: String): List<String> {
        val norm = normalizeArabicName(itemName)
        return when {
            norm.contains("اتاي") -> listOf(
                "صغيرة", "متوسطة", "كبيرة", "×2", "×3", "شعرة", "مكركب", "200غ", "500غ"
            )
            norm.contains("جافيل") || norm.contains("زيت") || norm.contains("حليب") ||
                norm.contains("ماء") || norm.contains("سائل") || norm.contains("عصير") -> listOf(
                "صغيرة", "متوسطة", "كبيرة", "×2", "×3", "1 لتر", "2 لتر", "5 لتر"
            )
            norm.contains("بيض") -> listOf(
                "بلدي", "رومي", "كبير", "×2"
            )
            else -> listOf(
                "صغيرة", "متوسطة", "كبيرة", "×2", "×3", "×4"
            )
        }
    }

    private val SIZE_MODIFIERS = setOf(
        "صغيرة", "صغير", "متوسطة", "متوسط", "كبيرة", "كبير", "حجم صغير", "حجم متوسط", "حجم كبير"
    )
    private val TEA_TYPE_MODIFIERS = setOf("شعرة", "مكركب", "حبوب")
    private val WEIGHT_PACK_MODIFIERS = setOf("100غ", "200غ", "250غ", "500غ", "1 لتر", "2 لتر", "5 لتر")
    private val COUNT_PREFIX_REGEX = Regex("^(?:(\\d+)\\s*[×x]?\\s*)(.+)$")

    /**
     * Checks if a given modifier chip (e.g. "صغيرة", "كبيرة", "×2", "شعرة") is currently active in [note].
     */
    fun isNoteModifierActive(note: String, modifierChip: String): Boolean {
        val clean = note.trim()
        if (clean.isEmpty()) return false
        if (modifierChip.startsWith("×")) {
            val num = modifierChip.removePrefix("×").trim()
            val match = COUNT_PREFIX_REGEX.find(clean)
            return match?.groupValues?.getOrNull(1) == num
        }
        val tokens = clean.split(MULTI_SPACE_REGEX)
        return tokens.contains(modifierChip)
    }

    /**
     * Applies or toggles a base unit (like "علبة", "قرعة", "كيلو") while preserving any chosen
     * size/count modifiers if appropriate, or toggling off if the exact base unit was already the whole note.
     */
    fun applySmartBaseUnit(currentNote: String, chosenBaseUnit: String): String {
        val cleanCurrent = currentNote.trim()
        if (cleanCurrent == chosenBaseUnit) {
            return ""
        }
        // Keep active size/count modifiers if switching between container units (e.g. علبة -> باكية or قرعة)
        val containerUnits = setOf("علبة", "باكية", "باكيه", "قرعة", "خنيشة", "حبة", "ربطة", "طقم")
        if (chosenBaseUnit !in containerUnits) {
            return chosenBaseUnit
        }

        var countPrefix: String? = null
        var remainder = cleanCurrent
        val countMatch = COUNT_PREFIX_REGEX.find(remainder)
        if (countMatch != null && remainder.none { it == 'د' }) { // not "5 دراهم"
            val firstToken = remainder.substringBefore(" ")
            if (firstToken.all { it.isDigit() } && remainder.substringAfter(" ") in containerUnits ||
                containerUnits.any { remainder.substringAfter(" ").startsWith(it) }
            ) {
                countPrefix = firstToken
                remainder = remainder.substringAfter(" ").trim()
            }
        }

        val tokens = remainder.split(MULTI_SPACE_REGEX).filter { it.isNotEmpty() }
        val keptModifiers = tokens.filter {
            it in SIZE_MODIFIERS || it in TEA_TYPE_MODIFIERS || it in WEIGHT_PACK_MODIFIERS
        }

        val parts = mutableListOf<String>()
        if (countPrefix != null) parts.add(countPrefix)
        parts.add(chosenBaseUnit)
        parts.addAll(keptModifiers)
        return parts.joinToString(" ")
    }

    /**
     * Composes a size/count/detail modifier (e.g. "صغيرة", "كبيرة", "×2", "شعرة") onto [currentNote]
     * without erasing the base unit (e.g. "علبة" + "كبيرة" -> "علبة كبيرة", + "×2" -> "2 علبة كبيرة").
     */
    fun applySmartNoteModifier(
        currentNote: String,
        modifierChip: String,
        defaultContainerIfEmpty: String = "علبة"
    ): String {
        val clean = currentNote.trim()

        // Handle count multipliers like "×2", "×3", "×4"
        if (modifierChip.startsWith("×")) {
            val targetCount = modifierChip.removePrefix("×").trim()
            val tokens = clean.split(MULTI_SPACE_REGEX).filter { it.isNotEmpty() }.toMutableList()
            if (tokens.isEmpty()) {
                return "$targetCount $defaultContainerIfEmpty"
            }
            val firstIsNumber = tokens.first().all { it.isDigit() } && tokens.size >= 2
            return if (firstIsNumber) {
                if (tokens.first() == targetCount) {
                    // Toggle off count prefix
                    tokens.drop(1).joinToString(" ")
                } else {
                    tokens[0] = targetCount
                    tokens.joinToString(" ")
                }
            } else {
                "$targetCount ${tokens.joinToString(" ")}"
            }
        }

        val tokens = clean.split(MULTI_SPACE_REGEX).filter { it.isNotEmpty() }.toMutableList()
        if (tokens.isEmpty()) {
            return if (modifierChip in SIZE_MODIFIERS) {
                "$defaultContainerIfEmpty $modifierChip"
            } else {
                modifierChip
            }
        }

        // If already active, toggle it off
        if (tokens.contains(modifierChip)) {
            tokens.remove(modifierChip)
            return tokens.joinToString(" ")
        }

        // Replace mutually exclusive modifier in the same group (e.g., صغيرة <-> متوسطة <-> كبيرة)
        when {
            modifierChip in SIZE_MODIFIERS -> {
                val idx = tokens.indexOfFirst { it in SIZE_MODIFIERS }
                if (idx >= 0) {
                    tokens[idx] = modifierChip
                } else {
                    tokens.add(modifierChip)
                }
            }
            modifierChip in TEA_TYPE_MODIFIERS -> {
                val idx = tokens.indexOfFirst { it in TEA_TYPE_MODIFIERS }
                if (idx >= 0) {
                    tokens[idx] = modifierChip
                } else {
                    tokens.add(modifierChip)
                }
            }
            modifierChip in WEIGHT_PACK_MODIFIERS -> {
                val idx = tokens.indexOfFirst { it in WEIGHT_PACK_MODIFIERS }
                if (idx >= 0) {
                    tokens[idx] = modifierChip
                } else {
                    tokens.add(modifierChip)
                }
            }
            else -> {
                tokens.add(modifierChip)
            }
        }

        return tokens.joinToString(" ")
    }

    fun buildShareText(
        vendors: List<VendorEntity>,
        items: List<ShoppingItemEntity>,
        selectedVendorId: Long? = null,
        selectedItemIds: Set<Long>? = null
    ): String {
        val activeItems = items.filter { !it.isDeleted && !it.isPurchased }
        val vendorFiltered = if (selectedVendorId != null) {
            activeItems.filter { it.vendorId == selectedVendorId }
        } else {
            activeItems
        }
        val filteredItems = if (selectedItemIds != null) {
            vendorFiltered.filter { selectedItemIds.contains(it.id) }
        } else {
            vendorFiltered
        }

        if (filteredItems.isEmpty()) {
            return "قائمة التقدية فارغة حالياً."
        }

        val vendorMap = vendors.associateBy { it.id }
        val sb = StringBuilder()

        if (selectedVendorId != null) {
            val vendorName = vendorMap[selectedVendorId]?.name ?: "البائع"
            sb.append("قائمة التقدية - ").append(vendorName).append(":\n")
            sb.append("--------------------\n")
            filteredItems.forEachIndexed { index, item ->
                sb.append("${index + 1}. ${item.name}")
                if (item.note.isNotBlank()) {
                    sb.append(" (${item.note.trim()})")
                }
                sb.append("\n")
            }
        } else {
            sb.append("قائمة النواقص للتقدية:\n")
            sb.append("====================\n")
            val grouped = filteredItems.groupBy { it.vendorId }
            val orderedVendors = vendors.filter { grouped.containsKey(it.id) }
            orderedVendors.forEachIndexed { vIndex, vendor ->
                if (vIndex > 0) sb.append("\n")
                sb.append("[ ").append(vendor.name).append(" ]\n")
                grouped[vendor.id].orEmpty().forEach { item ->
                    sb.append("- ").append(item.name)
                    if (item.note.isNotBlank()) {
                        sb.append(" (${item.note.trim()})")
                    }
                    sb.append("\n")
                }
            }
            val otherItems = filteredItems.filter { item -> orderedVendors.none { it.id == item.vendorId } }
            if (otherItems.isNotEmpty()) {
                if (orderedVendors.isNotEmpty()) sb.append("\n")
                sb.append("[ متفرقات ]\n")
                otherItems.forEach { item ->
                    sb.append("- ").append(item.name)
                    if (item.note.isNotBlank()) {
                        sb.append(" (${item.note.trim()})")
                    }
                    sb.append("\n")
                }
            }
        }
        sb.append("--------------------\n")
        sb.append("المجموع: ${formatItemsCount(filteredItems.size)}")
        return sb.toString()
    }
}
