package org.ledor.data.holiday

import android.content.Context
import org.ledor.R
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.event.Event
import org.ledor.domain.event.EventDate
import org.ledor.domain.event.EventType
import org.ledor.domain.event.HebrewParts
import org.ledor.domain.event.MilestoneDate
import org.ledor.domain.event.MourningCalculator
import org.ledor.domain.event.MourningMilestone
import org.ledor.domain.event.YahrzeitAdarPolicy
import org.ledor.ui.locale.AppLocale
import java.time.LocalDate

/**
 * Вехи первого года после смерти (B-26): шлошим, 11 и 12 месяцев. В базе их нет — вычисляются из йорцайтов с известным
 * годом и подмешиваются как разовые события того же списка (`milestone:<веха>:<id йорцайта>`), пока не прошёл первый йорцайт.
 * Сам первый йорцайт — обычное наступление события, отдельной строки для него нет.
 */
class MilestoneEvents(private val baseContext: Context) {
    companion object {
        private const val PREFIX = "milestone:"
        fun isMilestone(eventId: String) = eventId.startsWith(PREFIX)
        /** Йорцайт, к которому относится веха. */
        fun parentId(eventId: String) = eventId.removePrefix(PREFIX).substringAfter(':')

        fun labelRes(m: MourningMilestone): Int = when (m) {
            MourningMilestone.SHLOSHIM -> R.string.milestone_shloshim
            MourningMilestone.ELEVEN_MONTHS -> R.string.milestone_11_months
            MourningMilestone.TWELVE_MONTHS -> R.string.milestone_12_months
            MourningMilestone.FIRST_YAHRZEIT -> R.string.milestone_first_yahrzeit
        }

        /** Вехи первого года для события; null — не йорцайт, год смерти неизвестен или первый йорцайт уже прошёл. */
        fun firstYear(e: Event, today: LocalDate, policy: YahrzeitAdarPolicy): List<MilestoneDate>? {
            if (e.type != EventType.YAHRZEIT || !e.recurring) return null
            val death = e.date.hebrew?.toHebrewDateOrNull() ?: return null
            return MourningCalculator.firstYear(death, policy).takeIf { !it.last().date.isBefore(today) }
        }
    }

    fun build(events: List<Event>, today: LocalDate, policy: YahrzeitAdarPolicy): List<Event> {
        val ctx = AppLocale.wrap(baseContext)
        return events.flatMap { e ->
            firstYear(e, today, policy).orEmpty().filter { it.milestone != MourningMilestone.FIRST_YAHRZEIT }.map { m ->
                Event(
                    id = "$PREFIX${m.milestone.name}:${e.id}", listId = e.listId, title = e.title, type = EventType.OTHER,
                    customLabel = ctx.getString(labelRes(m.milestone)), recurring = false, observeBy = CalendarKind.HEBREW,
                    date = EventDate.fromHebrew(HebrewParts.of(m.hebrewDate)), note = null,
                )
            }
        }
    }
}
