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

import android.graphics.fonts.FontManager
import android.os.Bundle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsFooterLegacy
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import org.uwuaosp.settingsext.background.ExpressiveModeMenuItem

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
    var activeFont by remember { mutableStateOf(controller.activeFontName()) }
    var operationRunning by remember { mutableStateOf(false) }
    var activeIconStyle by remember { mutableStateOf(iconController.activeStyle()) }
    var iconOperationRunning by remember { mutableStateOf(false) }
    var iconMenuExpanded by remember { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                operationRunning = true
                try {
                    val operationResult =
                        withContext(Dispatchers.IO) {
                            runCatching { controller.install(uri) }
                                .getOrElse {
                                    FontOperationResult.Failed(
                                        FontManager.RESULT_ERROR_INVALID_FONT_FILE
                                    )
                                }
                        }
                    when (operationResult) {
                        is FontOperationResult.Success -> {
                            activeFont = operationResult.displayName
                            Toast.makeText(
                                    context,
                                    R.string.custom_font_apply_success,
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        is FontOperationResult.Failed -> {
                            Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.custom_font_apply_failed,
                                        operationResult.errorCode,
                                    ),
                                    Toast.LENGTH_LONG,
                                )
                                .show()
                        }
                        FontOperationResult.InvalidFile -> {
                            Toast.makeText(
                                    context,
                                    R.string.custom_font_file_invalid,
                                    Toast.LENGTH_SHORT,
                                )
                                .show()
                        }
                    }
                } finally {
                    operationRunning = false
                }
            }
        }

    SettingsScaffold(
        title = stringResource(R.string.interface_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsCategory(title = stringResource(R.string.interface_settings_category_fonts))
        PreferenceRow(
            title = stringResource(R.string.custom_font_title),
            summary =
                activeFont?.let { stringResource(R.string.custom_font_summary_active, it) }
                    ?: stringResource(R.string.custom_font_summary_default),
            enabled = !operationRunning,
            position =
                if (activeFont == null) {
                    PreferencePosition.Single
                } else {
                    PreferencePosition.Top
                },
            iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_custom_font) },
            onClick = { picker.launch(FONT_MIME_TYPES) },
        )
        if (activeFont != null) {
            PreferenceGroupSpacer()
            PreferenceRow(
                title = stringResource(R.string.custom_font_restore_title),
                summary = stringResource(R.string.custom_font_restore_summary),
                enabled = !operationRunning,
                position = PreferencePosition.Bottom,
                onClick = {
                    scope.launch {
                        operationRunning = true
                        try {
                            val result =
                                withContext(Dispatchers.IO) {
                                    runCatching { controller.restoreDefault() }
                                        .getOrDefault(FontManager.RESULT_ERROR_FAILED_UPDATE_CONFIG)
                                }
                            if (result == FontManager.RESULT_SUCCESS) {
                                activeFont = null
                                Toast.makeText(
                                        context,
                                        R.string.custom_font_restore_success,
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            } else {
                                Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.custom_font_apply_failed,
                                            result,
                                        ),
                                        Toast.LENGTH_LONG,
                                    )
                                    .show()
                            }
                        } finally {
                            operationRunning = false
                        }
                    }
                },
            )
        }
        SettingsFooterLegacy(stringResource(R.string.custom_font_footer))

        SettingsCategory(title = stringResource(R.string.interface_settings_category_icons))
        PreferenceRow(
            title = stringResource(R.string.system_small_icons_title),
            summary = stringResource(R.string.system_small_icons_summary),
            enabled = !iconOperationRunning,
            position = PreferencePosition.Single,
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

@Composable
private fun systemIconStyleLabel(style: SystemIconStyle): String {
    return stringResource(
        when (style) {
            SystemIconStyle.DEFAULT -> R.string.system_small_icons_default
            SystemIconStyle.PUI -> R.string.system_small_icons_pui
        }
    )
}

private val FONT_MIME_TYPES =
    arrayOf("font/ttf", "application/x-font-ttf", "application/octet-stream")
