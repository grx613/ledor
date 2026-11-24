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
        val next = remember(e, today, policy) { OccurrenceCalculator.next(e, today, policy) }
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

            if (next != null) {
                val days = ChronoUnit.DAYS.between(today, next.date)
                HighlightCard {
                    Text(stringResource(if (e.recurring) R.string.next_label else R.string.date_label), style = MaterialTheme.typography.labelMedium)
                    Text(f.relativeDays(days).replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.headlineSmall)
                    Text("${f.gregorianShortWeekday(next.date)} · ${f.hebrewDate(next.hebrewDate)}", style = MaterialTheme.typography.bodyMedium)
                    if (next.startsEveBefore) Text(stringResource(R.string.conv_eve_note, f.gregorianDayMonth(next.date.minusDays(1))), style = MaterialTheme.typography.bodySmall)
                    f.agePhrase(e.type, next.yearsSince)?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                }
            } else if (!e.recurring) {
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
