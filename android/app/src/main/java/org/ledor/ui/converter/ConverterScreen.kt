package org.ledor.ui.converter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath as M
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.*
import org.ledor.ui.components.*
import org.ledor.ui.navigation.EditorRoute
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(nav: NavController, outer: PaddingValues) {
    val f = rememberFormatters()
    val today = remember { LocalDate.now() }
    val h0 = remember { M.fromGregorian(today) }
    var dir by rememberSaveable { mutableStateOf(CalendarKind.GREGORIAN) }
    var gd by rememberSaveable { mutableIntStateOf(today.dayOfMonth) }
    var gm by rememberSaveable { mutableIntStateOf(today.monthValue) }
    var gy by rememberSaveable { mutableStateOf(today.year.toString()) }
    var after by rememberSaveable { mutableStateOf(false) }
    var hd by rememberSaveable { mutableIntStateOf(h0.day) }
    var hm by rememberSaveable { mutableStateOf(h0.month.name) }
    var hy by rememberSaveable { mutableStateOf(h0.year.toString()) }

    val gDate = gy.toIntOrNull()?.takeIf { M.isGregorianYearSupported(it) }?.let { runCatching { LocalDate.of(it, gm, gd) }.getOrNull() }
    val hYear = hy.toIntOrNull()?.takeIf { M.isHebrewYearSupported(it) }
    val hMonth = HebrewMonth.valueOf(hm)
    val hDate = hYear?.takeIf { M.isValid(it, hMonth, hd) }?.let { HebrewDate(it, hMonth, hd) }

    Scaffold(
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_converter)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Segmented(listOf(CalendarKind.GREGORIAN, CalendarKind.HEBREW), dir,
                label = { Text(stringResource(if (it == CalendarKind.GREGORIAN) R.string.conv_g2h else R.string.conv_h2g), maxLines = 1) },
                onSelect = { dir = it })

            var resultHebrew: HebrewDate? = null
            var resultDate: LocalDate? = null
            SectionCard {
                if (dir == CalendarKind.GREGORIAN) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectField(stringResource(R.string.day), gd, (1..31).toList(), { it.toString() }, { gd = it }, Modifier.weight(0.8f), isError = gDate == null, showArrow = false)
                        SelectField(stringResource(R.string.month), gm, (1..12).toList(), { f.gregorianMonthName(it) }, { gm = it }, Modifier.weight(1.4f))
                        NumberField(stringResource(R.string.year), gy, { gy = it }, Modifier.weight(1f), isError = gDate == null)
                    }
                    SunsetToggle(after) { after = it }
                    if (gDate != null) {
                        resultHebrew = M.fromGregorian(gDate, after)
                        resultDate = gDate
                    }
                } else {
                    val months = hYear?.let { M.monthsOfYear(it) } ?: HebrewMonth.ORDER_WITHOUT_YEAR
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectField(stringResource(R.string.day), hd, (1..30).toList(), { it.toString() }, { hd = it }, Modifier.weight(0.8f), isError = hDate == null, showArrow = false)
                        SelectField(stringResource(R.string.month), hMonth, months, { f.monthNom(it) }, { hm = it.name }, Modifier.weight(1.4f), isError = hDate == null)
                        NumberField(stringResource(R.string.year), hy, { v ->
                            hy = v
                            val y = v.toIntOrNull()
                            if (y != null && M.isHebrewYearSupported(y) && !M.isMonthInYear(hMonth, y)) hm = (if (hMonth == HebrewMonth.ADAR) HebrewMonth.ADAR_II else HebrewMonth.ADAR).name
                        }, Modifier.weight(1f), isError = hDate == null)
                    }
                    if (hDate != null) { resultHebrew = hDate; resultDate = M.toGregorian(hDate) }
                }
            }

            val rh = resultHebrew
            val rd = resultDate
            if (rh != null && rd != null) {
                HighlightCard {
                    if (dir == CalendarKind.GREGORIAN) {
                        Text(stringResource(R.string.conv_hebrew_label), style = MaterialTheme.typography.labelMedium)
                        Text(f.hebrewDate(rh), style = MaterialTheme.typography.headlineSmall)
                        Text(f.gregorianWeekday(if (after) rd.plusDays(1) else rd).substringBefore(','), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(stringResource(R.string.conv_gregorian_label), style = MaterialTheme.typography.labelMedium)
                        Text(f.gregorianWeekday(rd), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.conv_eve_note, f.gregorianDayMonth(rd.minusDays(1))), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(onClick = {
                    nav.navigate(
                        if (dir == CalendarKind.GREGORIAN) EditorRoute(calendar = CalendarKind.GREGORIAN.name, year = rd.year, month = rd.monthValue, day = rd.dayOfMonth, afterSunset = after)
                        else EditorRoute(calendar = CalendarKind.HEBREW.name, year = rh.year, hebrewMonth = rh.month.name, day = rh.day),
                    )
                }, Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.conv_create_event))
                }

                val parts = HebrewParts.of(rh)
                val hints = MitzvahCalculator.mitzvahHints(parts, today)
                val nextYahrzeit = (M.hebrewYearOf(today)..M.hebrewYearOf(today) + 1).firstNotNullOfOrNull { y ->
                    if (y <= rh.year) null else AnniversaryCalculator.hebrewAnniversary(AnniversaryRule.YAHRZEIT, parts, y).let { h -> M.toGregorian(h).takeIf { !it.isBefore(today) }?.let { h to it } }
                }
                SectionCard(title = stringResource(R.string.conv_if_birth)) {
                    if (hints.isEmpty()) Text(stringResource(R.string.conv_mitzvah_passed), style = MaterialTheme.typography.bodyMedium)
                    hints.forEach { h ->
                        Text(stringResource(if (h.type == EventType.BAR_MITZVAH) R.string.hint_bar else R.string.hint_bat, f.hebrewDate(h.hebrewDate), f.gregorianShortWeekday(h.date)), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (nextYahrzeit != null) SectionCard(title = stringResource(R.string.conv_if_death)) {
                    Text(stringResource(R.string.conv_next_yahrzeit, f.hebrewDate(nextYahrzeit.first), f.gregorianShortWeekday(nextYahrzeit.second)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
