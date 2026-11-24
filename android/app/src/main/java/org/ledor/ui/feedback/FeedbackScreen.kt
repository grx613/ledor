package org.ledor.ui.feedback

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.ledor.BuildConfig
import org.ledor.KalendarApp
import org.ledor.R
import org.ledor.data.remote.FailureKind
import org.ledor.data.remote.RpcResult
import org.ledor.ui.components.SectionCard

const val FEEDBACK_MAX = 2000
const val CONTACT_MAX = 200

/** Отзыв разработчику (B-2): текст, необязательный контакт, по согласию — техданные без событий и ПИН-кодов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(nav: NavController, prefill: String?) {
    val ctx = LocalContext.current
    val rpc = (ctx.applicationContext as KalendarApp).container.rpc
    val scope = rememberCoroutineScope()
    val localeTag = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].toLanguageTag()
    var text by rememberSaveable { mutableStateOf(prefill?.let { "$it\n\n" }.orEmpty()) }
    var contact by rememberSaveable { mutableStateOf("") }
    var attachMeta by rememberSaveable { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feedback_title)) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (sent) {
                SectionCard {
                    Text(stringResource(R.string.feedback_thanks_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.feedback_thanks_text))
                }
                Button(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
                return@Column
            }
            Text(stringResource(R.string.feedback_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(FEEDBACK_MAX); error = null },
                label = { Text(stringResource(R.string.feedback_message)) },
                supportingText = { Text("${text.length} / $FEEDBACK_MAX") },
                minLines = 5, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = contact, onValueChange = { contact = it.take(CONTACT_MAX) },
                label = { Text(stringResource(R.string.feedback_contact)) },
                supportingText = { Text(stringResource(R.string.feedback_contact_hint)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = attachMeta, onCheckedChange = { attachMeta = it })
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.feedback_attach_meta))
                    Text(
                        stringResource(R.string.feedback_meta_detail, BuildConfig.VERSION_NAME, deviceName(), Build.VERSION.RELEASE),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    sending = true; error = null
                    val meta = if (attachMeta) mapOf(
                        "app_version" to BuildConfig.VERSION_NAME, "device" to deviceName(),
                        "os_version" to "Android ${Build.VERSION.RELEASE}",
                        "locale" to localeTag,
                    ) else null
                    scope.launch {
                        when (val r = rpc.submitFeedback(text.trim(), contact.trim().ifEmpty { null }, meta)) {
                            is RpcResult.Ok -> sent = true
                            is RpcResult.Error -> error = if (r.code == "RATE_LIMITED") R.string.feedback_rate_limited else R.string.feedback_failed
                            is RpcResult.Failure -> error = when (r.kind) {
                                FailureKind.NETWORK -> R.string.feedback_no_network
                                else -> R.string.feedback_failed
                            }
                        }
                        sending = false
                    }
                },
                enabled = !sending && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(if (sending) R.string.feedback_sending else R.string.feedback_send)) }
            Text(stringResource(R.string.feedback_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun deviceName(): String =
    if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) Build.MODEL
    else "${Build.MANUFACTURER} ${Build.MODEL}"
