package org.ledor.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.data.local.SyncState
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.event.EventType
import org.ledor.domain.event.MitzvahCalculator
import org.ledor.domain.event.OccurrenceCalculator
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.*
import org.ledor.ui.navigation.EditorRoute
import org.ledor.ui.theme.KalendarColors
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(vm: AppViewModel, nav: NavController, id: String) {
    val event by remember(id) { vm.container.eventRepository.observe(id) }.collectAsStateWithLifecycle(null)
    val settings by vm.settings.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    LaunchedEffect(event) { pending = vm.container.database.events().get(id)?.syncState == SyncState.PENDING_UPSERT }

    val e = event
    val list = lists.firstOrNull { it.list.id == e?.listId }?.list
    val canEdit = list?.canEdit == true

    if (confirmDelete && e != null) ConfirmDialog(stringResource(R.string.delete_event_q, e.title), null, stringResource(R.string.delete),
        onConfirm = { confirmDelete = false; scope.launch { vm.container.eventRepository.delete(e.id); nav.popBackStack() } },
        onDismiss = { confirmDelete = false })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    if (canEdit && e != null) {
                        IconButton(onClick = { nav.navigate(EditorRoute(id = e.id)) }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        if (e == null) return@Scaffold
        val policy = settings?.adarPolicy ?: org.ledor.domain.event.YahrzeitAdarPolicy.ADAR_I
        // «По обоим календарям» — две ближайшие даты, по карточке на каждую.
        val nexts = remember(e, today, policy) { OccurrenceCalculator.nextEach(e, today, policy) }
        val next = nexts.firstOrNull()
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EventTypeIcon(e.type, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(e.title, style = MaterialTheme.typography.headlineSmall)
                    Text(f.typeLabel(e, next?.yearsSince), style = MaterialTheme.typography.titleSmall, color = KalendarColors.extra.forType(e.type))
                    if (list != null) { Spacer(Modifier.height(4.dp)); ListBadge(listDisplayName(list), list.colorIndex) }
                }
            }
            if (pending) Text(stringResource(R.string.pending_upload), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            nexts.forEach { o ->
                val days = ChronoUnit.DAYS.between(today, o.date)
                HighlightCard {
                    Text(stringResource(when {
                        !e.recurring -> R.string.date_label
                        nexts.size == 1 -> R.string.next_label
                        o.calendar == CalendarKind.HEBREW -> R.string.observe_hebrew
                        else -> R.string.observe_gregorian
                    }), style = MaterialTheme.typography.labelMedium)
                    Text(f.relativeDays(days).replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.headlineSmall)
                    Text(f.occurrenceDates(o, CalendarKind.GREGORIAN).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                    if (o.startsEveBefore) Text(stringResource(R.string.conv_eve_note, f.gregorianDayMonth(o.date.minusDays(1))), style = MaterialTheme.typography.bodySmall)
                    f.agePhrase(e.type, o.yearsSince)?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                }
            }
            if (nexts.isEmpty() && !e.recurring) {
                HighlightCard { Text(stringResource(R.string.event_passed)) }
            }

            SectionCard(title = stringResource(R.string.original_date)) {
                e.date.gregorian?.let { InfoLine(stringResource(R.string.gregorian_date), f.gregorianParts(it) + if (e.date.afterSunset) " · " + stringResource(R.string.after_sunset).lowercase() else "") }
                e.date.hebrew?.let { InfoLine(stringResource(R.string.hebrew_date), f.hebrewParts(it)) }
                if (e.recurring) InfoLine(stringResource(R.string.observe_title),
                    stringResource(when { e.observeBoth -> R.string.observe_both; e.observeBy == CalendarKind.HEBREW -> R.string.observe_hebrew; else -> R.string.observe_gregorian }))
                else InfoLine(stringResource(R.string.field_recurring), stringResource(R.string.recurring_no))
            }

            if (e.type == EventType.BIRTHDAY && e.date.hebrew?.year != null) {
                MitzvahCalculator.mitzvahHints(e.date.hebrew!!, today).forEach { h ->
                    HighlightCard {
                        Text(stringResource(if (h.type == EventType.BAR_MITZVAH) R.string.hint_bar else R.string.hint_bat,
                            f.hebrewDate(h.hebrewDate), f.gregorianShortWeekday(h.date)), style = MaterialTheme.typography.bodyMedium)
                        if (canEdit) TextButton(onClick = {
                            nav.navigate(EditorRoute(calendar = CalendarKind.HEBREW.name, year = h.hebrewDate.year, hebrewMonth = h.hebrewDate.month.name,
                                day = h.hebrewDate.day, type = h.type.name, title = e.title, listId = e.listId))
                        }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.add_as_event)) }
                    }
                }
            }

            // Первый год после смерти: шлошим, 11 и 12 месяцев, первый йорцайт (B-26).
            org.ledor.data.holiday.MilestoneEvents.firstYear(e, today, policy)?.let { milestones ->
                SectionCard(title = stringResource(R.string.first_year_title)) {
                    milestones.forEach { m ->
                        val days = ChronoUnit.DAYS.between(today, m.date)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(org.ledor.data.holiday.MilestoneEvents.labelRes(m.milestone)), style = MaterialTheme.typography.bodyLarge)
                                Text("${f.hebrewDate(m.hebrewDate)} · ${f.gregorianShortWeekday(m.date)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (days < 0) stringResource(R.string.milestone_passed) else f.relativeDays(days),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (days < 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(stringResource(R.string.first_year_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            e.note?.let { SectionCard(title = stringResource(R.string.field_note)) { Text(it) } }
            if (!canEdit) Text(stringResource(R.string.read_only_list), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
