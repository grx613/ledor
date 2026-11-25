package org.ledor.ui.holidays

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.data.holiday.HOLIDAYS_LIST_ID
import org.ledor.data.holiday.HolidayEvents
import org.ledor.domain.holiday.CandleLightingCalculator
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.EventRow
import org.ledor.ui.components.SectionCard
import org.ledor.ui.components.SwitchRow
import org.ledor.ui.navigation.CityPickerRoute
import org.ledor.ui.settings.cityName

/** Встроенный список «Еврейские праздники» (B-25): три переключателя и ближайшие даты на год вперёд. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysScreen(vm: AppViewModel, nav: NavController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val upcoming by vm.upcoming.collectAsStateWithLifecycle()
    val s = settings ?: return
    val h = s.holidays
    val city = remember(s.cityId) { vm.container.cityCatalog.byId(s.cityId) }
    val items = upcoming.orEmpty().filter { it.event.listId == HOLIDAYS_LIST_ID }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.holidays_title)) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        LazyColumn(Modifier.padding(inner).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SwitchRow(stringResource(R.string.holidays_show), h.enabled, { vm.setHolidays(h.copy(enabled = it)) })
                    SwitchRow(stringResource(R.string.holidays_notify), h.enabled && h.notify, { vm.setHolidays(h.copy(notify = it)) },
                        subtitle = stringResource(R.string.holidays_notify_hint), enabled = h.enabled)
                    SwitchRow(stringResource(R.string.holidays_shabbat), h.enabled && h.shabbat && city != null, { vm.setHolidays(h.copy(shabbat = it)) },
                        subtitle = stringResource(R.string.holidays_shabbat_hint), enabled = h.enabled && city != null)
                    if (city == null) {
                        Text(stringResource(R.string.holidays_city_needed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { nav.navigate(CityPickerRoute) }) { Text(stringResource(R.string.holidays_choose_city)) }
                    } else {
                        val region = stringResource(if (HolidayEvents.inIsrael(city)) R.string.holidays_region_israel else R.string.holidays_region_diaspora)
                        val rule = stringResource(R.string.holidays_candles_rule, CandleLightingCalculator.minutesBeforeSunset(city), cityName(city))
                        Text("$region $rule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (h.enabled && items.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.holidays_upcoming), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(items, key = { it.event.id }) { u ->
                    EventRow(u.event, u.occurrence, u.daysUntil, null, onClick = null, showList = false)
                }
            }
        }
    }
}
