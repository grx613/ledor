package org.ledor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import org.ledor.KalendarApp
import org.ledor.data.local.ListEntity
import org.ledor.data.local.ListWithCount
import org.ledor.data.holiday.HolidayEvents
import org.ledor.data.prefs.AppSettings
import org.ledor.domain.event.Event
import org.ledor.domain.event.Occurrence
import org.ledor.domain.event.OccurrenceCalculator
import org.ledor.reminders.HealthIssue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Событие + ближайшее наступление + список. */
data class UpcomingItem(val event: Event, val occurrence: Occurrence, val list: ListEntity?, val daysUntil: Long)

/** Общие для всех вкладок данные (живут на уровне Activity). */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val container = (app as KalendarApp).container

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    val settings: StateFlow<AppSettings?> = container.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** События пользователя, вехи первого года по йорцайтам (B-26) и встроенный список праздников (B-25, на год назад и на десять вперёд). */
    val events: StateFlow<List<Event>?> = combine(container.eventRepository.observeAll(), settings, today, container.localeTag) { own, s, t, _ ->
        if (s == null) null
        else own + container.milestoneEvents.build(own, t, s.adarPolicy) +
            container.holidayEvents.build(s.holidays, container.cityCatalog.byId(s.cityId),
                from = t.minusYears(1), to = t.plusYears(10), shabbatFrom = t, shabbatTo = t.plusDays(13), nextDays = true,
                yizkor = own.any { it.type == org.ledor.domain.event.EventType.YAHRZEIT })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val lists: StateFlow<List<ListWithCount>> = container.listRepository.observeLists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val notices: StateFlow<List<String>> = container.settingsStore.notices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _health = MutableStateFlow<List<HealthIssue>>(emptyList())
    val health: StateFlow<List<HealthIssue>> = _health.asStateFlow()

    /** Ближайшее наступление каждого события, отсортировано. */
    val upcoming: StateFlow<List<UpcomingItem>?> = combine(events, lists, settings, today) { ev, ls, s, t ->
        if (ev == null || s == null) return@combine null
        val byId = ls.associate { it.list.id to it.list }
        var shabbatShown = false
        ev.mapNotNull { e ->
            OccurrenceCalculator.next(e, t, s.adarPolicy)?.let { o ->
                UpcomingItem(e, o, byId[e.listId], java.time.temporal.ChronoUnit.DAYS.between(t, o.date))
            }
        }.sortedWith(compareBy({ it.occurrence.date }, { it.event.title })).filter { u ->
            val id = u.event.id
            when {
                !HolidayEvents.isBuiltIn(id) -> true
                // Праздники — на год вперёд; второй и следующие дни — только сегодняшний; Шабат — только ближайший.
                u.daysUntil > 366 -> false
                HolidayEvents.isNextDay(id) -> u.daysUntil == 0L
                HolidayEvents.isShabbat(id) -> !shabbatShown.also { shabbatShown = true }
                else -> true
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(Duration.between(now, midnight).toMillis() + 1000)
                _today.value = LocalDate.now()
            }
        }
    }

    fun onResume() {
        _today.value = LocalDate.now()
        _health.value = container.healthChecker.check()
    }

    fun dismissNotices() = viewModelScope.launch { container.settingsStore.clearNotices() }
    fun setOnboardingShown() = viewModelScope.launch { container.settingsStore.setOnboardingShown() }
    fun disableHealthWarnings() = viewModelScope.launch { container.settingsStore.setHealthWarnings(false) }
    fun setGridMode(m: org.ledor.data.prefs.GridMode) = viewModelScope.launch { container.settingsStore.setGridMode(m) }

    fun setHolidays(v: org.ledor.data.prefs.HolidaySettings) = viewModelScope.launch { container.settingsStore.setHolidays(v) }

    fun listsById(): Map<String, ListEntity> = lists.value.associate { it.list.id to it.list }
}
