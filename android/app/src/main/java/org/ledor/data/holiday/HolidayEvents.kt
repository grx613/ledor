package org.ledor.data.holiday

import android.content.Context
import org.ledor.R
import org.ledor.data.prefs.HolidaySettings
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.event.Event
import org.ledor.domain.event.EventDate
import org.ledor.domain.event.EventType
import org.ledor.domain.event.HebrewParts
import org.ledor.domain.holiday.CandleLighting
import org.ledor.domain.holiday.CandleLightingCalculator
import org.ledor.domain.holiday.Holiday
import org.ledor.domain.holiday.HolidayKind
import org.ledor.domain.holiday.Holidays
import org.ledor.domain.reminder.City
import org.ledor.ui.format.DateFormatters
import org.ledor.ui.locale.AppLocale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val HOLIDAYS_LIST_ID = "holidays"

/**
 * Встроенный список «Еврейские праздники» (B-25). В базе его нет: события вычисляются и подмешиваются
 * к событиям пользователя как разовые, поэтому календарь, список событий и планировщик работают с ними как с обычными.
 *
 * Идентификаторы: `holiday:<Holiday>:<первый день>` — праздник; `holiday+:<Holiday>:<день>` — второй и следующие дни
 * (только для сетки календаря); `shabbat:<суббота>` — Шабат.
 */
class HolidayEvents(private val baseContext: Context) {
    companion object {
        private const val MAIN = "holiday:"
        private const val NEXT_DAY = "holiday+:"
        private const val SHABBAT = "shabbat:"

        fun isBuiltIn(eventId: String) = eventId.startsWith(MAIN) || eventId.startsWith(NEXT_DAY) || eventId.startsWith(SHABBAT)
        /** Второй и следующие дни многодневного праздника. */
        fun isNextDay(eventId: String) = eventId.startsWith(NEXT_DAY)
        fun isShabbat(eventId: String) = eventId.startsWith(SHABBAT)

        /** Праздник строки `holiday:<Holiday>:<дата>`; у следующих дней и Шабата — null. */
        fun holidayOf(eventId: String): Holiday? =
            if (!eventId.startsWith(MAIN)) null
            else runCatching { Holiday.valueOf(eventId.removePrefix(MAIN).substringBefore(':')) }.getOrNull()

        /** Строка под датами: когда начинается («С вечера пт, 4 декабря · свечи в 16:02»). */
        fun rowLine(e: Event): String? = e.note?.substringBefore('\n')?.takeIf { it.isNotBlank() }
        /** Фраза для уведомления: зажигание свечей или время поста. */
        fun notificationLine(e: Event): String? = e.note?.substringAfter('\n', "")?.takeIf { it.isNotBlank() }

        /** По обычаю Израиля — один день праздника: если город в Израиле, а без города — по часовому поясу телефона. */
        fun inIsrael(city: City?, zone: ZoneId = ZoneId.systemDefault()): Boolean =
            if (city != null) city.countryCode == "IL" else zone.id in setOf("Asia/Jerusalem", "Asia/Tel_Aviv", "Israel")

        fun nameRes(h: Holiday): Int = when (h) {
            Holiday.ROSH_HASHANA -> R.string.holiday_rosh_hashana
            Holiday.TZOM_GEDALIAH -> R.string.holiday_tzom_gedaliah
            Holiday.YOM_KIPPUR -> R.string.holiday_yom_kippur
            Holiday.SUKKOT -> R.string.holiday_sukkot
            Holiday.SHMINI_ATZERET -> R.string.holiday_shmini_atzeret
            Holiday.CHANUKAH -> R.string.holiday_chanukah
            Holiday.ASARA_BETEVET -> R.string.holiday_asara_betevet
            Holiday.TU_BISHVAT -> R.string.holiday_tu_bishvat
            Holiday.TAANIT_ESTHER -> R.string.holiday_taanit_esther
            Holiday.PURIM -> R.string.holiday_purim
            Holiday.SHUSHAN_PURIM -> R.string.holiday_shushan_purim
            Holiday.PESACH -> R.string.holiday_pesach
            Holiday.PESACH_VII -> R.string.holiday_pesach_vii
            Holiday.YOM_HASHOAH -> R.string.holiday_yom_hashoah
            Holiday.YOM_HAZIKARON -> R.string.holiday_yom_hazikaron
            Holiday.YOM_HAATZMAUT -> R.string.holiday_yom_haatzmaut
            Holiday.LAG_BAOMER -> R.string.holiday_lag_baomer
            Holiday.YOM_YERUSHALAYIM -> R.string.holiday_yom_yerushalayim
            Holiday.SHAVUOT -> R.string.holiday_shavuot
            Holiday.TZOM_TAMMUZ -> R.string.holiday_tzom_tammuz
            Holiday.TISHA_BAV -> R.string.holiday_tisha_bav
            Holiday.TU_BAV -> R.string.holiday_tu_bav
        }

        private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
    }

    /** Тексты — на языке приложения. */
    private val context get() = AppLocale.wrap(baseContext)

    private fun event(id: String, title: String, label: String, date: LocalDate, note: String?) = Event(
        id = id, listId = HOLIDAYS_LIST_ID, title = title, type = EventType.OTHER, customLabel = label, recurring = false,
        observeBy = CalendarKind.HEBREW,
        date = EventDate.fromHebrew(HebrewParts.of(HebrewCalendarMath.fromGregorian(date))), note = note,
    )

    private fun time(c: CandleLighting, city: City) = TIME.format(c.time.atZone(ZoneId.of(city.zoneId)))

    /** Фраза для уведомления: когда зажигают свечи. */
    private fun candleNote(c: CandleLighting?, city: City?): String = if (c == null || city == null) "" else
        context.getString(if (c.afterNightfall) R.string.notif_candles_after else R.string.notif_candles, time(c, city))

    /** В [Event.note] две строки: для списка и для уведомления (см. [rowLine], [notificationLine]). */
    private fun note(row: String, notification: String) = "$row\n$notification"

    /**
     * События списка за [from]..[to] (праздники, хотя бы один день которых попадает в отрезок).
     * [nextDays] — добавлять ли вторые и следующие дни праздников (нужны только календарю).
     * Шабаты — за [shabbatFrom]..[shabbatTo]: только при включённом переключателе и выбранном городе; если в субботу
     * начинается праздник со свечами, отдельной строки Шабата нет (напоминание было бы вторым о том же).
     * [yizkor] — у пользователя есть йорцайты: у четырёх праздников отмечается день Изкора (B-26).
     */
    fun build(s: HolidaySettings, city: City?, from: LocalDate, to: LocalDate, shabbatFrom: LocalDate, shabbatTo: LocalDate, nextDays: Boolean,
              yizkor: Boolean = false): List<Event> {
        if (!s.enabled) return emptyList()
        val ctx = context
        val f = DateFormatters(ctx)
        val result = ArrayList<Event>()
        val candleDays = HashSet<LocalDate>()
        for (o in Holidays.between(from, to, inIsrael(city))) {
            val h = o.holiday
            val name = ctx.getString(nameRes(h))
            val kind = ctx.getString(when (h.kind) {
                HolidayKind.FESTIVAL -> R.string.holiday_kind_festival
                HolidayKind.FAST -> R.string.holiday_kind_fast
                HolidayKind.MEMORIAL -> R.string.holiday_kind_memorial
            })
            val candles = if (h.candles && city != null) CandleLightingCalculator.forDay(o.start, city) else null
            if (candles != null) candleDays += o.start
            // Когда начинается: посты «с рассвета» — в сам день, остальное — накануне вечером; у Хануки — первая свеча.
            val eve = f.gregorianDayMonthWeekday(o.start.minusDays(1))
            val starts = when {
                h.dawn -> ctx.getString(R.string.holiday_fast_dawn)
                h == Holiday.CHANUKAH -> ctx.getString(R.string.holiday_first_candle, eve)
                else -> ctx.getString(R.string.holiday_from_eve, eve)
            }
            val row = if (candles == null || city == null) starts else starts + " · " +
                ctx.getString(if (candles.afterNightfall) R.string.holiday_candles_after else R.string.holiday_candles_eve, time(candles, city))
            val yizkorDay = if (yizkor) Holidays.yizkorDay(o) else null
            val notification = listOfNotNull(
                (if (h.dawn) ctx.getString(R.string.notif_fast_dawn) else candleNote(candles, city)).takeIf { it.isNotEmpty() },
                yizkorDay?.let { ctx.getString(R.string.notif_yizkor, f.gregorianDayMonthWeekday(it)) },
            ).joinToString(" ")
            val rowWithYizkor = if (yizkorDay == null) row else row + " · " + ctx.getString(R.string.yizkor)
            result += event("$MAIN${h.name}:${o.start}", name, kind, o.start, note(rowWithYizkor, notification))
            // Седьмой день Песаха — отдельная строка списка, поэтому дни Песаха дальше шестого не дублируем.
            val lastDay = if (h == Holiday.PESACH) minOf(o.days, 6) else o.days
            if (nextDays) for (n in 2..lastDay) {
                val day = o.start.plusDays(n - 1L)
                result += event("$NEXT_DAY${h.name}:$day", ctx.getString(R.string.holiday_day_n, name, n), kind, day, null)
            }
        }
        if (s.shabbat && city != null) for (sat in Holidays.shabbats(shabbatFrom, shabbatTo)) {
            if (sat in candleDays) continue
            val candles = CandleLightingCalculator.forDay(sat, city) ?: continue
            result += event("$SHABBAT$sat", ctx.getString(R.string.shabbat), ctx.getString(R.string.shabbat_candles_label, time(candles, city)),
                sat, note("", candleNote(candles, city)))
        }
        return result
    }
}
