package org.ledor.ui.lists

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.data.local.LOCAL_LIST_ID
import org.ledor.data.local.ListEntity
import org.ledor.data.local.ListRole
import org.ledor.data.local.ListState
import org.ledor.data.remote.FailureKind
import org.ledor.data.remote.RpcResult
import org.ledor.data.repo.JoinOutcome
import org.ledor.domain.lists.ListNameRules
import org.ledor.domain.lists.PinGenerator
import org.ledor.domain.lists.PinRules
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.*
import org.ledor.ui.navigation.FeedbackRoute
import org.ledor.ui.navigation.ListCreateRoute
import org.ledor.ui.navigation.ListDetailRoute
import org.ledor.ui.navigation.ListJoinRoute
import org.ledor.ui.navigation.ListsRoute
import org.ledor.ui.theme.ListColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** До публикации в Google Play — страница скачивания на лендинге (BACKLOG B-3). */
const val APP_LINK = "https://kalendar.bothub.chat/"

@Composable
fun rpcErrorText(r: RpcResult<*>): String {
    val ctx = LocalContext.current
    return rpcErrorText(ctx, r)
}

fun rpcErrorText(ctx: Context, r: RpcResult<*>): String = when (r) {
    is RpcResult.Failure -> ctx.getString(when (r.kind) {
        FailureKind.NETWORK -> R.string.err_network
        FailureKind.SERVER -> R.string.err_server
        FailureKind.NOT_CONFIGURED -> R.string.err_cloud_not_configured
    })
    is RpcResult.Error -> when (r.code) {
        "NAME_TAKEN" -> ctx.getString(R.string.err_name_taken)
        "NOT_FOUND" -> ctx.getString(R.string.err_not_found)
        "WRONG_PIN" -> if ((r.attemptsLeft ?: 1) > 0) ctx.getString(R.string.err_wrong_pin, r.attemptsLeft ?: 0)
            else ctx.getString(R.string.err_locked, ((r.retryAfterSec ?: 900) + 59) / 60)
        "LOCKED" -> ctx.getString(R.string.err_locked, ((r.retryAfterSec ?: 900) + 59) / 60)
        "RATE_LIMITED" -> ctx.getString(R.string.err_rate_limited)
        "INVALID_NAME" -> ctx.getString(R.string.err_invalid_name)
        "INVALID_PIN", "PINS_EQUAL" -> ctx.getString(R.string.err_invalid_pin)
        "FORBIDDEN" -> ctx.getString(R.string.err_forbidden)
        "UNAUTHORIZED" -> ctx.getString(R.string.err_unauthorized)
        "LIST_DELETED" -> ctx.getString(R.string.err_list_deleted)
        else -> ctx.getString(R.string.err_server)
    }
    is RpcResult.Ok -> ""
}

@Composable
private fun roleText(l: ListEntity) = stringResource(
    when {
        l.id == LOCAL_LIST_ID -> R.string.role_personal
        l.state == ListState.ACCESS_LOST -> R.string.role_no_access
        l.role == ListRole.EDITOR -> R.string.role_editor
        else -> R.string.role_reader
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopBar(title: String, nav: NavController) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(vm: AppViewModel, nav: NavController) {
    val lists by vm.lists.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var refreshing by remember { mutableStateOf(false) }
    val hasCloud = lists.any { it.list.isCloud }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar(stringResource(R.string.manage_lists), nav) },
    ) { inner ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    val r = vm.container.syncEngine.syncAll()
                    refreshing = false
                    if (r.networkError) Toast.makeText(ctx, R.string.err_network, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.padding(inner).fillMaxSize(),
        ) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate(ListCreateRoute()) }, Modifier.weight(1f)) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.create_list), maxLines = 1)
                        }
                        OutlinedButton(onClick = { nav.navigate(ListJoinRoute()) }, Modifier.weight(1f)) {
                            Text(stringResource(R.string.join_list), maxLines = 1)
                        }
                    }
                }
                if (!vm.container.rpc.isConfigured) item {
                    HighlightCard { Text(stringResource(R.string.err_cloud_not_configured), style = MaterialTheme.typography.bodyMedium) }
                }
                items(lists, key = { it.list.id }) { lw ->
                    val l = lw.list
                    Surface(
                        Modifier.fillMaxWidth().clickable { nav.navigate(ListDetailRoute(l.id)) },
                        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (l.isCloud) AppIcons.Cloud else AppIcons.Phone, null, tint = ListColors[l.colorIndex.mod(8)])
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(listDisplayName(l), style = MaterialTheme.typography.titleMedium)
                                val count = pluralStringResource(R.plurals.events_count, lw.eventCount, lw.eventCount)
                                Text("${roleText(l)} · $count", style = MaterialTheme.typography.bodySmall,
                                    color = if (l.state == ListState.ACCESS_LOST) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                l.lastSyncAt?.let {
                                    Text(stringResource(R.string.synced_at, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(checked = l.notifyEnabled, onCheckedChange = { v -> scope.launch { vm.container.listRepository.setNotifyEnabled(l.id, v) } })
                        }
                    }
                }
                item {
                    val h = settings?.holidays ?: return@item
                    Surface(
                        Modifier.fillMaxWidth().clickable { nav.navigate(org.ledor.ui.navigation.HolidaysRoute) },
                        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            org.ledor.ui.components.MagenDavid(Modifier.size(24.dp), org.ledor.ui.theme.KalendarColors.extra.gold, 1.8.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.holidays_title), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.holidays_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            // Главный выключатель списка: выключен — праздников нет ни в календаре, ни в событиях, ни в напоминаниях.
                            Switch(checked = h.enabled, onCheckedChange = { v -> vm.setHolidays(h.copy(enabled = v)) },
                                modifier = Modifier.testTag("holidays_switch"))
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.lists_hint_notify) + " " + stringResource(R.string.lists_hint_holidays), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (hasCloud) Text(stringResource(R.string.pull_to_sync), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PinField(label: String, value: String, onChange: (String) -> Unit, maxLen: Int, isError: Boolean = false, supporting: String? = null,
             initiallyVisible: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    var visible by rememberSaveable { mutableStateOf(initiallyVisible) }
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(maxLen)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        trailingIcon = {
            Row {
                IconButton(onClick = { visible = !visible }) {
                    Icon(if (visible) AppIcons.VisibilityOff else AppIcons.Visibility, stringResource(if (visible) R.string.hide else R.string.show))
                }
                trailing?.invoke()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListCreateScreen(vm: AppViewModel, nav: NavController, moveLocal: Boolean = false) {
    val scope = rememberCoroutineScope()
    val lists by vm.lists.collectAsStateWithLifecycle()
    val localCount = lists.firstOrNull { it.list.id == LOCAL_LIST_ID }?.eventCount ?: 0
    var name by rememberSaveable { mutableStateOf("") }
    var readPin by rememberSaveable { mutableStateOf(PinGenerator.generate(6)) }
    var masterPin by rememberSaveable { mutableStateOf(PinGenerator.generate(8)) }
    var move by rememberSaveable { mutableStateOf(moveLocal) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var nameStatus by remember { mutableStateOf<Boolean?>(null) }
    val ctx = LocalContext.current

    LaunchedEffect(name) {
        nameStatus = null
        if (!ListNameRules.isValid(name) || !vm.container.rpc.isConfigured) return@LaunchedEffect
        delay(500)
        val r = vm.container.listRepository.checkExists(name)
        if (r is RpcResult.Ok) nameStatus = !r.value
    }
    val nameOk = ListNameRules.isValid(name)
    val readOk = PinRules.isValidReadPin(readPin)
    val masterOk = PinRules.isValidMasterPin(masterPin) && masterPin != readPin

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { SimpleTopBar(stringResource(R.string.create_list), nav) }) { inner ->
        Column(Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!vm.container.rpc.isConfigured) HighlightCard { Text(stringResource(R.string.err_cloud_not_configured)) }
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(ListNameRules.MAX) }, label = { Text(stringResource(R.string.list_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                isError = nameStatus == false,
                supportingText = {
                    Text(when {
                        name.isNotEmpty() && !nameOk -> stringResource(R.string.err_invalid_name)
                        nameStatus == true -> stringResource(R.string.name_free)
                        nameStatus == false -> stringResource(R.string.err_name_taken)
                        else -> ""
                    })
                },
            )
            PinField(stringResource(R.string.read_pin), readPin, { readPin = it }, 8, isError = !readOk, supporting = stringResource(R.string.read_pin_hint),
                trailing = { TextButton(onClick = { readPin = PinGenerator.generate(6) }) { Text(stringResource(R.string.regenerate)) } })
            PinField(stringResource(R.string.master_pin), masterPin, { masterPin = it }, 12, isError = !masterOk, supporting = stringResource(R.string.master_pin_hint),
                trailing = { TextButton(onClick = { masterPin = PinGenerator.generate(8) }) { Text(stringResource(R.string.regenerate)) } })
            Text(stringResource(R.string.pins_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (localCount > 0) SwitchRow(stringResource(R.string.move_local_events), move, { move = it },
                subtitle = pluralStringResource(R.plurals.events_count, localCount, localCount))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        when (val r = vm.container.listRepository.create(name, readPin, masterPin, move)) {
                            is RpcResult.Ok -> {
                                nav.navigate(ListDetailRoute(r.value.id)) { popUpTo(ListsRoute) }
                            }
                            else -> error = rpcErrorText(ctx, r)
                        }
                        busy = false
                    }
                },
                enabled = !busy && nameOk && readOk && masterOk && nameStatus != false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.create)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListJoinScreen(nav: NavController, prefill: String?) {
    val app = LocalContext.current.applicationContext as org.ledor.KalendarApp
    val repo = app.container.listRepository
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val res = LocalResources.current
    var name by rememberSaveable { mutableStateOf(prefill.orEmpty()) }
    var found by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { SimpleTopBar(stringResource(R.string.join_list), nav) }) { inner ->
        Column(Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!repo.cloudConfigured) HighlightCard { Text(stringResource(R.string.err_cloud_not_configured)) }
            Text(stringResource(R.string.join_explain), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(ListNameRules.MAX); found = false; error = null },
                label = { Text(stringResource(R.string.list_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                trailingIcon = if (found) {{ Icon(Icons.Filled.CheckCircle, null, tint = org.ledor.ui.theme.KalendarColors.extra.success) }} else null,
            )
            if (!found) {
                Button(onClick = {
                    busy = true; error = null
                    scope.launch {
                        when (val r = repo.checkExists(name)) {
                            is RpcResult.Ok -> if (r.value) found = true else error = res.getString(R.string.err_not_found)
                            else -> error = rpcErrorText(ctx, r)
                        }
                        busy = false
                    }
                }, enabled = !busy && ListNameRules.isValid(name), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.find))
                }
            } else {
                PinField(stringResource(R.string.pin_code), pin, { pin = it; error = null }, 12, supporting = stringResource(R.string.pin_any_hint), initiallyVisible = false)
                Button(onClick = {
                    busy = true; error = null
                    scope.launch {
                        when (val r = repo.join(name, pin)) {
                            is RpcResult.Ok -> {
                                val j = r.value as JoinOutcome.Joined
                                Toast.makeText(ctx, res.getString(R.string.imported_n, j.imported), Toast.LENGTH_LONG).show()
                                nav.navigate(ListDetailRoute(j.list.id)) { popUpTo(ListsRoute) { inclusive = false } ; launchSingleTop = true }
                            }
                            else -> error = rpcErrorText(ctx, r)
                        }
                        busy = false
                    }
                }, enabled = !busy && PinRules.isValidAnyPin(pin), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.connect))
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

fun buildShareText(ctx: Context, listName: String, pin: String) =
    ctx.getString(R.string.share_message, listName, ctx.getString(R.string.app_name), APP_LINK, listName, pin)

fun shareTo(ctx: Context, target: String?, text: String) {
    val enc = Uri.encode(text)
    val intent = when (target) {
        "telegram" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=${Uri.encode(APP_LINK)}&text=$enc"))
        "whatsapp" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=$enc"))
        else -> null
    }
    try {
        if (intent != null) { ctx.startActivity(intent); return }
    } catch (_: Exception) { }
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    ctx.startActivity(Intent.createChooser(send, null))
}

private fun copy(ctx: Context, text: String) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("pin", text))
    Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(vm: AppViewModel, nav: NavController, id: String) {
    val list by remember(id) { vm.container.listRepository.observe(id) }.collectAsStateWithLifecycle(null)
    val lists by vm.lists.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val res = LocalResources.current
    var confirmDisconnect by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var deletePin by remember { mutableStateOf("") }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var pinsDialog by remember { mutableStateOf(false) }
    var shareMaster by remember { mutableStateOf(false) }
    var masterWarn by remember { mutableStateOf(false) }
    var moveDialog by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var gone by remember { mutableStateOf(false) }

    val l = list
    LaunchedEffect(l) { if (l == null && gone) nav.popBackStack() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { SimpleTopBar(l?.let { listDisplayName(it) } ?: "", nav) }) { inner ->
        if (l == null) return@Scaffold
        gone = true
        val count = lists.firstOrNull { it.list.id == l.id }?.eventCount ?: 0
        Column(Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (l.isCloud) AppIcons.Cloud else AppIcons.Phone, null, tint = ListColors[l.colorIndex.mod(8)])
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(roleText(l), style = MaterialTheme.typography.titleSmall)
                        Text(pluralStringResource(R.plurals.events_count, count, count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SwitchRow(stringResource(R.string.list_notifications), l.notifyEnabled, { v -> scope.launch { vm.container.listRepository.setNotifyEnabled(l.id, v) } })
                if (l.state == ListState.ACCESS_LOST) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.err_unauthorized), color = MaterialTheme.colorScheme.error)
                }
            }

            if (!l.isCloud) {
                Text(stringResource(R.string.personal_list_explain), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val targets = lists.filter { it.list.isCloud && it.list.canEdit }
                // B-27: кнопка есть всегда, когда в личном списке есть события. Облачных списков нет — сразу «Создать список»
                // с включённым переносом; есть — выбор списка или новый.
                if (count > 0) OutlinedButton(onClick = { if (targets.isEmpty()) nav.navigate(ListCreateRoute(moveLocal = true)) else moveDialog = true }) {
                    Text(stringResource(R.string.move_to_cloud))
                }
                if (moveDialog) AlertDialog(
                    onDismissRequest = { moveDialog = false },
                    title = { Text(stringResource(R.string.move_to_cloud)) },
                    text = {
                        Column { targets.forEach { t ->
                            TextButton(onClick = {
                                moveDialog = false
                                scope.launch {
                                    vm.container.listRepository.moveLocalEventsTo(t.list.id)
                                    nav.navigate(ListDetailRoute(t.list.id)) { popUpTo(ListsRoute) }
                                }
                            }) { Text(t.list.name) }
                        }
                        TextButton(onClick = { moveDialog = false; nav.navigate(ListCreateRoute(moveLocal = true)) }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.create_list))
                        } }
                    },
                    confirmButton = {}, dismissButton = { TextButton(onClick = { moveDialog = false }) { Text(stringResource(R.string.cancel)) } },
                )
                return@Column
            }

            SectionCard(title = stringResource(R.string.pin_codes)) {
                l.readPin?.let { pin ->
                    PinField(stringResource(R.string.read_pin), pin, {}, 8, initiallyVisible = false,
                        trailing = { IconButton(onClick = { copy(ctx, pin) }) { Icon(AppIcons.Copy, stringResource(R.string.copy)) } })
                }
                l.masterPin?.let { pin ->
                    PinField(stringResource(R.string.master_pin), pin, {}, 12, initiallyVisible = false,
                        trailing = { IconButton(onClick = { copy(ctx, pin) }) { Icon(AppIcons.Copy, stringResource(R.string.copy)) } })
                }
                if (l.role == ListRole.READER) Text(stringResource(R.string.reader_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (l.role == ListRole.EDITOR) OutlinedButton(onClick = { pinsDialog = true }, modifier = Modifier.fillMaxWidth().testTag("change_pins")) {
                    Text(stringResource(R.string.change_pins))
                }
            }

            SectionCard(title = stringResource(R.string.share)) {
                if (l.masterPin != null) SwitchRow(stringResource(R.string.share_edit_rights), shareMaster, { v -> if (v) masterWarn = true else shareMaster = false })
                val pin = if (shareMaster) l.masterPin else l.readPin
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { pin?.let { shareTo(ctx, "telegram", buildShareText(ctx, l.name, it)) } }, enabled = pin != null, modifier = Modifier.weight(1f)) { Text("Telegram") }
                    FilledTonalButton(onClick = { pin?.let { shareTo(ctx, "whatsapp", buildShareText(ctx, l.name, it)) } }, enabled = pin != null, modifier = Modifier.weight(1f)) { Text("WhatsApp") }
                }
                OutlinedButton(onClick = { pin?.let { shareTo(ctx, null, buildShareText(ctx, l.name, it)) } }, enabled = pin != null, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.share_other))
                }
            }
            if (masterWarn) ConfirmDialog(stringResource(R.string.share_edit_rights), stringResource(R.string.master_share_warning), stringResource(R.string.ok),
                onConfirm = { masterWarn = false; shareMaster = true }, onDismiss = { masterWarn = false })

            SectionCard {
                OutlinedButton(onClick = {
                    syncing = true
                    scope.launch {
                        val r = vm.container.syncEngine.syncList(l.id)
                        syncing = false
                        Toast.makeText(ctx, if (r.ok > 0) R.string.synced else if (r.networkError) R.string.err_network else R.string.err_server, Toast.LENGTH_SHORT).show()
                    }
                }, enabled = !syncing, modifier = Modifier.fillMaxWidth()) {
                    Icon(AppIcons.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.sync_now))
                }
                if (l.role == ListRole.READER) OutlinedButton(onClick = { nav.navigate(ListJoinRoute(l.name)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.enter_master_pin))
                }
                OutlinedButton(onClick = { confirmDisconnect = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.disconnect_list)) }
                if (l.role == ListRole.EDITOR) OutlinedButton(
                    onClick = { deletePin = l.masterPin.orEmpty(); deleteError = null; deleteDialog = true }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete_from_cloud)) }
                TextButton(onClick = { nav.navigate(FeedbackRoute(res.getString(R.string.report_subject, l.name))) }) {
                    Text(stringResource(R.string.report_list))
                }
            }
        }

        if (pinsDialog) ChangePinsDialog(l, onDone = {
            pinsDialog = false
            Toast.makeText(ctx, R.string.pins_changed, Toast.LENGTH_LONG).show()
        }, onDismiss = { pinsDialog = false }) { cur, read, master -> vm.container.listRepository.changePins(l.id, cur, read, master) }
        if (confirmDisconnect) ConfirmDialog(stringResource(R.string.disconnect_list), stringResource(R.string.disconnect_text), stringResource(R.string.disconnect),
            onConfirm = { confirmDisconnect = false; scope.launch { vm.container.listRepository.disconnect(l.id) } },
            onDismiss = { confirmDisconnect = false })
        if (deleteDialog) ConfirmDialog(
            stringResource(R.string.delete_from_cloud), stringResource(R.string.delete_cloud_text), stringResource(R.string.delete),
            onConfirm = {
                scope.launch {
                    val r = vm.container.listRepository.deleteFromCloud(l.id, deletePin)
                    if (r is RpcResult.Ok) deleteDialog = false else deleteError = rpcErrorText(ctx, r)
                }
            },
            onDismiss = { deleteDialog = false },
            confirmEnabled = PinRules.isValidMasterPin(deletePin),
            content = {
                PinField(stringResource(R.string.master_pin), deletePin, { deletePin = it }, 12, isError = deleteError != null, supporting = deleteError, initiallyVisible = false)
            },
        )
    }
}

/** Введённые в окне смены ПИН-кодов значения (B-40). Отдельно от окна — чтобы проверять без всплывающего окна. */
class ChangePinsState(readPin: String, masterPin: String, val masterStored: Boolean) {
    var read by mutableStateOf(readPin)
    var master by mutableStateOf(masterPin)
    var current by mutableStateOf(if (masterStored) masterPin else "")
    val readOk get() = PinRules.isValidReadPin(read)
    val masterOk get() = PinRules.isValidMasterPin(master) && master != read
    val canSave get() = readOk && masterOk && PinRules.isValidMasterPin(current)
}

@Composable
fun ChangePinsFields(state: ChangePinsState, error: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PinField(stringResource(R.string.read_pin), state.read, { state.read = it }, 8, isError = !state.readOk, supporting = stringResource(R.string.read_pin_hint))
        PinField(stringResource(R.string.master_pin), state.master, { state.master = it }, 12, isError = !state.masterOk,
            supporting = stringResource(R.string.master_pin_hint), initiallyVisible = false)
        if (!state.masterStored) PinField(stringResource(R.string.current_master_pin), state.current, { state.current = it }, 12, initiallyVisible = false)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

/**
 * B-40: смена ПИН-кодов списка. ПИН-код для чтения можно сделать простым и запоминающимся (например, для общины),
 * Мастер-ПИН — по-прежнему 6–12 цифр. Если Мастер-ПИН на этом телефоне не сохранён, его спрашивают отдельно.
 * Сервер при смене Мастер-ПИНа делает остальных редакторов читателями (на случай, если старый код утёк).
 */
@Composable
fun ChangePinsDialog(
    l: ListEntity,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    change: suspend (current: String, read: String, master: String) -> RpcResult<Unit>,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { ChangePinsState(l.readPin.orEmpty(), l.masterPin.orEmpty(), l.masterPin != null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    ConfirmDialog(
        stringResource(R.string.change_pins), stringResource(R.string.change_pins_text), stringResource(R.string.save),
        destructive = false,
        confirmEnabled = state.canSave && !busy,
        onConfirm = {
            busy = true; error = null
            scope.launch {
                val r = change(state.current, state.read, state.master)
                busy = false
                if (r is RpcResult.Ok) onDone() else error = rpcErrorText(ctx, r)
            }
        },
        onDismiss = onDismiss,
        content = { ChangePinsFields(state, error) },
    )
}
