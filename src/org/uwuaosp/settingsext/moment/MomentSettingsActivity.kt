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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import org.uwuaosp.compose.framework.theme.SettingsTheme
import org.uwuaosp.compose.widget.preference.MainSwitchPreference
import org.uwuaosp.compose.widget.preference.PreferenceRow
import org.uwuaosp.compose.widget.preference.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.widget.preference.SettingsSection
import org.uwuaosp.compose.widget.preference.SwitchPreferenceRow
import org.uwuaosp.compose.widget.scaffold.SettingsScaffold
import org.uwuaosp.settingsext.background.ExpressiveModeMenuItem
import org.uwuaosp.settingsext.R

class MomentSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomentSettingsTheme {
                MomentSettingsScreen(lifecycle = lifecycle, onNavigateUp = ::finish)
            }
        }
    }
}

@Composable
private fun MomentSettingsTheme(content: @Composable () -> Unit) {
    SettingsTheme(content = content)
}

@Composable
private fun MomentSettingsScreen(lifecycle: Lifecycle, onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var momentEnabled by remember {
        mutableStateOf(MomentSecureSettings.isEnabled(context, false))
    }
    var fixedAspectRatio by remember {
        mutableStateOf(MomentSecureSettings.isFixedAspectRatio(context))
    }
    var aspectRatioMenuExpanded by remember { mutableStateOf(false) }
    var arcGestureEnabled by remember {
        mutableStateOf(MomentSecureSettings.isArcGestureEnabled(context, true))
    }
    var navHandleDoubleTapEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true))
    }
    var notificationClickEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNotificationClickEnabled(context, true))
    }
    var notificationClickPortraitEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNotificationClickPortraitEnabled(context, true))
    }
    var notificationClickLandscapeEnabled by remember {
        mutableStateOf(MomentSecureSettings.isNotificationClickLandscapeEnabled(context, true))
    }
    var appJumpEnabled by remember {
        mutableStateOf(MomentSecureSettings.isAppJumpEnabled(context))
    }
    var appJumpSourceCount by remember {
        mutableStateOf(MomentSecureSettings.getAppJumpSourcePackages(context).size)
    }
    val appJumpSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        appJumpSourceCount = MomentSecureSettings.getAppJumpSourcePackages(context).size
    }
    val navHandleDoubleTapSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        navHandleDoubleTapEnabled =
            MomentSecureSettings.isNavHandleDoubleTapEnabled(context, true)
    }

    SettingsScaffold(
        title = stringResource(R.string.moment_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        MainSwitchPreference(
            title = stringResource(R.string.moment_enabled_title),
            checked = momentEnabled,
            onCheckedChange = { enabled ->
                momentEnabled = enabled
                MomentSecureSettings.setEnabled(context, enabled)
            },
        )

        MomentTutorialCard(lifecycle)

        SettingsSection(title = stringResource(R.string.moment_gestures_section)) {
            item {
                PrimarySwitchPreferenceRow(
            title = stringResource(R.string.moment_nav_handle_double_tap_title),
            summary = stringResource(R.string.moment_nav_handle_double_tap_summary),
            checked = navHandleDoubleTapEnabled,
            onCheckedChange = { enabled ->
                navHandleDoubleTapEnabled = enabled
                MomentSecureSettings.setNavHandleDoubleTapEnabled(context, enabled)
            },
            onClick = {
                navHandleDoubleTapSettingsLauncher.launch(
                    Intent(context, NavHandleDoubleTapSettingsActivity::class.java),
                )
            },
            enabled = momentEnabled,
                )
            }
            item {
                PrimarySwitchPreferenceRow(
            title = stringResource(R.string.moment_arc_gesture_title),
            summary = stringResource(R.string.moment_arc_gesture_summary),
            checked = arcGestureEnabled,
            onCheckedChange = { enabled ->
                arcGestureEnabled = enabled
                MomentSecureSettings.setArcGestureEnabled(context, enabled)
            },
            onClick = {
                context.startActivity(Intent(context, MomentArcEditorActivity::class.java))
            },
            enabled = momentEnabled,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.moment_notification_section)) {
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.moment_notification_click_title),
            summary = stringResource(R.string.moment_notification_click_summary),
            checked = notificationClickEnabled,
            onCheckedChange = { enabled ->
                notificationClickEnabled = enabled
                MomentSecureSettings.setNotificationClickEnabled(context, enabled)
            },
            enabled = momentEnabled,
                )
            }
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.moment_notification_click_portrait_title),
            summary = "",
            showSummary = false,
            checked = notificationClickPortraitEnabled,
            onCheckedChange = { enabled ->
                notificationClickPortraitEnabled = enabled
                MomentSecureSettings.setNotificationClickPortraitEnabled(context, enabled)
            },
            enabled = momentEnabled && notificationClickEnabled,
                )
            }
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.moment_notification_click_landscape_title),
            summary = "",
            showSummary = false,
            checked = notificationClickLandscapeEnabled,
            onCheckedChange = { enabled ->
                notificationClickLandscapeEnabled = enabled
                MomentSecureSettings.setNotificationClickLandscapeEnabled(context, enabled)
            },
            enabled = momentEnabled && notificationClickEnabled,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.moment_app_jump_section)) {
            item {
                SwitchPreferenceRow(
                    title = stringResource(R.string.moment_app_jump_title),
                    summary = stringResource(R.string.moment_app_jump_summary),
                    checked = appJumpEnabled,
                    onCheckedChange = { enabled ->
                        appJumpEnabled = enabled
                        MomentSecureSettings.setAppJumpEnabled(context, enabled)
                    },
                    enabled = momentEnabled,
                )
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.moment_app_jump_source_apps_title),
                    summary = stringResource(
                        R.string.moment_app_jump_source_apps_summary, appJumpSourceCount,
                    ),
                    enabled = momentEnabled && appJumpEnabled,
                    onClick = {
                        appJumpSettingsLauncher.launch(
                            Intent(context, MomentAppJumpSourceAppsActivity::class.java),
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.moment_section_experience)) {
            item {
                PreferenceRow(
                    title = stringResource(R.string.moment_aspect_ratio_title),
                    summary = "",
                    showSummary = false,
                    enabled = momentEnabled,
                    onClick = { aspectRatioMenuExpanded = true },
                    trailingContent = {
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(
                                        if (fixedAspectRatio) R.string.moment_aspect_ratio_fixed
                                        else R.string.moment_aspect_ratio_device,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_left_down_line),
                                    contentDescription = stringResource(
                                        R.string.moment_aspect_ratio_title,
                                    ),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = aspectRatioMenuExpanded && momentEnabled,
                                onDismissRequest = { aspectRatioMenuExpanded = false },
                                modifier = Modifier.widthIn(min = 200.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                tonalElevation = 0.dp,
                                shadowElevation = 3.dp,
                            ) {
                                val ratios = listOf(false, true)
                                ratios.forEachIndexed { index, fixed ->
                                    ExpressiveModeMenuItem(
                                        text = stringResource(
                                            if (fixed) R.string.moment_aspect_ratio_fixed
                                            else R.string.moment_aspect_ratio_device,
                                        ),
                                        selected = fixedAspectRatio == fixed,
                                        position = index,
                                        itemCount = ratios.size,
                                        onClick = {
                                            MomentSecureSettings.setFixedAspectRatio(context, fixed)
                                            fixedAspectRatio = fixed
                                            aspectRatioMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}
