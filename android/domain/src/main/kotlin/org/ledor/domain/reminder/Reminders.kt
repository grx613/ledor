package org.ledor.domain.reminder

import org.ledor.domain.event.Event
import org.ledor.domain.event.OccurrenceCalculator
import org.ledor.domain.event.YahrzeitAdarPolicy
import com.kosherjava.zmanim.AstronomicalCalendar
import com.kosherjava.zmanim.util.GeoLocation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
data class City(
    val id: String,
    val nameRu: String,
    val nameEn: String,
    val nameHe: String,
    val countryCode: String,
    val lat: Double,
    val lon: Double,
    val zoneId: String,
)

class CityCatalog(val cities: List<City>) {
    private val byId = cities.associateBy { it.id }
    fun byId(id: String?): City? = id?.let { byId[it] }

    fun search(query: String): List<City> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return cities
        return cities.filter {
            it.nameRu.lowercase().contains(q) || it.nameEn.lowercase().contains(q) || it.nameHe.contains(q)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String) = CityCatalog(json.decodeFromString<List<City>>(text))
        fun loadFromResources(): CityCatalog {
            val text = CityCatalog::class.java.getResourceAsStream("/cities.json")!!.bufferedReader().use { it.readText() }
            return parse(text)
        }
    }
}

fun interface SunsetProvider { fun sunset(date: LocalDate): Instant }

class CitySunsetProvider(private val city: City) : SunsetProvider {
    private val zone = ZoneId.of(city.zoneId)
    private val geo = GeoLocation(city.nameEn, city.lat, city.lon, zone)
    private fun calendar(date: LocalDate) = AstronomicalCalendar(geo).apply { localDate = date }

    override fun sunset(date: LocalDate): Instant =
        sunsetOrNull(date) ?: ZonedDateTime.of(date, LocalTime.of(18, 0), zone).toInstant()

    /** Заход солнца или null, если в этот день его нет (полярный день/ночь). */
    fun sunsetOrNull(date: LocalDate): Instant? = calendar(date).seaLevelSunset

    /** Выход звёзд: солнце на 8,5° ниже горизонта. null — в эту ночь так не темнеет (высокие широты летом). */
    fun nightfallOrNull(date: LocalDate): Instant? =
        calendar(date).getSunsetOffsetByDegrees(AstronomicalCalendar.GEOMETRIC_ZENITH + NIGHTFALL_DEGREES)

    companion object { const val NIGHTFALL_DEGREES = 8.5 }
}

enum class SunsetVerdict { BEFORE, TWILIGHT, AFTER }

/** [sunset] и [nightfall] — местное время границ сумерек, к которым относится ответ. */
data class SunsetHint(val verdict: SunsetVerdict, val sunset: LocalTime, val nightfall: LocalTime)

/** Подсказка «до или после захода» по местному времени и городу события (BACKLOG B-24). */
object SunsetHints {
    /** Если по градусам ночь не наступает — считаем выход звёзд через 72 минуты после захода. */
    const val NIGHTFALL_FALLBACK_MINUTES = 72L

    /** null — в этот день в этом городе солнце не заходит. */
    fun evaluate(date: LocalDate, time: LocalTime, city: City): SunsetHint? {
        val zone = ZoneId.of(city.zoneId)
        val provider = CitySunsetProvider(city)
        val moment = ZonedDateTime.of(date, time, zone).toInstant()
        fun bounds(d: LocalDate): Pair<Instant, Instant>? {
            val sunset = provider.sunsetOrNull(d) ?: return null
            val night = provider.nightfallOrNull(d)?.takeIf { it.isAfter(sunset) }
                ?: sunset.plusSeconds(NIGHTFALL_FALLBACK_MINUTES * 60)
            return sunset to night
        }
        fun hint(v: SunsetVerdict, b: Pair<Instant, Instant>) = SunsetHint(
            v, b.first.atZone(zone).toLocalTime().withSecond(0).withNano(0), b.second.atZone(zone).toLocalTime().withSecond(0).withNano(0),
        )
        // После полуночи ещё могут длиться сумерки вчерашнего вечера (летом на севере).
        bounds(date.minusDays(1))?.let { prev -> if (moment.isBefore(prev.second)) return hint(SunsetVerdict.TWILIGHT, prev) }
        val today = bounds(date) ?: return null
        return when {
            moment.isBefore(today.first) -> hint(SunsetVerdict.BEFORE, today)
            moment.isBefore(today.second) -> hint(SunsetVerdict.TWILIGHT, today)
            else -> hint(SunsetVerdict.AFTER, today)
        }
    }
}

class DefaultSunsetProvider(private val zone: ZoneId) : SunsetProvider {
    override fun sunset(date: LocalDate): Instant = ZonedDateTime.of(date, LocalTime.of(18, 0), zone).toInstant()
}

enum class ReminderKind { EVE, DAY }
enum class EveMode { BEFORE_SUNSET, FIXED_TIME }

data class ReminderSettings(
    val eveEnabled: Boolean = true,
    val eveMode: EveMode = EveMode.BEFORE_SUNSET,
    val eveMinutesBeforeSunset: Int = 180,
    val eveFixedTime: LocalTime = LocalTime.of(15, 0),
    val dayEnabled: Boolean = true,
    val dayTime: LocalTime = LocalTime.of(12, 0),
) {
    val anyEnabled get() = eveEnabled || dayEnabled
}

data class PlannedReminder(
    val eventId: String,
    val occurrenceDate: LocalDate,
    val kind: ReminderKind,
    val triggerAt: Instant,
    val yearsSince: Int?,
    /** Календарь наступления — для текста уведомления (при «по обоим» бывают оба). */
    val calendar: org.ledor.domain.calendar.CalendarKind = org.ledor.domain.calendar.CalendarKind.HEBREW,
)

object ReminderPlanner {
    fun eveTrigger(date: LocalDate, s: ReminderSettings, sunset: SunsetProvider, zone: ZoneId): Instant {
        val eve = date.minusDays(1)
        return when (s.eveMode) {
            EveMode.BEFORE_SUNSET -> sunset.sunset(eve).minusSeconds(s.eveMinutesBeforeSunset * 60L)
            EveMode.FIXED_TIME -> ZonedDateTime.of(eve, s.eveFixedTime, zone).toInstant()
        }
    }

    fun dayTrigger(date: LocalDate, s: ReminderSettings, zone: ZoneId): Instant =
        ZonedDateTime.of(date, s.dayTime, zone).toInstant()

    /** Все напоминания с triggerAt в (fromExclusive, toInclusive], по возрастанию. */
    fun plan(
        events: List<Event>,
        settings: ReminderSettings,
        sunset: SunsetProvider,
        zone: ZoneId,
        policy: YahrzeitAdarPolicy,
        fromExclusive: Instant,
        toInclusive: Instant,
    ): List<PlannedReminder> {
        if (!settings.anyEnabled || !toInclusive.isAfter(fromExclusive)) return emptyList()
        val fromDate = fromExclusive.atZone(zone).toLocalDate().minusDays(1)
        val toDate = toInclusive.atZone(zone).toLocalDate().plusDays(2)
        val result = ArrayList<PlannedReminder>()
        for (e in events) {
            for (occ in OccurrenceCalculator.between(e, fromDate, toDate, policy)) {
                if (settings.eveEnabled) {
                    val t = eveTrigger(occ.date, settings, sunset, zone)
                    if (t.isAfter(fromExclusive) && !t.isAfter(toInclusive)) {
                        result += PlannedReminder(e.id, occ.date, ReminderKind.EVE, t, occ.yearsSince, occ.calendar)
                    }
                }
                if (settings.dayEnabled) {
                    val t = dayTrigger(occ.date, settings, zone)
                    if (t.isAfter(fromExclusive) && !t.isAfter(toInclusive)) {
                        result += PlannedReminder(e.id, occ.date, ReminderKind.DAY, t, occ.yearsSince, occ.calendar)
                    }
                }
            }
        }
        return result.sortedWith(compareBy({ it.triggerAt }, { it.eventId }, { it.kind }))
    }
}
