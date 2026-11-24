package org.ledor.data.local

import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.Event
import org.ledor.domain.event.EventDate
import org.ledor.domain.event.EventType
import org.ledor.domain.event.GregorianParts
import org.ledor.domain.event.HebrewParts

/** Значение observe_by для «по обоим календарям» (B-1); основной календарь — календарь ввода. */
const val OBSERVE_BOTH = "BOTH"

fun EventEntity.toDomain(): Event {
    val g = if (gMonth != null && gDay != null) GregorianParts(gYear, gMonth, gDay) else null
    val hm = HebrewMonth.parse(hMonth)
    val h = if (hm != null && hDay != null) HebrewParts(hYear, hm, hDay) else null
    return Event(
        id = id,
        listId = listId,
        title = title,
        type = EventType.parse(type),
        customLabel = customLabel,
        recurring = recurring,
        observeBy = when (observeBy) {
            "GREGORIAN" -> CalendarKind.GREGORIAN
            OBSERVE_BOTH -> if (inputCalendar == "HEBREW") CalendarKind.HEBREW else CalendarKind.GREGORIAN
            else -> CalendarKind.HEBREW
        },
        observeBoth = observeBy == OBSERVE_BOTH,
        date = EventDate(
            inputCalendar = if (inputCalendar == "HEBREW") CalendarKind.HEBREW else CalendarKind.GREGORIAN,
            gregorian = g, afterSunset = afterSunset, hebrew = h,
        ),
        note = note,
    )
}

fun Event.toEntity(syncState: String, rev: Long = 0, now: Long = System.currentTimeMillis()) = EventEntity(
    id = id,
    listId = listId,
    rev = rev,
    syncState = syncState,
    updatedAt = now,
    title = title.trim(),
    type = type.name,
    customLabel = customLabel?.trim()?.takeIf { it.isNotEmpty() },
    recurring = recurring,
    observeBy = if (observeBoth) OBSERVE_BOTH else observeBy.name,
    inputCalendar = date.inputCalendar.name,
    gYear = date.gregorian?.year,
    gMonth = date.gregorian?.month,
    gDay = date.gregorian?.day,
    afterSunset = date.afterSunset,
    hYear = date.hebrew?.year,
    hMonth = date.hebrew?.month?.name,
    hDay = date.hebrew?.day,
    note = note?.trim()?.takeIf { it.isNotEmpty() },
)
