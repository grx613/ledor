package org.ledor.ui.components

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.ledor.KalendarApp
import org.ledor.R
import org.ledor.domain.reminder.SunsetHints
import org.ledor.domain.reminder.SunsetVerdict
import org.ledor.ui.settings.cityName
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Подсказка «до или после захода» (BACKLOG B-24): по местному времени и городу события считает заход солнца
 * в тот день и сама выставляет переключатель. В сумерках ничего не меняет — показывает, что дата под сомнением.
 * Город по умолчанию — из настроек напоминаний.
 */
@Composable
fun SunsetHint(date: LocalDate?, onResult: (afterSunset: Boolean) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    if (!open) {
        TextButton(onClick = { open = true }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.sunset_hint_open)) }
        return
    }
    val ctx = LocalContext.current
    val container = (ctx.applicationContext as KalendarApp).container
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
    var minutes by rememberSaveable { mutableStateOf(-1) }
    var chosenCity by rememberSaveable { mutableStateOf<String?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val is24 = DateFormat.is24HourFormat(ctx)
    val fmt = remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a") }
    val city = container.cityCatalog.byId(chosenCity ?: settings?.cityId)
    val time = if (minutes >= 0) LocalTime.of(minutes / 60, minutes % 60) else null
    val complete = date != null && time != null && city != null
    val hint = remember(date, time, city) { if (date != null && time != null && city != null) SunsetHints.evaluate(date, time, city) else null }
    LaunchedEffect(hint) {
        when (hint?.verdict) {
            SunsetVerdict.BEFORE -> onResult(false)
            SunsetVerdict.AFTER -> onResult(true)
            else -> {}
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val start = time ?: LocalTime.NOON
                    TimePickerDialog(ctx, { _, h, m -> minutes = h * 60 + m }, start.hour, start.minute, is24).show()
                },
                modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Text(
                    if (time != null) stringResource(R.string.sunset_hint_time, fmt.format(time)) else stringResource(R.string.sunset_hint_set_time),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = { picking = !picking }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp)) {
                Text(
                    if (city != null) stringResource(R.string.sunset_hint_city, cityName(city)) else stringResource(R.string.sunset_hint_set_city),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (picking) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.city_search)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
            )
            container.cityCatalog.search(query).take(5).forEach { c ->
                Text(
                    cityName(c),
                    Modifier.fillMaxWidth().clickable { chosenCity = c.id; picking = false; query = "" }.padding(horizontal = 4.dp, vertical = 10.dp),
                )
            }
        }
        if (complete && hint == null) {
            Text(stringResource(R.string.sunset_hint_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        } else if (hint != null && time != null) {
            val t = fmt.format(time)
            Text(stringResource(R.string.sunset_hint_sunset, fmt.format(hint.sunset)), style = MaterialTheme.typography.bodySmall)
            when (hint.verdict) {
                SunsetVerdict.BEFORE -> Text(stringResource(R.string.sunset_hint_before, t), style = MaterialTheme.typography.bodyMedium)
                SunsetVerdict.AFTER -> Text(stringResource(R.string.sunset_hint_after, t), style = MaterialTheme.typography.bodyMedium)
                SunsetVerdict.TWILIGHT -> Text(
                    stringResource(R.string.sunset_hint_twilight, t, fmt.format(hint.nightfall)),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(stringResource(R.string.sunset_hint_local), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
