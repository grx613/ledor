package org.ledor.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.ledor.KalendarApp
import org.ledor.data.local.LOCAL_LIST_ID
import org.ledor.data.local.ListEntity
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath as M
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.*
import org.ledor.ui.navigation.EditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Повтор события в редакторе (EVT-4, B-41): «Каждый месяц» — только у типа «Другое». */
enum class Repeat { ONCE, YEARLY, MONTHLY }

data class EditorState(
    val loaded: Boolean = false,
    val id: String? = null,
    val title: String = "",
    val listId: String = LOCAL_LIST_ID,
    val type: EventType = EventType.BIRTHDAY,
    val customLabel: String = "",
    val recurring: Boolean = true,
    val recurringManual: Boolean = false,
    val monthly: Boolean = false,
    val input: CalendarKind = CalendarKind.GREGORIAN,
    val gDay: Int = LocalDate.now().dayOfMonth,
    val gMonth: Int = LocalDate.now().monthValue,
    val gYear: String = LocalDate.now().year.toString(),
    val gYearUnknown: Boolean = false,
    val afterSunset: Boolean = false,
    val hDay: Int = 1,
    val hMonth: HebrewMonth = HebrewMonth.TISHREI,
    val hYear: String = "",
    val hYearUnknown: Boolean = false,
    val observeBy: CalendarKind = CalendarKind.HEBREW,
    val observeBoth: Boolean = false,
    val note: String = "",
    val attempted: Boolean = false,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val readOnly: Boolean = false,
) {
    val gYearInt: Int? get() = if (gYearUnknown) null else gYear.toIntOrNull()
    val hYearInt: Int? get() = if (hYearUnknown) null else hYear.toIntOrNull()
    val inputYearKnown: Boolean get() = if (input == CalendarKind.GREGORIAN) gYearInt != null else hYearInt != null

    /** Каждый месяц (B-41): только «Другое», дата — григорианская, без захода солнца. */
    val isMonthly: Boolean get() = monthly && recurring && type == EventType.OTHER
    val repeat: Repeat get() = when { !recurring -> Repeat.ONCE; isMonthly -> Repeat.MONTHLY; else -> Repeat.YEARLY }

    fun eventDate(): EventDate = when {
        isMonthly -> EventDate.fromGregorian(GregorianParts(gYearInt, gMonth, gDay), false)
        input == CalendarKind.GREGORIAN -> EventDate.fromGregorian(GregorianParts(gYearInt, gMonth, gDay), afterSunset && gYearInt != null)
        else -> EventDate.fromHebrew(HebrewParts(hYearInt, hMonth, hDay))
    }

    /** Фактический календарь отметки с учётом правил I1/I2. */
    fun effectiveObserve(): CalendarKind = when {
        isMonthly -> CalendarKind.GREGORIAN
        !recurring || !inputYearKnown || effectiveBoth() -> input
        else -> observeBy
    }

    /** «По обоим календарям» — только для повторяющихся событий с известным годом (B-1). */
    fun effectiveBoth(): Boolean = recurring && !isMonthly && inputYearKnown && observeBoth

    fun toEvent(): Event = Event(
        id = id ?: "",
        listId = listId,
        title = title.trim(),
        type = type,
        customLabel = if (type == EventType.OTHER) customLabel.trim() else null,
        recurring = recurring,
        observeBy = effectiveObserve(),
        date = eventDate(),
        note = note.trim().ifEmpty { null },
        observeBoth = effectiveBoth(),
        monthly = isMonthly,
    )

    fun yearMissingForOneOff() = !recurring && !inputYearKnown
}

class EventEditorViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as KalendarApp).container
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    fun init(route: EditorRoute) {
        if (_state.value.loaded) return
        viewModelScope.launch {
            val settings = c.settingsStore.current()
            val existing = route.id?.let { c.eventRepository.get(it) }
            if (existing != null) {
                val list = c.listRepository.get(existing.listId)
                _state.value = fromEvent(existing).copy(loaded = true, readOnly = list?.canEdit != true)
                return@launch
            }
            val lastList = defaultListId(c.listRepository.observeAll().first(), settings.lastListId)
            var s = EditorState(loaded = true, listId = route.listId ?: lastList)
            route.type?.let { t -> val type = EventType.parse(t); s = s.copy(type = type, recurring = EventDefaults.recurring(type)) }
            route.title?.let { s = s.copy(title = it) }
            when (route.calendar) {
                CalendarKind.GREGORIAN.name -> s = s.copy(
                    input = CalendarKind.GREGORIAN,
                    gDay = route.day ?: s.gDay, gMonth = route.month ?: s.gMonth,
                    gYear = route.year?.toString() ?: s.gYear, afterSunset = route.afterSunset,
                )
                CalendarKind.HEBREW.name -> s = s.copy(
                    input = CalendarKind.HEBREW, hDay = route.day ?: 1,
                    hMonth = HebrewMonth.parse(route.hebrewMonth) ?: HebrewMonth.TISHREI,
                    hYear = route.year?.toString() ?: "",
                    hYearUnknown = route.year == null,
                )
                else -> {}
            }
            _state.value = syncHebrewFromGregorian(s)
        }
    }

    private fun fromEvent(e: Event): EditorState {
        val g = e.date.gregorian
        val h = e.date.hebrew
        val base = EditorState(
            id = e.id, title = e.title, listId = e.listId, type = e.type, customLabel = e.customLabel.orEmpty(),
            recurring = e.recurring, recurringManual = e.recurring != EventDefaults.recurring(e.type), monthly = e.isMonthly,
            input = e.date.inputCalendar, afterSunset = e.date.afterSunset, observeBy = e.observeBy, observeBoth = e.observeBoth, note = e.note.orEmpty(),
        )
        return base.copy(
            gDay = g?.day ?: base.gDay, gMonth = g?.month ?: base.gMonth,
            gYear = g?.year?.toString() ?: "", gYearUnknown = g != null && g.year == null,
            hDay = h?.day ?: 1, hMonth = h?.month ?: HebrewMonth.TISHREI,
            hYear = h?.year?.toString() ?: "", hYearUnknown = h != null && h.year == null,
        )
    }

    /** При смене календаря ввода переносим дату, если она полная. */
    private fun syncHebrewFromGregorian(s: EditorState): EditorState {
        val d = s.eventDate()
        return when (s.input) {
            CalendarKind.GREGORIAN -> d.hebrew?.let { s.copy(hDay = it.day, hMonth = it.month, hYear = it.year?.toString() ?: "", hYearUnknown = false) } ?: s
            CalendarKind.HEBREW -> d.gregorian?.let { s.copy(gDay = it.day, gMonth = it.month, gYear = it.year?.toString() ?: "", gYearUnknown = false, afterSunset = false) } ?: s
        }
    }

    private fun edit(f: (EditorState) -> EditorState) = _state.update { syncHebrewFromGregorian(f(it)).copy(dirty = true) }

    fun setTitle(v: String) = edit { it.copy(title = v.take(EventValidator.TITLE_MAX)) }
    fun setList(id: String) = edit { it.copy(listId = id) }
    fun setType(t: EventType) = edit {
        // «Каждый месяц» бывает только у «Другое»: при смене типа повтор становится ежегодным.
        val s = if (t != EventType.OTHER && it.monthly) it.copy(monthly = false) else it
        if (s.recurringManual) s.copy(type = t) else s.copy(type = t, recurring = EventDefaults.recurring(t))
    }
    fun setCustomLabel(v: String) = edit { it.copy(customLabel = v.take(EventValidator.LABEL_MAX)) }
    fun setRecurring(v: Boolean) = edit { it.copy(recurring = v, recurringManual = true) }

    /** EVT-4 для «Другое»: один раз / каждый год / каждый месяц. Каждый месяц — дата всегда григорианская (B-41). */
    fun setRepeat(r: Repeat) = _state.update { s0 ->
        val s = syncHebrewFromGregorian(s0)
        val base = s.copy(recurring = r != Repeat.ONCE, monthly = r == Repeat.MONTHLY, recurringManual = true, dirty = true)
        if (r != Repeat.MONTHLY) base
        // Год нужен, чтобы знать первый раз; поле «Год неизвестен» для ежемесячного не показывается.
        else base.copy(input = CalendarKind.GREGORIAN, afterSunset = false, observeBoth = false, gYearUnknown = false,
            gYear = s.gYear.takeIf { !s.gYearUnknown && it.isNotBlank() } ?: LocalDate.now().year.toString())
    }
    fun setInput(k: CalendarKind) = _state.update { s ->
        val synced = syncHebrewFromGregorian(s)
        synced.copy(input = k, dirty = true)
    }
    fun setGDay(v: Int) = edit { it.copy(gDay = v) }
    fun setGMonth(v: Int) = edit { it.copy(gMonth = v) }
    fun setGYear(v: String) = edit { it.copy(gYear = v) }
    fun setGYearUnknown(v: Boolean) = edit { it.copy(gYearUnknown = v) }
    fun setAfterSunset(v: Boolean) = edit { it.copy(afterSunset = v) }
    fun setHDay(v: Int) = edit { it.copy(hDay = v) }
    fun setHMonth(v: HebrewMonth) = edit { it.copy(hMonth = v) }
    fun setHYear(v: String) = edit { s ->
        val y = v.toIntOrNull()
        // Если месяц не существует в новом году — подобрать соответствующий Адар.
        val m = if (y != null && M.isHebrewYearSupported(y) && !M.isMonthInYear(s.hMonth, y)) when (s.hMonth) {
            HebrewMonth.ADAR -> HebrewMonth.ADAR_II
            else -> HebrewMonth.ADAR
        } else s.hMonth
        s.copy(hYear = v, hMonth = m)
    }
    fun setHYearUnknown(v: Boolean) = edit { it.copy(hYearUnknown = v) }
    fun setObserve(k: CalendarKind) = edit { it.copy(observeBy = k, observeBoth = false) }
    fun setObserveBoth() = edit { it.copy(observeBoth = true) }
    fun setNote(v: String) = edit { it.copy(note = v.take(EventValidator.NOTE_MAX)) }

    fun errors(s: EditorState): List<ValidationError> = EventValidator.validate(s.toEvent().copy(id = "x"))

    fun save() {
        val s = _state.value
        val errs = errors(s)
        if (errs.isNotEmpty()) { _state.update { it.copy(attempted = true) }; return }
        viewModelScope.launch {
            val e = s.toEvent().let { if (it.id.isEmpty()) it.copy(id = UUID.randomUUID().toString()) else it }
            c.eventRepository.save(e)
            c.settingsStore.setLastListId(e.listId)
            _state.update { it.copy(saved = true, dirty = false) }
        }
    }

    fun delete() {
        val id = _state.value.id ?: return
        viewModelScope.launch {
            c.eventRepository.delete(id)
            _state.update { it.copy(saved = true, dirty = false) }
        }
    }

    /** Создать одноразовое событие бар/бат-мицвы из подсказки. */
    fun addMitzvah(hint: MitzvahHint, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            val e = Event(
                id = UUID.randomUUID().toString(), listId = s.listId, title = s.title.trim(), type = hint.type,
                customLabel = null, recurring = false, observeBy = CalendarKind.HEBREW,
                date = EventDate.fromHebrew(HebrewParts.of(hint.hebrewDate)), note = null,
            )
            if (e.title.isNotEmpty()) { c.eventRepository.save(e); onDone() }
        }
    }
}

/**
 * Список по умолчанию для нового события (EVT-2, B-30). Есть облачный список с правом редактирования — он:
 * последний использованный, иначе первый по порядку. Облачных нет — личный.
 */
fun defaultListId(lists: List<ListEntity>, lastListId: String?): String {
    val cloud = lists.filter { it.isCloud && it.canEdit }
    return (cloud.firstOrNull { it.id == lastListId } ?: cloud.firstOrNull())?.id ?: LOCAL_LIST_ID
}
