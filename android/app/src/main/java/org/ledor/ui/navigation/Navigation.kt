package org.ledor.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.ledor.R
import org.ledor.ui.AppViewModel
import org.ledor.ui.about.AboutScreen
import org.ledor.ui.calendar.CalendarScreen
import org.ledor.ui.converter.ConverterScreen
import org.ledor.ui.detail.EventDetailScreen
import org.ledor.ui.editor.EventEditorScreen
import org.ledor.ui.events.EventsScreen
import org.ledor.ui.feedback.FeedbackScreen
import org.ledor.ui.lists.ListCreateScreen
import org.ledor.ui.lists.ListDetailScreen
import org.ledor.ui.lists.ListJoinScreen
import org.ledor.ui.lists.ListsScreen
import org.ledor.ui.more.MoreScreen
import org.ledor.ui.settings.CityPickerScreen
import org.ledor.ui.settings.SettingsScreen
import org.ledor.ui.splash.BrandSplash
import org.ledor.ui.theme.KalendarTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable object CalendarRoute
@Serializable object EventsRoute
@Serializable object ConverterRoute
@Serializable object MoreRoute
@Serializable object ListsRoute
@Serializable object ListCreateRoute
@Serializable data class ListJoinRoute(val name: String? = null)
@Serializable data class ListDetailRoute(val id: String)
@Serializable object SettingsRoute
@Serializable object CityPickerRoute
@Serializable object AboutRoute
@Serializable data class FeedbackRoute(val prefill: String? = null)
@Serializable data class EventDetailRoute(val id: String)

/** Редактор: либо существующее событие (id), либо новое с подставленной датой. */
@Serializable
data class EditorRoute(
    val id: String? = null,
    val calendar: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val hebrewMonth: String? = null,
    val day: Int? = null,
    val afterSunset: Boolean = false,
    val type: String? = null,
    val title: String? = null,
    val listId: String? = null,
)

const val SPLASH_DURATION_MS = 1500L

private data class Tab(val route: Any, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(CalendarRoute, R.string.tab_calendar, Icons.Filled.DateRange),
    Tab(EventsRoute, R.string.tab_events, Icons.AutoMirrored.Filled.List),
    Tab(ConverterRoute, R.string.tab_converter, org.ledor.ui.components.AppIcons.Swap),
    Tab(MoreRoute, R.string.tab_more, Icons.Filled.Menu),
)

@Composable
fun KalendarRoot(showBrandSplash: Boolean, openEventId: StateFlow<String?>, onEventOpened: () -> Unit) {
    val vm: AppViewModel = viewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.onResume() }

    KalendarTheme(settings?.themeMode ?: org.ledor.data.prefs.ThemeMode.SYSTEM) {
        var splash by rememberSaveable { mutableStateOf(showBrandSplash) }
        LaunchedEffect(Unit) { if (splash) { delay(SPLASH_DURATION_MS); splash = false } }

        val nav = rememberNavController()
        val pendingEvent by openEventId.collectAsStateWithLifecycle()
        LaunchedEffect(pendingEvent, splash) {
            val id = pendingEvent
            if (id != null && !splash) { nav.navigate(EventDetailRoute(id)); onEventOpened() }
        }

        val back by nav.currentBackStackEntryAsState()
        val dest = back?.destination
        val isTab = tabs.any { t -> dest?.hasRoute(t.route::class) == true }

        Box(Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    if (isTab) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = dest?.hasRoute(t.route::class) == true,
                                onClick = {
                                    nav.navigate(t.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(t.icon, null) },
                                label = { Text(stringResource(t.label)) },
                            )
                        }
                    }
                },
            ) { padding ->
                NavHost(nav, startDestination = CalendarRoute, modifier = Modifier.fillMaxSize()) {
                    composable<CalendarRoute> { CalendarScreen(vm, nav, padding, splashVisible = splash) }
                    composable<EventsRoute> { EventsScreen(vm, nav, padding) }
                    composable<ConverterRoute> { ConverterScreen(nav, padding) }
                    composable<MoreRoute> { MoreScreen(nav, padding) }
                    composable<ListsRoute> { ListsScreen(vm, nav) }
                    composable<ListCreateRoute> { ListCreateScreen(vm, nav) }
                    composable<ListJoinRoute> { ListJoinScreen(nav, it.toRoute<ListJoinRoute>().name) }
                    composable<ListDetailRoute> { ListDetailScreen(vm, nav, it.toRoute<ListDetailRoute>().id) }
                    composable<SettingsRoute> { SettingsScreen(vm, nav) }
                    composable<CityPickerRoute> { CityPickerScreen(vm, nav) }
                    composable<AboutRoute> { AboutScreen(nav) }
                    composable<FeedbackRoute> { FeedbackScreen(nav, it.toRoute<FeedbackRoute>().prefill) }
                    composable<EventDetailRoute> { EventDetailScreen(vm, nav, it.toRoute<EventDetailRoute>().id) }
                    composable<EditorRoute> { EventEditorScreen(vm, nav, it.toRoute<EditorRoute>()) }
                }
            }
            AnimatedVisibility(splash, enter = fadeIn(tween(0)), exit = fadeOut(tween(250))) { BrandSplash() }
        }
    }
}
