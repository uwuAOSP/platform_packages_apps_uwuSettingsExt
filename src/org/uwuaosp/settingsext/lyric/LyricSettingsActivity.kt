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

package org.uwuaosp.settingsext.lyric

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.uwuaosp.compose.settingslib.MainSwitchPreference
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsSection
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SettingsTopIntro
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.compose.settingslib.TextInputPreferenceDialog
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class LyricSettingsActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                LyricSettingsScreen(
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
private fun LyricSettingsScreen(refreshToken: Int, onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember(refreshToken) {
        mutableStateOf(LyricSecureSettings.isEnabled(context, false))
    }
    var position by remember(refreshToken) {
        mutableIntStateOf(
            LyricSecureSettings.getPosition(context, LyricSecureSettings.POSITION_OVERLAY),
        )
    }
    var showTranslation by remember(refreshToken) {
        mutableStateOf(LyricSecureSettings.isShowTranslationEnabled(context, false))
    }
    var hideClockRightIcon by remember(refreshToken) {
        mutableStateOf(LyricSecureSettings.isHideIconOnClockRightEnabled(context, false))
    }
    var sources by remember(refreshToken) {
        mutableStateOf(LyricSecureSettings.getSources(context))
    }
    var showPositionDialog by remember { mutableStateOf(false) }
    var showSourcesDialog by remember { mutableStateOf(false) }

    SettingsScaffold(
        title = stringResource(R.string.lyric_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
        contentTopPadding = 0.dp,
    ) {
        SettingsTopIntro(
            text = stringResource(R.string.lyric_settings_description),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        MainSwitchPreference(
            title = stringResource(R.string.settings_ext_lyric_fetch_title),
            checked = enabled,
            onCheckedChange = { value ->
                enabled = value
                LyricSecureSettings.setEnabled(context, value)
            },
        )

        SettingsSection(title = stringResource(R.string.lyric_settings_section_behavior)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.lyric_position_title),
            summary = stringResource(
                if (position == LyricSecureSettings.POSITION_CLOCK_RIGHT) {
                    R.string.lyric_position_summary_clock_right
                } else {
                    R.string.lyric_position_summary_overlay
                },
            ),
            enabled = enabled,
            onClick = { showPositionDialog = true },
                )
            }
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.lyric_show_translation_title),
            summary = stringResource(R.string.lyric_show_translation_summary),
            checked = showTranslation,
            enabled = enabled,
            onCheckedChange = { value ->
                showTranslation = value
                LyricSecureSettings.setShowTranslation(context, value)
            },
                )
            }
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.lyric_hide_icon_clock_right_title),
            summary = stringResource(R.string.lyric_hide_icon_clock_right_summary),
            checked = hideClockRightIcon,
            enabled = enabled && position == LyricSecureSettings.POSITION_CLOCK_RIGHT,
            onCheckedChange = { value ->
                hideClockRightIcon = value
                LyricSecureSettings.setHideIconOnClockRight(context, value)
            },
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.lyric_settings_section_sources)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.lyric_sources_title),
            summary = if (sources.isBlank()) {
                stringResource(R.string.lyric_sources_summary_default)
            } else {
                stringResource(R.string.lyric_sources_summary_configured)
            },
            enabled = enabled,
            onClick = {
                showSourcesDialog = true
            },
                )
            }
        }
    }

    if (showPositionDialog) {
        LyricPositionDialog(
            selectedPosition = position,
            onPositionSelected = { value ->
                position = value
                LyricSecureSettings.setPosition(context, value)
                showPositionDialog = false
            },
            onDismiss = { showPositionDialog = false },
        )
    }
    if (showSourcesDialog) {
        TextInputPreferenceDialog(
            title = stringResource(R.string.lyric_sources_dialog_title),
            value = sources,
            confirmText = stringResource(R.string.lyric_sources_dialog_save),
            dismissText = stringResource(android.R.string.cancel),
            singleLine = false,
            onConfirm = { value ->
                sources = value.trim()
                LyricSecureSettings.setSources(context, sources)
                showSourcesDialog = false
            },
            onDismissRequest = { showSourcesDialog = false },
        )
    }
}

@Composable
private fun LyricPositionDialog(
    selectedPosition: Int,
    onPositionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lyric_position_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                LyricPositionOption(
                    title = stringResource(R.string.lyric_position_entry_overlay),
                    selected = selectedPosition == LyricSecureSettings.POSITION_OVERLAY,
                    onClick = { onPositionSelected(LyricSecureSettings.POSITION_OVERLAY) },
                )
                LyricPositionOption(
                    title = stringResource(R.string.lyric_position_entry_clock_right),
                    selected = selectedPosition == LyricSecureSettings.POSITION_CLOCK_RIGHT,
                    onClick = { onPositionSelected(LyricSecureSettings.POSITION_CLOCK_RIGHT) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun LyricPositionOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
