package org.ledor.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.ledor.R
import org.ledor.reminders.HealthIssue
import org.ledor.reminders.ReminderHealthChecker
import org.ledor.ui.theme.KalendarColors

fun Context.launchFirst(intents: List<Intent>) {
    for (i in intents) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: ActivityNotFoundException) { } catch (_: SecurityException) { }
    }
}

private fun HealthIssue.titleRes() = when (this) {
    HealthIssue.NOTIFICATIONS_OFF -> R.string.health_notif_title
    HealthIssue.EXACT_ALARMS_DENIED -> R.string.health_exact_title
    HealthIssue.BACKGROUND_RESTRICTED -> R.string.health_bg_title
    HealthIssue.BATTERY_OPTIMIZED -> R.string.health_battery_title
}

private fun HealthIssue.textRes() = when (this) {
    HealthIssue.NOTIFICATIONS_OFF -> R.string.health_notif_text
    HealthIssue.EXACT_ALARMS_DENIED -> R.string.health_exact_text
    HealthIssue.BACKGROUND_RESTRICTED -> R.string.health_bg_text
    HealthIssue.BATTERY_OPTIMIZED -> R.string.health_battery_text
}

/** Кнопка «Открыть настройку» с правильной цепочкой откатов. */
@Composable
fun rememberHealthFixer(checker: ReminderHealthChecker, onDone: () -> Unit): (HealthIssue) -> Unit {
    val ctx = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) ctx.launchFirst(checker.fixIntents(HealthIssue.NOTIFICATIONS_OFF))
        onDone()
    }
    return { issue ->
        if (issue == HealthIssue.NOTIFICATIONS_OFF && Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else ctx.launchFirst(checker.fixIntents(issue))
    }
}

/** Список всех четырёх проверок со статусом (для листа «Исправить» и настроек). */
@Composable
fun HealthChecklist(issues: List<HealthIssue>, checker: ReminderHealthChecker, onRecheck: () -> Unit, showAll: Boolean) {
    val fix = rememberHealthFixer(checker, onRecheck)
    val shown = if (showAll) HealthIssue.entries else issues
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (shown.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = KalendarColors.extra.success)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.health_all_ok), style = MaterialTheme.typography.titleSmall)
            }
        }
        for (issue in shown) {
            val bad = issue in issues
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (bad) Icons.Filled.Warning else Icons.Filled.CheckCircle, null,
                    tint = if (bad) MaterialTheme.colorScheme.error else KalendarColors.extra.success,
                    modifier = Modifier.padding(top = 2.dp).size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(issue.titleRes()), style = MaterialTheme.typography.titleSmall)
                    if (bad) {
                        Text(stringResource(issue.textRes()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (issue == HealthIssue.BATTERY_OPTIMIZED || issue == HealthIssue.BACKGROUND_RESTRICTED) {
                            checker.manufacturerHint()?.let {
                                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        FilledTonalButton(onClick = { fix(issue) }) { Text(stringResource(R.string.health_open_setting)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthSheet(issues: List<HealthIssue>, checker: ReminderHealthChecker, onRecheck: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.health_sheet_title), style = MaterialTheme.typography.titleLarge)
            HealthChecklist(issues, checker, onRecheck, showAll = false)
        }
    }
}
