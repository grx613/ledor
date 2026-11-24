package org.ledor.ui.settings

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.data.prefs.ThemeMode
import org.ledor.domain.event.YahrzeitAdarPolicy
import org.ledor.domain.reminder.EveMode
import org.ledor.domain.reminder.ReminderSettings
import org.ledor.reminders.previewEveTrigger
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, nav: NavController) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
private fun timeText(t: LocalTime): String {
    val ctx = LocalContext.current
    return DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm a").format(t)
}

@Composable
private fun instantTime(i: Instant): String = timeText(i.atZone(ZoneId.systemDefault()).toLocalTime())

@Composable
private fun TimeRow(title: String, time: LocalTime, enabled: Boolean, onPick: (LocalTime) -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) {
            TimePickerDialog(ctx, { _, h, m -> onPick(LocalTime.of(h, m)) }, time.hour, time.minute, DateFormat.is24HourFormat(ctx)).show()
        }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(timeText(time), style = MaterialTheme.typography.titleMedium, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavController) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val health by vm.health.collectAsStateWithLifecycle()
    val s = settings ?: return
    val store = vm.container.settingsStore
    val scope = rememberCoroutineScope()
    val r = s.reminders
    fun setR(n: ReminderSettings) = scope.launch { store.setReminders(n) }
    val city = vm.container.cityCatalog.byId(s.cityId)
    val ctx = LocalContext.current
    val preview = remember(s.cityId, r) { previewEveTrigger(vm.container.reminderScheduler, s.cityId, r) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { BackTopBar(stringResource(R.string.settings), nav) }) { inner ->
        Column(Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard(title = stringResource(R.string.set_eve_title)) {
                SwitchRow(stringResource(R.string.set_enabled), r.eveEnabled, { setR(r.copy(eveEnabled = it)) })
                RadioRow(stringResource(R.string.set_eve_before_sunset), r.eveMode == EveMode.BEFORE_SUNSET, { setR(r.copy(eveMode = EveMode.BEFORE_SUNSET)) }, enabled = r.eveEnabled)
                if (r.eveMode == EveMode.BEFORE_SUNSET) {
                    val h = r.eveMinutesBeforeSunset / 60
                    val m = r.eveMinutesBeforeSunset % 60
                    Text(stringResource(R.string.set_before_sunset_value, h, m), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = r.eveMinutesBeforeSunset.toFloat(), onValueChange = { setR(r.copy(eveMinutesBeforeSunset = (it / 30).toInt() * 30)) },
                        valueRange = 0f..360f, steps = 11, enabled = r.eveEnabled,
                    )
                }
                RadioRow(stringResource(R.string.set_eve_fixed), r.eveMode == EveMode.FIXED_TIME, { setR(r.copy(eveMode = EveMode.FIXED_TIME)) }, enabled = r.eveEnabled)
                if (r.eveMode == EveMode.FIXED_TIME) TimeRow(stringResource(R.string.set_time), r.eveFixedTime, r.eveEnabled) { setR(r.copy(eveFixedTime = it)) }
            }
            SectionCard(title = stringResource(R.string.set_day_title)) {
                SwitchRow(stringResource(R.string.set_enabled), r.dayEnabled, { setR(r.copy(dayEnabled = it)) })
                TimeRow(stringResource(R.string.set_time), r.dayTime, r.dayEnabled) { setR(r.copy(dayTime = it)) }
            }
            SectionCard(title = stringResource(R.string.set_city_title)) {
                Row(Modifier.fillMaxWidth().clickable { nav.navigate(org.ledor.ui.navigation.CityPickerRoute) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(city?.let { cityName(it) } ?: stringResource(R.string.city_none), style = MaterialTheme.typography.bodyLarge)
                        if (city == null) Text(stringResource(R.string.city_none_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Filled.KeyboardArrowRight, null)
                }
                if (r.eveEnabled) Text(
                    stringResource(R.string.set_preview, instantTime(preview.first), instantTime(preview.second)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SectionCard(title = stringResource(R.string.set_health_title)) {
                HealthChecklist(health, vm.container.healthChecker, vm::onResume, showAll = true)
                SwitchRow(stringResource(R.string.set_health_warnings), s.healthWarningsEnabled, { v -> scope.launch { store.setHealthWarnings(v) } })
                OutlinedButton(onClick = { vm.container.notifications.showTest() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.set_test_notification)) }
            }
            SectionCard(title = stringResource(R.string.set_language)) {
                val activity = androidx.activity.compose.LocalActivity.current ?: return@SectionCard
                val currentLang = remember { org.ledor.ui.locale.AppLocale.current(activity) }
                RadioRow(stringResource(R.string.lang_system), currentLang.isEmpty(), { org.ledor.ui.locale.AppLocale.set(activity, "") })
                org.ledor.ui.locale.AppLocale.SUPPORTED.forEach { (tag, name) ->
                    RadioRow(name, currentLang == tag, { org.ledor.ui.locale.AppLocale.set(activity, tag) })
                }
            }
            SectionCard(title = stringResource(R.string.set_view_title)) {
                Text(stringResource(R.string.set_week_start), style = MaterialTheme.typography.bodyMedium)
                Segmented(listOf(true, false), s.weekStartsOnSunday,
                    label = { Text(stringResource(if (it) R.string.sunday else R.string.monday)) },
                    onSelect = { v -> scope.launch { store.setWeekStartsOnSunday(v) } })
                Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.bodyMedium)
                Segmented(ThemeMode.entries.toList(), s.themeMode,
                    label = { Text(stringResource(when (it) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }), maxLines = 1) },
                    onSelect = { v -> scope.launch { store.setTheme(v) } })
            }
            SectionCard(title = stringResource(R.string.set_adar_title)) {
                RadioRow(stringResource(R.string.set_adar_i), s.adarPolicy == YahrzeitAdarPolicy.ADAR_I, { scope.launch { store.setAdarPolicy(YahrzeitAdarPolicy.ADAR_I) } })
                RadioRow(stringResource(R.string.set_adar_ii), s.adarPolicy == YahrzeitAdarPolicy.ADAR_II, { scope.launch { store.setAdarPolicy(YahrzeitAdarPolicy.ADAR_II) } })
                Text(stringResource(R.string.set_adar_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun cityName(c: org.ledor.domain.reminder.City): String =
    when (stringResource(R.string.locale_tag)) { "ru" -> c.nameRu; "he", "yi" -> c.nameHe; else -> c.nameEn }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityPickerScreen(vm: AppViewModel, nav: NavController) {
    var q by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val catalog = vm.container.cityCatalog
    val results = remember(q) { catalog.search(q) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { BackTopBar(stringResource(R.string.set_city_title), nav) }) { inner ->
        Column(Modifier.padding(inner)) {
            OutlinedTextField(
                value = q, onValueChange = { q = it }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text(stringResource(R.string.city_search)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.small,
            )
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.city_none)) },
                        supportingContent = { Text(stringResource(R.string.city_none_hint)) },
                        modifier = Modifier.clickable { scope.launch { vm.container.settingsStore.setCity(null); nav.popBackStack() } },
                    )
                }
                items(results, key = { it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(cityName(c)) },
                        supportingContent = { Text("${c.nameEn} · ${c.nameHe}") },
                        modifier = Modifier.clickable { scope.launch { vm.container.settingsStore.setCity(c.id); nav.popBackStack() } },
                    )
                }
            }
        }
    }
}
