package org.ledor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.ledor.reminders.NotificationFactory
import org.ledor.ui.navigation.KalendarRoot
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val openEvent = MutableStateFlow<String?>(null)

    override fun attachBaseContext(newBase: android.content.Context) = super.attachBaseContext(org.ledor.ui.locale.AppLocale.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        (application as KalendarApp).container.localeTag.value = resources.configuration.locales[0].toLanguageTag()
        if (savedInstanceState == null) openEvent.value = intent.getStringExtra(NotificationFactory.EXTRA_EVENT_ID)
        setContent {
            KalendarRoot(
                showBrandSplash = savedInstanceState == null,
                openEventId = openEvent,
                onEventOpened = { openEvent.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(NotificationFactory.EXTRA_EVENT_ID)?.let { openEvent.value = it }
    }
}
