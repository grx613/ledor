package org.ledor.ui.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.data.prefs.GridMode
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath as M
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.Event
import org.ledor.domain.event.Occurrence
import org.ledor.domain.event.OccurrenceCalculator
import org.ledor.domain.event.YahrzeitAdarPolicy
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.*
import org.ledor.ui.navigation.EditorRoute
import org.ledor.ui.navigation.EventDetailRoute
import org.ledor.ui.navigation.ListJoinRoute
import org.ledor.ui.navigation.ListsRoute
import org.ledor.ui.settings.HealthSheet
import org.ledor.ui.theme.KalendarColors
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val PAGES = 2400
private const val CENTER = 1200

data class MonthGridModel(
    val titlePrimary: String,
    val titleSecondary: String,
    val cells: List<LocalDate>,
    val inMonth: (LocalDate) -> Boolean,
    val first: LocalDate,
    val last: LocalDate,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: AppViewModel, nav: NavController, outer: PaddingValues, splashVisible: Boolean) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val notices by vm.notices.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val s = settings ?: return
    val listsById = vm.lists.collectAsStateWithLifecycle().value.associate { it.list.id to it.list }

    val mode = s.gridMode
    val pager = rememberPagerState(initialPage = CENTER) { PAGES }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDate = selected?.let { LocalDate.parse(it) }
    var showHealth by remember { mutableStateOf(false) }
    var confirmNoReminders by remember { mutableStateOf(false) }

    val baseGreg = remember(today) { YearMonth.from(today) }
    val baseHeb = remember(today) { M.fromGregorian(today).let { it.year to it.month } }
    val firstDow = if (s.weekStartsOnSunday) DayOfWeek.SUNDAY else DayOfWeek.MONDAY

    fun model(page: Int): MonthGridModel {
        val off = page - CENTER
        return if (mode == GridMode.GREGORIAN) {
            val ym = baseGreg.plusMonths(off.toLong())
            val first = ym.atDay(1)
            val last = ym.atEndOfMonth()
            val hf = M.fromGregorian(first)
            val hl = M.fromGregorian(last)
            val sec = if (hf.month == hl.month) "${f.monthNom(hf.month)} ${hf.year}"
            else if (hf.year == hl.year) "${f.monthNom(hf.month)} – ${f.monthNom(hl.month)} ${hl.year}"
            else "${f.monthNom(hf.month)} ${hf.year} – ${f.monthNom(hl.month)} ${hl.year}"
            MonthGridModel(f.gregorianMonthTitle(ym), sec, gridCells(first, last, firstDow), { it.month == ym.month && it.year == ym.year }, first, last)
        } else {
            var (y, m) = baseHeb
            repeat(kotlin.math.abs(off)) { if (off > 0) M.nextMonth(y, m).let { y = it.first; m = it.second } else M.previousMonth(y, m).let { y = it.first; m = it.second } }
            val first = M.firstDayOf(y, m)
            val last = first.plusDays(M.daysInMonth(y, m) - 1L)
            val sec = if (first.month == last.month) f.gregorianMonthTitle(YearMonth.from(first))
            else "${f.gregorianMonthName(first.monthValue)} – ${f.gregorianMonthTitle(YearMonth.from(last))}"
            MonthGridModel(f.hebrewMonthTitle(y, m), sec, gridCells(first, last, firstDow), { !it.isBefore(first) && !it.isAfter(last) }, first, last)
        }
    }

    val current = remember(pager.currentPage, mode, today, firstDow) { model(pager.currentPage) }
    val monthOcc = remember(events, current, s.adarPolicy) { occurrencesIn(events.orEmpty(), current.cells.first(), current.cells.last(), s.adarPolicy) }

    val onboarding = !s.onboardingImportShown && !splashVisible
    if (onboarding) {
        AlertDialog(
            onDismissRequest = { vm.setOnboardingShown() },
            title = {
                Row(verticalAlignment = Alignment.Top) {
                    Text(stringResource(R.string.onboarding_title), Modifier.weight(1f))
                    IconButton(onClick = { vm.setOnboardingShown() }) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                }
            },
            text = { Text(stringResource(R.string.onboarding_text)) },
            confirmButton = { Button(onClick = { vm.setOnboardingShown(); nav.navigate(ListJoinRoute()) }) { Text(stringResource(R.string.yes)) } },
            dismissButton = { TextButton(onClick = { vm.setOnboardingShown() }) { Text(stringResource(R.string.no)) } },
        )
    }
    if (showHealth) HealthSheet(health, vm.container.healthChecker, vm::onResume) { showHealth = false }
    if (confirmNoReminders) ConfirmDialog(
        stringResource(R.string.health_disable_title), stringResource(R.string.health_disable_text), stringResource(R.string.health_disable_confirm),
        onConfirm = { vm.disableHealthWarnings(); confirmNoReminders = false }, onDismiss = { confirmNoReminders = false }, destructive = false,
    )

    Scaffold(
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = {
                        selected = null
                        scope.launch { pager.animateScrollToPage(CENTER) }
                    }) { Icon(AppIcons.Today, stringResource(R.string.today)) }
                    TextButton(onClick = { nav.navigate(ListsRoute) }) {
                        Icon(AppIcons.Group, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.lists))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val d = selectedDate
                nav.navigate(if (d != null) EditorRoute(calendar = CalendarKind.GREGORIAN.name, year = d.year, month = d.monthValue, day = d.dayOfMonth) else EditorRoute())
            }, containerColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Filled.Add, stringResource(R.string.add_event)) }
        },
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(bottom = 88.dp)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    notices.firstOrNull()?.let { InfoNotice(it) { vm.dismissNotices() } }
                    if (health.isNotEmpty() && s.reminders.anyEnabled && s.healthWarningsEnabled) {
                        WarningBanner(stringResource(R.string.health_banner_title), stringResource(R.string.health_banner_text)) {
                            TextButton(onClick = { confirmNoReminders = true }) { Text(stringResource(R.string.health_no_reminders)) }
                            Button(onClick = { showHealth = true }) { Text(stringResource(R.string.health_fix)) }
                        }
                    }
                }
            }
            item {
                MonthHeader(current, mode,
                    onPrev = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    onMode = { vm.setGridMode(it) })
            }
            item {
                WeekdayRow(firstDow, f)
                HorizontalPager(pager, beyondViewportPageCount = 0, verticalAlignment = Alignment.Top) { page ->
                    val m = remember(page, mode, today, firstDow) { model(page) }
                    val occ = remember(events, m, s.adarPolicy) { occurrencesIn(events.orEmpty(), m.cells.first(), m.cells.last(), s.adarPolicy) }
                    MonthGrid(m, mode, today, selectedDate, occ) { d -> selected = if (selected == d.toString()) null else d.toString() }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
            if (selectedDate != null) {
                val dayItems = monthOcc[selectedDate].orEmpty()
                item {
                    BlockTitle(stringResource(R.string.events_of_day, f.gregorianDayMonthWeekday(selectedDate)) + " · " + f.hebrewDate(M.fromGregorian(selectedDate))) {
                        TextButton(onClick = { selected = null }) { Text(stringResource(R.string.show_upcoming)) }
                    }
                }
                if (dayItems.isEmpty()) item {
                    Text(stringResource(R.string.no_events_day), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(dayItems, key = { "d" + it.first.id }) { (e, o) ->
                    EventRow(e, o, ChronoUnit.DAYS.between(today, o.date), listsById[e.listId], onClick = { nav.navigate(EventDetailRoute(e.id)) })
                }
                item {
                    OutlinedButton(onClick = {
                        nav.navigate(EditorRoute(calendar = CalendarKind.GREGORIAN.name, year = selectedDate.year, month = selectedDate.monthValue, day = selectedDate.dayOfMonth))
                    }, Modifier.padding(16.dp)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_on_this_date)) }
                }
            } else {
                val all = upcoming
                if (all != null && events?.isEmpty() == true) {
                    item {
                        EmptyState(stringResource(R.string.empty_events)) {
                            Button(onClick = { nav.navigate(EditorRoute()) }) { Text(stringResource(R.string.add_event)) }
                            OutlinedButton(onClick = { nav.navigate(ListJoinRoute()) }) { Text(stringResource(R.string.join_list)) }
                        }
                    }
                } else if (all != null) {
                    val within = all.filter { it.daysUntil <= 30 }
                    val shown = if (within.size >= 5) within else all.take(5)
                    item { BlockTitle(stringResource(R.string.upcoming_events)) {} }
                    items(shown, key = { "u" + it.event.id }) { u ->
                        EventRow(u.event, u.occurrence, u.daysUntil, u.list, onClick = { nav.navigate(EventDetailRoute(u.event.id)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockTitle(text: String, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action()
    }
}

fun gridCells(first: LocalDate, last: LocalDate, firstDow: DayOfWeek): List<LocalDate> {
    val lead = (first.dayOfWeek.value - firstDow.value + 7) % 7
    val start = first.minusDays(lead.toLong())
    val total = ChronoUnit.DAYS.between(start, last).toInt() + 1
    val weeks = (total + 6) / 7
    return (0 until weeks * 7).map { start.plusDays(it.toLong()) }
}

fun occurrencesIn(events: List<Event>, from: LocalDate, to: LocalDate, policy: YahrzeitAdarPolicy): Map<LocalDate, List<Pair<Event, Occurrence>>> =
    events.flatMap { e -> OccurrenceCalculator.between(e, from, to, policy).map { e to it } }
        .sortedBy { it.first.title }
        .groupBy { it.second.date }

@Composable
private fun MonthHeader(m: MonthGridModel, mode: GridMode, onPrev: () -> Unit, onNext: () -> Unit, onMode: (GridMode) -> Unit) {
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Canvas(Modifier.matchParentSize()) {
            drawMagenDavid(Color(0xFFD4A72C).copy(alpha = 0.09f), 2.dp.toPx(), center = Offset(size.width - 30.dp.toPx(), 20.dp.toPx()), sizePx = 150.dp.toPx())
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Filled.KeyboardArrowLeft, stringResource(R.string.prev_month)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(m.titlePrimary, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(m.titleSecondary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
                }
                IconButton(onClick = onNext) { Icon(Icons.Filled.KeyboardArrowRight, stringResource(R.string.next_month)) }
            }
            Segmented(
                listOf(GridMode.GREGORIAN, GridMode.HEBREW), mode,
                label = { Text(stringResource(if (it == GridMode.GREGORIAN) R.string.grid_gregorian else R.string.grid_hebrew), maxLines = 1) },
                onSelect = onMode, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun WeekdayRow(firstDow: DayOfWeek, f: org.ledor.ui.format.DateFormatters) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        for (i in 0 until 7) {
            val d = firstDow.plus(i.toLong())
            Text(
                f.weekdayShort(d), Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = if (d == DayOfWeek.SATURDAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    m: MonthGridModel,
    mode: GridMode,
    today: LocalDate,
    selected: LocalDate?,
    occ: Map<LocalDate, List<Pair<Event, Occurrence>>>,
    onClick: (LocalDate) -> Unit,
) {
    val f = rememberFormatters()
    val extra = KalendarColors.extra
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        m.cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    val h = M.fromGregorian(d)
                    val inMonth = m.inMonth(d)
                    val isSel = d == selected
                    val isToday = d == today
                    val dayOcc = occ[d].orEmpty()
                    val big = if (mode == GridMode.GREGORIAN) d.dayOfMonth.toString() else h.day.toString()
                    val small = if (mode == GridMode.GREGORIAN) (if (h.day == 1) "1 ${f.monthShort(h.month)}" else h.day.toString())
                    else (if (d.dayOfMonth == 1) "1 ${f.gregorianMonthName(d.monthValue).take(3)}" else d.dayOfMonth.toString())
                    val desc = "${f.gregorianDayMonth(d)}, ${f.hebrewDayMonth(h.day, h.month)}" +
                        if (dayOcc.isNotEmpty()) ", " + pluralEvents(dayOcc.size) else ""
                    Box(
                        Modifier.weight(1f).heightIn(min = 58.dp).padding(2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSel -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                                    d.dayOfWeek == DayOfWeek.SATURDAY -> extra.shabbat
                                    else -> Color.Transparent
                                },
                            )
                            .clickable { onClick(d) }
                            .semantics(mergeDescendants = true) { contentDescription = desc },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        val alpha = if (inMonth) 1f else 0.38f
                        val fg = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        if (isToday) {
                            Box(Modifier.matchParentSize().border(1.5.dp, extra.gold, RoundedCornerShape(12.dp)))
                            MagenDavid(Modifier.align(Alignment.TopEnd).padding(3.dp).size(11.dp), if (isSel) MaterialTheme.colorScheme.onPrimary else extra.gold, 1.1.dp)
                        }
                        Column(Modifier.padding(top = 6.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(big, color = fg.copy(alpha = alpha), fontSize = 16.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"))
                            Text(small, color = (if (isSel) fg else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = alpha), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                dayOcc.take(3).forEach { (e, _) ->
                                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (isSel) fg else extra.forType(e.type).copy(alpha = alpha)))
                                }
                                if (dayOcc.size > 3) Text("+${dayOcc.size - 3}", fontSize = 8.sp, color = fg.copy(alpha = alpha))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun pluralEvents(n: Int): String = androidx.compose.ui.res.pluralStringResource(R.plurals.events_count, n, n)
