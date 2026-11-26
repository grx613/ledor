package org.ledor.ui.editor

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath as M
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.*
import org.ledor.ui.AppViewModel
import org.ledor.ui.components.*
import org.ledor.ui.navigation.EditorRoute
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorScreen(app: AppViewModel, nav: NavController, route: EditorRoute) {
    val vm: EventEditorViewModel = viewModel()
    LaunchedEffect(route) { vm.init(route) }
    val s by vm.state.collectAsStateWithLifecycle()
    val settings by app.settings.collectAsStateWithLifecycle()
    val today by app.today.collectAsStateWithLifecycle()
    val lists by app.lists.collectAsStateWithLifecycle()
    val f = rememberFormatters()
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(s.saved) { if (s.saved) nav.popBackStack() }
    BackHandler(enabled = s.dirty) { confirmLeave = true }
    if (!s.loaded) return

    val errors = vm.errors(s)
    val showErr = s.attempted
    val ed = s.eventDate()
    val policy = settings?.adarPolicy ?: YahrzeitAdarPolicy.ADAR_I
    val editableLists = lists.filter { it.list.canEdit }
    val personalName = stringResource(R.string.list_personal)

    if (confirmDelete) ConfirmDialog(stringResource(R.string.delete_event_q, s.title), null, stringResource(R.string.delete),
        onConfirm = { confirmDelete = false; vm.delete() }, onDismiss = { confirmDelete = false })
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text(stringResource(R.string.save_changes_q)) },
        confirmButton = { TextButton(onClick = { confirmLeave = false; vm.save() }) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmLeave = false; nav.popBackStack() }) { Text(stringResource(R.string.dont_save)) }
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) }
            }
        },
    )

    fun doSave() {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && errors.isEmpty()) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        vm.save()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (s.id == null) R.string.new_event else R.string.edit_event)) },
                navigationIcon = {
                    IconButton(onClick = { if (s.dirty) confirmLeave = true else nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                actions = {
                    if (s.id != null && !s.readOnly) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            if (!s.readOnly) Surface(color = MaterialTheme.colorScheme.background) {
                Button(onClick = ::doSave, modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(52.dp)) {
                    Text(stringResource(R.string.save))
                }
            }
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s.readOnly) HighlightCard { Text(stringResource(R.string.read_only_list)) }

            SectionCard {
                OutlinedTextField(
                    value = s.title, onValueChange = vm::setTitle, label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                    isError = showErr && (ValidationError.TITLE_EMPTY in errors),
                    supportingText = if (showErr && ValidationError.TITLE_EMPTY in errors) {{ Text(stringResource(R.string.err_title_empty)) }} else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    enabled = !s.readOnly,
                )
                SelectField(
                    label = stringResource(R.string.field_list),
                    selected = editableLists.firstOrNull { it.list.id == s.listId }?.list,
                    options = editableLists.map { it.list },
                    text = { l -> if (l.id == org.ledor.data.local.LOCAL_LIST_ID) personalName else l.name },
                    onSelect = { vm.setList(it.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !s.readOnly,
                )
            }

            SectionCard(title = stringResource(R.string.field_type)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EventType.entries.forEach { t ->
                        FilterChip(
                            selected = s.type == t, onClick = { vm.setType(t) }, enabled = !s.readOnly,
                            label = { Text(f.typeName(t)) },
                            leadingIcon = { EventTypeGlyph(t, org.ledor.ui.theme.KalendarColors.extra.forType(t), Modifier.size(18.dp)) },
                        )
                    }
                }
                if (s.type == EventType.OTHER) {
                    OutlinedTextField(
                        value = s.customLabel, onValueChange = vm::setCustomLabel, label = { Text(stringResource(R.string.field_custom_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                        isError = showErr && ValidationError.CUSTOM_LABEL_REQUIRED in errors, enabled = !s.readOnly,
                    )
                }
                SwitchRow(
                    stringResource(R.string.field_recurring), s.recurring, vm::setRecurring,
                    subtitle = stringResource(if (s.recurring) R.string.recurring_yes else R.string.recurring_no), enabled = !s.readOnly,
                )
            }

            SectionCard(title = stringResource(R.string.field_date)) {
                Segmented(
                    listOf(CalendarKind.GREGORIAN, CalendarKind.HEBREW), s.input,
                    label = { Text(stringResource(if (it == CalendarKind.GREGORIAN) R.string.gregorian_date else R.string.hebrew_date), maxLines = 1) },
                    onSelect = vm::setInput,
                )
                val dateErr = showErr && (ValidationError.DATE_INVALID in errors || ValidationError.YEAR_OUT_OF_RANGE in errors)
                if (s.input == CalendarKind.GREGORIAN) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectField(stringResource(R.string.day), s.gDay, (1..31).toList(), { it.toString() }, vm::setGDay, Modifier.weight(0.8f), isError = dateErr, showArrow = false)
                        SelectField(stringResource(R.string.month), s.gMonth, (1..12).toList(), { f.gregorianMonthName(it) }, vm::setGMonth, Modifier.weight(1.4f))
                        NumberField(stringResource(R.string.year), if (s.gYearUnknown) "" else s.gYear, vm::setGYear, Modifier.weight(1f), enabled = !s.gYearUnknown, isError = dateErr)
                    }
                    CheckRow(stringResource(R.string.year_unknown), s.gYearUnknown, vm::setGYearUnknown)
                    if (s.gYearInt != null) {
                        SunsetToggle(s.afterSunset, vm::setAfterSunset)
                        if (!s.readOnly) SunsetHint(runCatching { java.time.LocalDate.of(s.gYearInt!!, s.gMonth, s.gDay) }.getOrNull(), vm::setAfterSunset)
                    }
                } else {
                    val hy = s.hYearInt?.takeIf { M.isHebrewYearSupported(it) }
                    val months = hy?.let { M.monthsOfYear(it) } ?: HebrewMonth.ORDER_WITHOUT_YEAR
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectField(stringResource(R.string.day), s.hDay, (1..30).toList(), { it.toString() }, vm::setHDay, Modifier.weight(0.8f), isError = dateErr, showArrow = false)
                        SelectField(stringResource(R.string.month), s.hMonth, months, { f.monthNom(it) }, vm::setHMonth, Modifier.weight(1.4f), isError = dateErr)
                        NumberField(stringResource(R.string.year), if (s.hYearUnknown) "" else s.hYear, vm::setHYear, Modifier.weight(1f), enabled = !s.hYearUnknown, isError = dateErr)
                    }
                    CheckRow(stringResource(R.string.year_unknown), s.hYearUnknown, vm::setHYearUnknown)
                    if (hy != null && M.isMonthInYear(s.hMonth, hy) && s.hDay > M.daysInMonth(hy, s.hMonth)) {
                        Text(stringResource(R.string.err_month_days, M.daysInMonth(hy, s.hMonth)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (dateErr) Text(stringResource(R.string.err_date_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (showErr && ValidationError.ONE_OFF_NEEDS_YEAR in errors) Text(stringResource(R.string.err_one_off_year), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                HighlightCard {
                    when {
                        s.input == CalendarKind.GREGORIAN && ed.hebrew?.year != null -> {
                            Text(stringResource(R.string.conv_hebrew_label), style = MaterialTheme.typography.labelMedium)
                            Text(f.hebrewParts(ed.hebrew!!), style = MaterialTheme.typography.titleLarge)
                        }
                        s.input == CalendarKind.HEBREW && ed.gregorian?.year != null -> {
                            val g = ed.gregorian!!.toLocalDateOrNull()!!
                            Text(stringResource(R.string.conv_gregorian_label), style = MaterialTheme.typography.labelMedium)
                            Text(f.gregorianWeekday(g), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.conv_eve_note, f.gregorianDayMonth(g.minusDays(1))), style = MaterialTheme.typography.bodySmall)
                        }
                        else -> Text(stringResource(R.string.conv_need_year), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (s.recurring) SectionCard(title = stringResource(R.string.observe_title)) {
                val yearKnown = s.inputYearKnown
                val hebText = ed.hebrew?.let { f.hebrewDayMonth(it.day, it.month) }
                val gregText = ed.gregorian?.let { f.gregorianDayMonth(java.time.LocalDate.of(2000, it.month, it.day)) }
                val both = s.effectiveBoth()
                val eff = s.effectiveObserve()
                RadioRow(
                    stringResource(R.string.observe_hebrew) + (hebText?.let { " — $it" } ?: ""), !both && eff == CalendarKind.HEBREW,
                    { vm.setObserve(CalendarKind.HEBREW) },
                    enabled = !s.readOnly && (yearKnown || s.input == CalendarKind.HEBREW),
                    subtitle = if (!yearKnown && s.input != CalendarKind.HEBREW) stringResource(R.string.observe_needs_year) else null,
                )
                RadioRow(
                    stringResource(R.string.observe_gregorian) + (gregText?.let { " — $it" } ?: ""), !both && eff == CalendarKind.GREGORIAN,
                    { vm.setObserve(CalendarKind.GREGORIAN) },
                    enabled = !s.readOnly && (yearKnown || s.input == CalendarKind.GREGORIAN),
                    subtitle = if (!yearKnown && s.input != CalendarKind.GREGORIAN) stringResource(R.string.observe_needs_year) else null,
                )
                RadioRow(
                    stringResource(R.string.observe_both), both,
                    { vm.setObserveBoth() },
                    enabled = !s.readOnly && yearKnown,
                    subtitle = stringResource(if (yearKnown) R.string.observe_both_hint else R.string.observe_needs_year),
                )
            }

            val previews = remember(s, policy, today) {
                if (EventValidator.validateDate(ed).isEmpty() && !s.yearMissingForOneOff())
                    OccurrenceCalculator.nextEach(s.toEvent().copy(id = "preview"), today, policy) else emptyList()
            }
            previews.forEach { preview ->
                val days = ChronoUnit.DAYS.between(today, preview.date)
                val age = f.agePhrase(s.type, preview.yearsSince)
                Text(
                    stringResource(R.string.next_occurrence, f.occurrenceDatesInline(preview), f.relativeDays(days)) +
                        (age?.let { ", ${it.replaceFirstChar { c -> c.lowercase() }}" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (s.type == EventType.BIRTHDAY && ed.hebrew?.year != null && !s.readOnly) {
                val hints = remember(ed.hebrew, today) { MitzvahCalculator.mitzvahHints(ed.hebrew!!, today) }
                hints.forEach { h ->
                    HighlightCard {
                        Text(
                            stringResource(if (h.type == EventType.BAR_MITZVAH) R.string.hint_bar else R.string.hint_bat,
                                f.hebrewDate(h.hebrewDate), f.gregorianShortWeekday(h.date)),
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                        )
                        TextButton(onClick = {
                            if (s.title.isBlank()) Toast.makeText(ctx, R.string.err_title_empty, Toast.LENGTH_SHORT).show()
                            else vm.addMitzvah(h) { Toast.makeText(ctx, R.string.event_added, Toast.LENGTH_SHORT).show() }
                        }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.add_as_event)) }
                    }
                }
            }

            SectionCard {
                OutlinedTextField(
                    value = s.note, onValueChange = vm::setNote, label = { Text(stringResource(R.string.field_note)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, shape = MaterialTheme.shapes.small, enabled = !s.readOnly,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(x = (-12).dp)) {
        Checkbox(checked, onChange)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
