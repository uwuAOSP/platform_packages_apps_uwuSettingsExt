/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uwuaosp.settingsext.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.settingslib.AppListEmpty
import org.uwuaosp.compose.settingslib.AppListError
import org.uwuaosp.compose.settingslib.AppListItem
import org.uwuaosp.compose.settingslib.AppListLoading
import org.uwuaosp.compose.settingslib.AppListScaffold
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsToolbarActionButton
import org.uwuaosp.compose.settingslib.preferencePosition
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class BackgroundManagementActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)
    private var observersRegistered = false

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            requestRefresh()
        }
    }

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            requestRefresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                BackgroundManagementScreen(
                    refreshToken = refreshToken.intValue,
                    onNavigateUp = ::finish,
                    onOpenSettings = {
                        startActivity(
                            Intent(this, BackgroundManagementSettingsActivity::class.java),
                        )
                    },
                    onRefresh = ::requestRefresh,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!observersRegistered) {
            val packageFilter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
            registerReceiver(packageReceiver, packageFilter, Context.RECEIVER_EXPORTED)
            contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.UWU_APP_BACKGROUND_MODES),
                false,
                settingsObserver,
            )
            observersRegistered = true
        }
        requestRefresh()
    }

    override fun onStop() {
        if (observersRegistered) {
            unregisterReceiver(packageReceiver)
            contentResolver.unregisterContentObserver(settingsObserver)
            observersRegistered = false
        }
        super.onStop()
    }

    private fun requestRefresh() {
        refreshToken.intValue += 1
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundManagementScreen(
    refreshToken: Int,
    onNavigateUp: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { BackgroundAppRepository(context) }
    var apps by remember { mutableStateOf<List<BackgroundAppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(refreshToken) {
        loading = true
        loadFailed = false
        val result = runCatching {
            withContext(Dispatchers.IO) {
                repository.loadApps(BackgroundListPreferences.showSystemApps(context))
            }
        }
        result.onSuccess { apps = it }
        result.onFailure { loadFailed = true }
        loading = false
    }

    val filteredApps = remember(apps, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    AppListScaffold(
        title = stringResource(R.string.background_management_title),
        searchQuery = searchQuery,
        searchPlaceholder = stringResource(R.string.background_search_apps),
        clearSearchContentDescription = stringResource(R.string.background_search_close),
        onSearchQueryChange = { searchQuery = it },
        onNavigateUp = onNavigateUp,
        actions = {
            SettingsToolbarActionButton(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.background_settings_title),
                onClick = onOpenSettings,
            )
        },
    ) {
        item(key = "apps_category") {
            SettingsCategory(title = stringResource(R.string.background_apps_category))
        }
        when {
            loading -> item(key = "loading") { AppListLoading() }
            loadFailed -> item(key = "error") {
                AppListError(
                    text = stringResource(R.string.background_load_failed),
                    retryText = stringResource(R.string.background_retry),
                    onRetry = onRefresh,
                )
            }
            filteredApps.isEmpty() -> item(key = "empty") {
                AppListEmpty(text = stringResource(R.string.background_no_apps))
            }
            else -> items(
                count = filteredApps.size,
                key = { filteredApps[it].packageName },
            ) { index ->
                val app = filteredApps[index]
                Column {
                    AppModePreferenceRow(
                        app = app,
                        position = preferencePosition(index, filteredApps.lastIndex),
                        onModeSelected = { mode ->
                            if (BackgroundModeSecureSettings.setMode(
                                    context,
                                    app.packageName,
                                    mode,
                                )
                            ) {
                                apps = apps.map { entry ->
                                    if (entry.packageName == app.packageName) {
                                        entry.copy(mode = mode)
                                    } else {
                                        entry
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.background_mode_update_failed,
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                    if (index != filteredApps.lastIndex) {
                        PreferenceGroupSpacer()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppModePreferenceRow(
    app: BackgroundAppEntry,
    position: PreferencePosition,
    onModeSelected: (Int) -> Unit,
) {
    var expanded by remember(app.packageName) { mutableStateOf(false) }
    val modeLabel = backgroundModeLabel(app.mode)

    AppListItem(
        label = app.label,
        packageName = app.packageName,
        icon = app.icon.asImageBitmap(),
        position = position,
        enabled = app.configurable,
        onClick = { expanded = true },
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        enabled = app.configurable,
                        role = Role.Button,
                        onClick = { expanded = true },
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = modeLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left_down_line),
                    contentDescription = stringResource(R.string.background_app_mode),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 180.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shadowElevation = 3.dp,
            ) {
                val modes = listOf(
                    BackgroundModeSecureSettings.MODE_DEFAULT,
                    BackgroundModeSecureSettings.MODE_TOMBSTONE,
                    BackgroundModeSecureSettings.MODE_FULL,
                )
                modes.forEachIndexed { index, mode ->
                    ExpressiveModeMenuItem(
                        text = backgroundModeLabel(mode),
                        selected = app.mode == mode,
                        position = index,
                        itemCount = modes.size,
                        onClick = {
                            expanded = false
                            onModeSelected(mode)
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExpressiveModeMenuItem(
    text: String,
    selected: Boolean,
    position: Int,
    itemCount: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = if (selected) {
        RoundedCornerShape(12.dp)
    } else {
        when (position) {
            0 -> RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = 4.dp,
                bottomEnd = 4.dp,
            )
            itemCount - 1 -> RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 4.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp,
            )
            else -> RoundedCornerShape(4.dp)
        }
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun backgroundModeLabel(mode: Int): String {
    return stringResource(
        when (mode) {
            BackgroundModeSecureSettings.MODE_TOMBSTONE -> R.string.background_mode_tombstone
            BackgroundModeSecureSettings.MODE_FULL -> R.string.background_mode_full
            else -> R.string.background_mode_default
        },
    )
}
