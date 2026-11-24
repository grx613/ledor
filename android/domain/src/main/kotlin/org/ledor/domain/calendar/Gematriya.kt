package org.ledor.domain.calendar

/** Числа еврейскими буквами (для иврита и идиша): 28 → כ״ח, 5750 → תש״ן. Тысячи года не пишутся. */
object Gematriya {
    private val ONES = arrayOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
    private val TENS = arrayOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
    private val HUNDREDS = arrayOf("", "ק", "ר", "ש", "ת")
    private val FINAL = mapOf('כ' to 'ך', 'מ' to 'ם', 'נ' to 'ן', 'פ' to 'ף', 'צ' to 'ץ')

    fun format(number: Int): String {
        val n = number % 1000
        val sb = StringBuilder()
        var h = n / 100
        while (h > 4) { sb.append('ת'); h -= 4 }
        sb.append(HUNDREDS[h])
        when (val r = n % 100) {
            15 -> sb.append("טו")
            16 -> sb.append("טז")
            else -> sb.append(TENS[r / 10]).append(ONES[r % 10])
        }
        if (sb.length == 1) return "$sb׳"
        val last = sb.last()
        return sb.substring(0, sb.length - 1) + "״" + (FINAL[last] ?: last)
    }
}
