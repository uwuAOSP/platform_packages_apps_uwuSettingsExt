/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.uwuaosp.settingsext.interfaceui

import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.uwuaosp.compose.widget.preference.PreferenceRow
import org.uwuaosp.compose.widget.preference.SettingsSection
import org.uwuaosp.compose.widget.preference.SwitchPreferenceRow
import org.uwuaosp.compose.widget.scaffold.SettingsScaffold
import org.uwuaosp.compose.widget.ui.SettingsFooterLegacy
import org.uwuaosp.settingsext.util.SettingsHomepageIcon
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import org.uwuaosp.settingsext.background.ExpressiveModeMenuItem
import org.uwuaosp.settingsext.qsstyle.QSStyleSecureSettings

class InterfaceSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsExtTheme { InterfaceSettingsScreen(onNavigateUp = ::finish) } }
    }
}

@Composable
private fun InterfaceSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val controller = remember(context) { CustomFontController(context) }
    val iconController = remember(context) { PuiSystemIconController(context) }
    val scope = rememberCoroutineScope()
    val resolver = context.contentResolver
    var qsStyleCircular by remember(context) {
        mutableStateOf(QSStyleSecureSettings.isCircular(context))
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                qsStyleCircular = QSStyleSecureSettings.isCircular(context)
            }
        }
        resolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.UWU_QS_STYLE), false, observer,
        )
        observer.onChange(false)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    var activeFont by remember { mutableStateOf(controller.activeFontName()) }
    var activeIconStyle by remember { mutableStateOf(iconController.activeStyle()) }
    var iconOperationRunning by remember { mutableStateOf(false) }
    var iconMenuExpanded by remember { mutableStateOf(false) }
    val fontSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            activeFont = controller.activeFontName()
        }

    SettingsScaffold(
        title = stringResource(R.string.interface_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsSection(title = stringResource(R.string.interface_settings_category_qs)) {
            item {
                SwitchPreferenceRow(
                    title = stringResource(R.string.qs_style_title),
                    summary = stringResource(R.string.qs_style_summary),
                    checked = qsStyleCircular,
                    iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_qs_style) },
                    onCheckedChange = { enabled ->
                        if (QSStyleSecureSettings.setCircular(context, enabled)) {
                            qsStyleCircular = QSStyleSecureSettings.isCircular(context)
                        } else {
                            Toast.makeText(context, R.string.qs_style_update_failed,
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
        SettingsSection(title = stringResource(R.string.interface_settings_category_fonts)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.custom_font_title),
            summary =
                activeFont?.let { stringResource(R.string.custom_font_summary_active, it) }
                    ?: stringResource(R.string.custom_font_summary_default),
            iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_custom_font) },
            onClick = {
                fontSettingsLauncher.launch(Intent(context, CustomFontActivity::class.java))
            },
                )
            }
        }
        SettingsFooterLegacy(stringResource(R.string.custom_font_footer))

        SettingsSection(title = stringResource(R.string.interface_settings_category_icons)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.system_small_icons_title),
            summary = stringResource(R.string.system_small_icons_summary),
            enabled = !iconOperationRunning,
            iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_system_small_icons) },
            onClick = { iconMenuExpanded = true },
            trailingContent = {
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = systemIconStyleLabel(activeIconStyle),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left_down_line),
                            contentDescription = stringResource(R.string.system_small_icons_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = iconMenuExpanded,
                        onDismissRequest = { iconMenuExpanded = false },
                        modifier = Modifier.widthIn(min = 200.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                        shadowElevation = 3.dp,
                    ) {
                        val styles = listOf(SystemIconStyle.DEFAULT, SystemIconStyle.PUI)
                        styles.forEachIndexed { index, style ->
                            ExpressiveModeMenuItem(
                                text = systemIconStyleLabel(style),
                                selected = activeIconStyle == style,
                                position = index,
                                itemCount = styles.size,
                                onClick = {
                                    iconMenuExpanded = false
                                    if (activeIconStyle == style) return@ExpressiveModeMenuItem
                                    scope.launch {
                                        iconOperationRunning = true
                                        try {
                                            if (iconController.setStyle(style)) {
                                                activeIconStyle = style
                                            } else {
                                                Toast.makeText(
                                                        context,
                                                        R.string.system_small_icons_update_failed,
                                                        Toast.LENGTH_SHORT,
                                                    )
                                                    .show()
                                            }
                                        } finally {
                                            iconOperationRunning = false
                                        }
                                    }
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

@Composable
private fun systemIconStyleLabel(style: SystemIconStyle): String {
    return stringResource(
        when (style) {
            SystemIconStyle.DEFAULT -> R.string.system_small_icons_default
            SystemIconStyle.PUI -> R.string.system_small_icons_pui
        }
    )
}
