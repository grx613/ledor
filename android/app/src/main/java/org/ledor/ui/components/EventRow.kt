package org.ledor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.ledor.R
import org.ledor.data.local.LOCAL_LIST_ID
import org.ledor.data.local.ListEntity
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.event.Event
import org.ledor.domain.event.Occurrence
import org.ledor.ui.format.DateFormatters
import org.ledor.ui.theme.KalendarColors

@Composable
fun rememberFormatters(): DateFormatters {
    val ctx = LocalContext.current
    return remember(ctx) { DateFormatters(ctx) }
}

@Composable
fun listDisplayName(list: ListEntity?): String =
    if (list == null || list.id == LOCAL_LIST_ID) stringResource(R.string.list_personal) else list.name

/**
 * Строка события: значок, имя, тип, даты в двух календарях, «через N дней», возраст, метка списка.
 * Крупно — главная дата (та, что отмечается и не меняется из года в год), мелко — тот же день в другом календаре (D-55).
 */
@Composable
fun EventRow(
    event: Event,
    occurrence: Occurrence?,
    daysUntil: Long?,
    list: ListEntity?,
    onClick: (() -> Unit)?,
    showList: Boolean = true,
    pending: Boolean = false,
) {
    val f = rememberFormatters()
    val typeLabel = f.typeLabel(event, occurrence?.yearsSince)
    val dates = occurrence?.let { f.occurrenceDates(it) }.orEmpty()
    val first = dates.getOrNull(0)
    val second = dates.getOrNull(1)
    val age = f.agePhrase(event.type, occurrence?.yearsSince)
    val rel = daysUntil?.let {
        // Пост «с рассвета» накануне вечером ещё не начинается.
        val fromEve = occurrence?.startsEveBefore == true && org.ledor.data.holiday.HolidayEvents.holidayOf(event.id)?.dawn != true
        if (it == 1L && fromEve) stringResource(R.string.rel_tomorrow_eve) else f.relativeDays(it)
    }
    val holidayLine = if (event.listId == org.ledor.data.holiday.HOLIDAYS_LIST_ID) org.ledor.data.holiday.HolidayEvents.rowLine(event) else null
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(event.title, typeLabel, first, holidayLine, rel, age).joinToString(", ") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (event.listId == org.ledor.data.holiday.HOLIDAYS_LIST_ID) HolidayIcon() else EventTypeIcon(event.type)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (pending) {
                    Spacer(Modifier.width(6.dp))
                    Icon(AppIcons.Schedule, stringResource(R.string.pending_upload), Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                listOfNotNull(typeLabel, age, event.takeIf { it.isMonthly }?.let { f.monthlyDay(it.date.gregorian!!.day) }).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = KalendarColors.extra.forType(event.type), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (first != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarkObserve(occurrence?.calendar ?: event.observeBy)
                    Spacer(Modifier.width(4.dp))
                    Text(first, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                second?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
            }
            holidayLine?.let { Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary) }
            if (showList && list != null && list.id != LOCAL_LIST_ID) ListBadge(listDisplayName(list), list.colorIndex)
        }
        if (rel != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                rel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if ((daysUntil ?: 99) <= 1) FontWeight.Bold else FontWeight.Medium,
                color = if ((daysUntil ?: 99) <= 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                maxLines = 2,
                modifier = Modifier.widthIn(max = 96.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

/** Значок события из встроенного списка праздников. */
@Composable
fun HolidayIcon(size: androidx.compose.ui.unit.Dp = 40.dp) {
    val gold = KalendarColors.extra.gold
    androidx.compose.foundation.layout.Box(
        Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(gold.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { MagenDavid(Modifier.size(size * 0.55f), gold, 1.8.dp) }
}

/** ✡ — отмечается по еврейскому календарю, ○ — по григорианскому. */
@Composable
fun MarkObserve(kind: CalendarKind) {
    if (kind == CalendarKind.HEBREW) MagenDavid(Modifier.size(12.dp), KalendarColors.extra.gold, 1.2.dp)
    else androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        drawCircle(KalendarColors_primaryHint, radius = size.minDimension / 2.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
    }
}

private val KalendarColors_primaryHint = androidx.compose.ui.graphics.Color(0xFF6B7FB3)
