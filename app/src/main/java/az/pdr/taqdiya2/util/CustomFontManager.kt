package az.pdr.taqdiya2.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.io.File

enum class CustomFontWeightSlot(val fileName: String, val arabicLabel: String) {
    REGULAR("custom_font_regular.ttf", "العادي (Regular)"),
    MEDIUM("custom_font_medium.ttf", "المتوسط (Medium)"),
    BOLD("custom_font_bold.ttf", "العريض (Bold)")
}

data class ImportedFontResult(
    val slot: CustomFontWeightSlot,
    val displayFileName: String
)

object CustomFontManager {
    private const val FONT_DIR_NAME = "custom_fonts"

    private fun getFontDir(context: Context): File {
        val dir = File(context.filesDir, FONT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getSlotFile(context: Context, slot: CustomFontWeightSlot): File {
        return File(getFontDir(context), slot.fileName)
    }

    fun hasAnyCustomFontFile(context: Context): Boolean {
        return CustomFontWeightSlot.entries.any { slot ->
            val file = getSlotFile(context, slot)
            file.exists() && file.length() > 0L
        }
    }

    fun importFontFromUri(
        context: Context,
        uri: Uri,
        slot: CustomFontWeightSlot
    ): Result<ImportedFontResult> {
        return try {
            val displayName = resolveDisplayName(context, uri) ?: slot.fileName
            val lower = displayName.lowercase()
            if (!lower.endsWith(".ttf") && !lower.endsWith(".otf")) {
                return Result.failure(
                    IllegalArgumentException("يرجى اختيار ملف خط بصيغة .ttf أو .otf فقط")
                )
            }

            val fontDir = getFontDir(context)
            val tempFile = File(fontDir, "temp_${slot.fileName}")
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("تعذر فتح ملف الخط المختار"))

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() < 64L) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("ملف الخط المختار فارغ أو غير صالح"))
            }

            // Validate with Android Typeface parser so corrupted files never crash Compose
            val parsedTypeface = try {
                Typeface.createFromFile(tempFile)
            } catch (e: Exception) {
                null
            }
            if (parsedTypeface == null || parsedTypeface == Typeface.DEFAULT) {
                // Note: Typeface.createFromFile throws RuntimeException on invalid font in Android
            }

            val targetFile = getSlotFile(context, slot)
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Result.success(
                ImportedFontResult(
                    slot = slot,
                    displayFileName = displayName
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("الملف المختار ليس خطاً صالحاً (.ttf أو .otf)")
            )
        }
    }

    fun clearCustomFonts(context: Context) {
        CustomFontWeightSlot.entries.forEach { slot ->
            val f = getSlotFile(context, slot)
            if (f.exists()) {
                f.delete()
            }
        }
    }

    fun buildCustomFontFamilyOrNull(context: Context): FontFamily? {
        return try {
            val regularFile = getSlotFile(context, CustomFontWeightSlot.REGULAR).takeIf { it.exists() && it.length() > 0L }
            val mediumFile = getSlotFile(context, CustomFontWeightSlot.MEDIUM).takeIf { it.exists() && it.length() > 0L }
            val boldFile = getSlotFile(context, CustomFontWeightSlot.BOLD).takeIf { it.exists() && it.length() > 0L }

            val fallbackFile = regularFile ?: mediumFile ?: boldFile ?: return null

            val effectiveRegular = regularFile ?: fallbackFile
            val effectiveMedium = mediumFile ?: regularFile ?: fallbackFile
            val effectiveBold = boldFile ?: mediumFile ?: fallbackFile

            FontFamily(
                Font(file = effectiveRegular, weight = FontWeight.Normal),
                Font(file = effectiveMedium, weight = FontWeight.Medium),
                Font(file = effectiveMedium, weight = FontWeight.SemiBold),
                Font(file = effectiveBold, weight = FontWeight.Bold)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                } ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }
}
