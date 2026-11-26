package org.ledor.ui.about

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import org.ledor.BuildConfig
import org.ledor.KalendarApp
import org.ledor.R
import org.ledor.ui.components.LogoBlock
import org.ledor.ui.components.SectionCard
import org.ledor.ui.components.Wordmark
import org.ledor.ui.navigation.FeedbackRoute
import org.ledor.update.InstallSource
import org.ledor.update.UpdateChecker
import org.ledor.update.UpdateState

/** Адреса — BACKLOG B-3, B-4, B-8. Репозиторий — GitHub (Q-03). */
const val SITE_URL = "https://kalendar.bothub.chat"
/** Страницы сайта на языке интерфейса: /en/, /he/, /ru/ … (теги совпадают с R.string.locale_tag). */
fun sitePage(localeTag: String, page: String = "") = "$SITE_URL/$localeTag/" + if (page.isEmpty()) "" else "$page/"
const val AUTHOR_CHANNEL_URL = "https://t.me/bothub_founder"
const val SOURCE_URL = "https://github.com/grx613/ledor"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(nav: androidx.navigation.NavController) {
    val ctx = LocalContext.current
    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    var licenses by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            LogoBlock(onDark = false, markSize = 88.dp)
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            UpdateBlock(remember { (ctx.applicationContext as KalendarApp).container.updateChecker })
            Spacer(Modifier.height(4.dp))
            SectionCard { Text(stringResource(R.string.about_text)) }
            SectionCard {
                Text(stringResource(R.string.about_author), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { open(AUTHOR_CHANNEL_URL) }, contentPadding = PaddingValues(0.dp)) { Text(stringResource(R.string.about_author_channel)) }
            }
            SectionCard {
                val lang = stringResource(R.string.locale_tag)
                TextButton(onClick = { open(sitePage(lang)) }) { Text(stringResource(R.string.about_site)) }
                TextButton(onClick = { open(sitePage(lang, "privacy")) }) { Text(stringResource(R.string.about_privacy)) }
                if (SOURCE_URL.isNotEmpty()) TextButton(onClick = { open(SOURCE_URL) }) { Text(stringResource(R.string.about_source)) }
                TextButton(onClick = { nav.navigate(FeedbackRoute()) }) { Text(stringResource(R.string.feedback_title)) }
                TextButton(onClick = { licenses = !licenses }) { Text(stringResource(R.string.about_licenses)) }
                if (licenses) Text(stringResource(R.string.licenses_text), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Wordmark(onDark = false)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * ABT-5: «Проверить обновление» и «Обновить». В сеть ходит только по нажатию. [initial] — для снимков экрана.
 */
@Composable
fun UpdateBlock(checker: UpdateChecker, initial: UpdateState = UpdateState.Idle) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(initial) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (val s = state) {
            UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.update_checking), style = MaterialTheme.typography.bodyMedium)
            }
            is UpdateState.Available -> {
                Text(stringResource(R.string.update_available, s.version), style = MaterialTheme.typography.titleSmall)
                Button(onClick = { checker.updateIntents(s.version).firstOrNull { runCatching { ctx.startActivity(it) }.isSuccess } }, modifier = Modifier.testTag("update_button")) {
                    Text(stringResource(R.string.update_button))
                }
                Text(
                    stringResource(if (checker.installSource == InstallSource.GOOGLE_PLAY) R.string.update_hint_play else R.string.update_hint_site),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
            }
            else -> {
                if (s == UpdateState.UpToDate) Text(stringResource(R.string.update_latest), style = MaterialTheme.typography.bodyMedium)
                if (s == UpdateState.Failed) Text(stringResource(R.string.update_failed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                OutlinedButton(onClick = { state = UpdateState.Checking; scope.launch { state = checker.check(BuildConfig.VERSION_NAME) } }, modifier = Modifier.testTag("update_check")) {
                    Text(stringResource(R.string.update_check))
                }
            }
        }
    }
}
