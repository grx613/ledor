package org.ledor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
 * [primary] — какой календарь показывать крупно.
 */
@Composable
fun EventRow(
    event: Event,
    occurrence: Occurrence?,
    daysUntil: Long?,
    list: ListEntity?,
    onClick: () -> Unit,
    primary: CalendarKind = CalendarKind.HEBREW,
    showList: Boolean = true,
    pending: Boolean = false,
) {
    val f = rememberFormatters()
    val typeLabel = f.typeLabel(event, occurrence?.yearsSince)
    val heb = occurrence?.let { f.hebrewDate(it.hebrewDate) }
    val greg = occurrence?.let { f.gregorianShortWeekday(it.date) }
    val first = if (primary == CalendarKind.HEBREW) heb else greg
    val second = if (primary == CalendarKind.HEBREW) greg else heb
    val age = f.agePhrase(event.type, occurrence?.yearsSince)
    val rel = daysUntil?.let {
        if (it == 1L && occurrence?.startsEveBefore == true) stringResource(R.string.rel_tomorrow_eve) else f.relativeDays(it)
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(event.title, typeLabel, first, rel, age).joinToString(", ") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EventTypeIcon(event.type)
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
                listOfNotNull(typeLabel, age).joinToString(" · "),
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

/** ✡ — отмечается по еврейскому календарю, ○ — по григорианскому. */
@Composable
fun MarkObserve(kind: CalendarKind) {
    if (kind == CalendarKind.HEBREW) MagenDavid(Modifier.size(12.dp), KalendarColors.extra.gold, 1.2.dp)
    else androidx.compose.foundation.Canvas(Modifier.size(12.dp)) {
        drawCircle(KalendarColors_primaryHint, radius = size.minDimension / 2.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
    }
}

private val KalendarColors_primaryHint = androidx.compose.ui.graphics.Color(0xFF6B7FB3)
