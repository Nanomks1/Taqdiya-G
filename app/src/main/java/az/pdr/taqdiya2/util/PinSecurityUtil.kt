package az.pdr.taqdiya2.util

import java.security.MessageDigest

object PinSecurityUtil {
    private const val SALT_PREFIX = "TAQDIYA_PIN_SALT_V1:"
    private const val HASH_PREFIX = "sha256:"

    fun isHashed(storedValue: String): Boolean {
        return storedValue.startsWith(HASH_PREFIX)
    }

    fun hashPin(rawPin: String): String {
        val clean = rawPin.trim()
        if (clean.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$SALT_PREFIX$clean".toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return "$HASH_PREFIX$hex"
    }

    fun normalizeForStorage(pinOrHash: String): String {
        val clean = pinOrHash.trim()
        return when {
            clean.isEmpty() -> ""
            isHashed(clean) -> clean
            clean.length == 4 && clean.all { it.isDigit() } -> hashPin(clean)
            else -> ""
        }
    }

    fun verifyPin(enteredPin: String, storedPinOrHash: String): Boolean {
        val cleanStored = storedPinOrHash.trim()
        if (cleanStored.isEmpty()) return true
        val cleanEntered = enteredPin.trim()
        if (cleanEntered.length != 4 || !cleanEntered.all { it.isDigit() }) return false
        return if (isHashed(cleanStored)) {
            hashPin(cleanEntered) == cleanStored
        } else {
            cleanEntered == cleanStored
        }
    }
}
