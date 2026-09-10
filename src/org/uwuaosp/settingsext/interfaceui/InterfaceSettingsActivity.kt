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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val scope = rememberCoroutineScope()
    var activeFont by remember { mutableStateOf(controller.activeFontName()) }
    var operationRunning by remember { mutableStateOf(false) }
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
    }
}

private val FONT_MIME_TYPES =
    arrayOf("font/ttf", "application/x-font-ttf", "application/octet-stream")
