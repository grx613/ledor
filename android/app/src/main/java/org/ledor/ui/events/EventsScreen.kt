package org.ledor.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.data.holiday.HOLIDAYS_LIST_ID
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.event.OccurrenceCalculator
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.*
import org.ledor.ui.navigation.EditorRoute
import org.ledor.ui.navigation.EventDetailRoute
import org.ledor.ui.navigation.ListJoinRoute
import org.ledor.ui.navigation.ListsRoute
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EventsScreen(vm: AppViewModel, nav: NavController, outer: PaddingValues) {
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var showPast by rememberSaveable { mutableStateOf(false) }
    val kind = if (tab == 0) CalendarKind.HEBREW else CalendarKind.GREGORIAN
    val listsById = lists.associate { it.list.id to it.list }

    val items = upcoming.orEmpty().filter { filter == null || it.event.listId == filter }
    val upcomingIds = upcoming.orEmpty().map { it.event.id }.toSet()
    val past = events.orEmpty().filter { it.id !in upcomingIds && !org.ledor.data.holiday.HolidayEvents.isBuiltIn(it.id) && !org.ledor.data.holiday.MilestoneEvents.isMilestone(it.id) && (filter == null || it.listId == filter) }
        .mapNotNull { e -> OccurrenceCalculator.oneOff(e)?.let { e to it } }
        .sortedByDescending { it.second.date }

    Scaffold(
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_events)) },
                actions = {
                    TextButton(onClick = { nav.navigate(ListsRoute) }) {
                        Icon(AppIcons.Group, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.lists))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(EditorRoute()) }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_event))
            }
        },
    ) { inner ->
        Column(Modifier.padding(inner)) {
            PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                Tab(tab == 0, { tab = 0 }, text = { Text(stringResource(R.string.tab_hebrew_calendar)) })
                Tab(tab == 1, { tab = 1 }, text = { Text(stringResource(R.string.tab_gregorian_calendar)) })
            }
            val hasHolidays = upcoming.orEmpty().any { it.event.listId == HOLIDAYS_LIST_ID }
            if (lists.size > 1 || hasHolidays) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(filter == null, { filter = null }, label = { Text(stringResource(R.string.all_lists)) })
                    lists.forEach { l ->
                        FilterChip(filter == l.list.id, { filter = l.list.id }, label = { Text(listDisplayName(l.list)) })
                    }
                    if (hasHolidays) FilterChip(filter == HOLIDAYS_LIST_ID, { filter = HOLIDAYS_LIST_ID }, label = { Text(stringResource(R.string.holidays_title)) })
                }
            }
            if (events?.none { it.listId != HOLIDAYS_LIST_ID } == true && items.isEmpty()) {
                EmptyState(stringResource(R.string.empty_events)) {
                    Button(onClick = { nav.navigate(EditorRoute()) }) { Text(stringResource(R.string.add_event)) }
                    OutlinedButton(onClick = { nav.navigate(ListJoinRoute()) }) { Text(stringResource(R.string.join_list)) }
                }
                return@Column
            }
            val groups = items.groupBy { u ->
                if (kind == CalendarKind.HEBREW) f.hebrewMonthTitle(u.occurrence.hebrewDate.year, u.occurrence.hebrewDate.month)
                else f.gregorianMonthTitle(YearMonth.from(u.occurrence.date))
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                groups.forEach { (title, group) ->
                    stickyHeader(key = "h$title") {
                        Text(
                            title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(group, key = { "${it.event.id}@${it.occurrence.date}" }) { u ->
                        EventRow(u.event, u.occurrence, u.daysUntil, u.list, onClick = { nav.navigate(EventDetailRoute(u.event.id)) }, showList = filter == null)
                    }
                }
                if (past.isNotEmpty()) {
                    item {
                        TextButton(onClick = { showPast = !showPast }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(stringResource(R.string.past_events, past.size))
                            Icon(if (showPast) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                        }
                    }
                    if (showPast) items(past, key = { "p" + it.first.id }) { (e, o) ->
                        EventRow(e, o, null, listsById[e.listId], onClick = { nav.navigate(EventDetailRoute(e.id)) })
                    }
                }
            }
        }
    }
}
