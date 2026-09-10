/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.uwuaosp.settingsext.externaldesktop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.uwuaosp.compose.settingslib.MainSwitchPreference
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsFooterLegacy
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SettingsTopIntro
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class ExternalDesktopSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsExtTheme { ExternalDesktopSettingsScreen(onNavigateUp = ::finish) } }
    }
}

@Composable
private fun ExternalDesktopSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(ExternalDesktopSecureSettings.isEnabled(context)) }
    var blankInternalDisplay by remember {
        mutableStateOf(ExternalDesktopSecureSettings.shouldBlankInternalDisplay(context))
    }
    var allowScrcpyVirtualDisplay by remember {
        mutableStateOf(ExternalDesktopSecureSettings.allowScrcpyVirtualDisplay(context))
    }

    SettingsScaffold(
        title = stringResource(R.string.external_desktop_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
        contentTopPadding = 0.dp,
    ) {
        SettingsTopIntro(
            text = stringResource(R.string.external_desktop_description),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        MainSwitchPreference(
            title = stringResource(R.string.external_desktop_enable_title),
            checked = enabled,
            onCheckedChange = { value ->
                enabled = value
                ExternalDesktopSecureSettings.setEnabled(context, value)
            },
        )

        SettingsCategory(title = stringResource(R.string.external_desktop_behavior_category))
        SwitchPreferenceRow(
            title = stringResource(R.string.external_desktop_blank_internal_title),
            summary = stringResource(R.string.external_desktop_blank_internal_summary),
            checked = blankInternalDisplay,
            enabled = enabled,
            position = PreferencePosition.Top,
            onCheckedChange = { value ->
                blankInternalDisplay = value
                ExternalDesktopSecureSettings.setBlankInternalDisplay(context, value)
            },
        )
        SwitchPreferenceRow(
            title = stringResource(R.string.external_desktop_allow_scrcpy_title),
            summary = stringResource(R.string.external_desktop_allow_scrcpy_summary),
            checked = allowScrcpyVirtualDisplay,
            enabled = enabled,
            position = PreferencePosition.Bottom,
            onCheckedChange = { value ->
                allowScrcpyVirtualDisplay = value
                ExternalDesktopSecureSettings.setAllowScrcpyVirtualDisplay(context, value)
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        SettingsFooterLegacy(stringResource(R.string.external_desktop_footer))
    }
}
