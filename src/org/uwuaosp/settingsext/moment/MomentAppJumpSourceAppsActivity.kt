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

package org.uwuaosp.settingsext.moment

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.widget.app.AppListEmpty
import org.uwuaosp.compose.widget.app.AppListLoading
import org.uwuaosp.compose.widget.preference.CustomPreferenceRow
import org.uwuaosp.compose.widget.preference.PreferenceRow
import org.uwuaosp.compose.widget.preference.SettingsPreferenceGroup
import org.uwuaosp.compose.widget.preference.SettingsSection
import org.uwuaosp.compose.widget.preference.SettingsSectionHeader
import org.uwuaosp.compose.widget.scaffold.SettingsScaffold
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import org.uwuaosp.settingsext.apppicker.AppPickerEntry
import org.uwuaosp.settingsext.apppicker.AppSelectionActivity
import org.uwuaosp.settingsext.apppicker.loadAppsByPackageNames

/** Displays only configured AppJump source apps, with an explicit entry point to add more. */
class MomentAppJumpSourceAppsActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                MomentAppJumpSourceAppsScreen(
                    refreshToken = refreshToken.intValue,
                    onNavigateUp = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshToken.intValue += 1
    }
}

@Composable
private fun MomentAppJumpSourceAppsScreen(
    refreshToken: Int,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val addSourceAppsTitle = stringResource(R.string.moment_app_jump_add_source_apps_title)
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var addedApps by remember { mutableStateOf<List<AppPickerEntry>>(emptyList()) }
    var hasLoadedOnce by remember { mutableStateOf(false) }

    val addSourceAppsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        reloadTrigger += 1
    }

    LaunchedEffect(refreshToken, reloadTrigger) {
        val loaded = withContext(Dispatchers.IO) {
            val savedPackages = MomentSecureSettings.getAppJumpSourcePackages(context)
            val resolved = loadAppsByPackageNames(context, savedPackages)
            val resolvedSet = resolved.map { it.packageName }.toSet()
            if (resolvedSet != savedPackages.toSet()) {
                MomentSecureSettings.setAppJumpSourcePackages(context, resolvedSet.toList())
            }
            resolved
        }
        addedApps = loaded
        hasLoadedOnce = true
    }

    SettingsScaffold(
        title = stringResource(R.string.moment_app_jump_source_apps_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsPreferenceGroup {
            item {
                PreferenceRow(
                    title = addSourceAppsTitle,
                    summary = stringResource(R.string.moment_app_jump_add_source_apps_summary),
                    icon = Icons.Outlined.Add,
                    onClick = {
                        addSourceAppsLauncher.launch(
                            Intent(context, AppSelectionActivity::class.java)
                                .putExtra(
                                    AppSelectionActivity.EXTRA_SELECTION_MODE,
                                    AppSelectionActivity.SELECTION_MODE_MOMENT_APP_JUMP,
                                )
                                .putExtra(AppSelectionActivity.EXTRA_TITLE, addSourceAppsTitle),
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        when {
            !hasLoadedOnce -> {
                SettingsSectionHeader(
                    title = stringResource(R.string.moment_app_jump_added_apps_section, 0),
                )
                AppListLoading()
            }
            addedApps.isEmpty() -> {
                SettingsSectionHeader(
                    title = stringResource(R.string.moment_app_jump_added_apps_section, 0),
                )
                AppListEmpty(text = stringResource(R.string.moment_app_jump_added_apps_empty))
            }
            else -> {
                SettingsSection(
                    title = stringResource(
                        R.string.moment_app_jump_added_apps_section,
                        addedApps.size,
                    ),
                ) {
                    addedApps.forEach { app ->
                        item {
                            key(app.packageName) {
                                AddedSourceAppRow(
                                    app = app,
                                    onRemove = {
                                        val updated = addedApps.filterNot {
                                            it.packageName == app.packageName
                                        }
                                        addedApps = updated
                                        MomentSecureSettings.setAppJumpSourcePackages(
                                            context,
                                            updated.map { it.packageName },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddedSourceAppRow(
    app: AppPickerEntry,
    onRemove: () -> Unit,
) {
    CustomPreferenceRow(onClick = null) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            Text(
                text = app.label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.moment_app_jump_remove_app, app.label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
