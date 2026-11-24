package org.ledor.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.ledor.R
import org.ledor.ui.components.AppIcons
import org.ledor.ui.navigation.AboutRoute
import org.ledor.ui.navigation.FeedbackRoute
import org.ledor.ui.navigation.ListsRoute
import org.ledor.ui.navigation.SettingsRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(nav: NavController, outer: PaddingValues) {
    Scaffold(
        modifier = Modifier.padding(bottom = outer.calculateBottomPadding()),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_more)) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
    ) { inner ->
        Column(Modifier.padding(inner).padding(horizontal = 8.dp)) {
            Item(AppIcons.Group, stringResource(R.string.manage_lists), stringResource(R.string.manage_lists_sub)) { nav.navigate(ListsRoute) }
            Item(Icons.Filled.Settings, stringResource(R.string.settings), stringResource(R.string.settings_sub)) { nav.navigate(SettingsRoute) }
            Item(Icons.Filled.Email, stringResource(R.string.feedback_title), stringResource(R.string.feedback_sub)) { nav.navigate(FeedbackRoute()) }
            Item(Icons.Filled.Info, stringResource(R.string.about), stringResource(R.string.about_author)) { nav.navigate(AboutRoute) }
        }
    }
}

@Composable
private fun Item(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(sub) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Filled.KeyboardArrowRight, null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
