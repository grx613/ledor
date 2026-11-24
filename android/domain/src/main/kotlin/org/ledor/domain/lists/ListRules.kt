package org.ledor.domain.lists

import java.security.SecureRandom
import java.text.Normalizer

object ListNameRules {
    const val MIN = 3
    const val MAX = 60

    fun clean(name: String): String = name.trim().replace(Regex("\\s+"), " ")
    fun isValid(name: String): Boolean = clean(name).length in MIN..MAX

    /** Как на сервере: lower(regexp_replace(btrim(normalize(name, NFKC)), '\s+', ' ')). */
    fun key(name: String): String = clean(Normalizer.normalize(name, Normalizer.Form.NFKC)).lowercase()
}

object PinRules {
    val READ_LENGTH = 4..8
    val MASTER_LENGTH = 6..12
    private val digits = Regex("^[0-9]+$")

    fun isValidReadPin(pin: String) = pin.length in READ_LENGTH && digits.matches(pin)
    fun isValidMasterPin(pin: String) = pin.length in MASTER_LENGTH && digits.matches(pin)
    fun isValidAnyPin(pin: String) = pin.length in 4..12 && digits.matches(pin)
}

object PinGenerator {
    private val random = SecureRandom()
    fun generate(length: Int): String = buildString { repeat(length) { append(random.nextInt(10)) } }
}
