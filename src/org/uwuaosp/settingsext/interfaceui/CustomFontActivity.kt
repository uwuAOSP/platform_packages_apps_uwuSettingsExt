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

package org.uwuaosp.settingsext.interfaceui

import android.content.Context
import android.graphics.fonts.FontManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsAppBarScaffold
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsFooterLegacy
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.preferencePosition
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class CustomFontActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsExtTheme { CustomFontScreen(onNavigateUp = ::finish) } }
    }
}

@Composable
private fun CustomFontScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val controller = remember(context) { CustomFontController(context) }
    val scope = rememberCoroutineScope()
    val navigationBarPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var fonts by remember { mutableStateOf(controller.preparedFonts()) }
    var selectedId by rememberSaveable { mutableStateOf(fonts.firstOrNull()?.id) }
    var activeFont by remember { mutableStateOf(controller.activeFontName()) }
    var operationRunning by remember { mutableStateOf(false) }
    val selectedFont = fonts.firstOrNull { it.id == selectedId }

    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                operationRunning = true
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching { controller.prepare(uri) }
                            .getOrDefault(FontImportResult.Failed)
                    }
                operationRunning = false
                when (result) {
                    is FontImportResult.Success -> {
                        fonts = result.fonts
                        selectedId = result.fonts.firstOrNull()?.id
                    }
                    FontImportResult.UnsupportedFile ->
                        showToast(context, R.string.custom_font_file_invalid)
                    FontImportResult.NoFonts ->
                        showToast(context, R.string.custom_font_archive_no_fonts)
                    FontImportResult.InvalidArchive ->
                        showToast(context, R.string.custom_font_archive_invalid)
                    FontImportResult.TooLarge ->
                        showToast(context, R.string.custom_font_archive_too_large)
                    FontImportResult.Failed ->
                        showToast(context, R.string.custom_font_import_failed)
                }
            }
        }

    SettingsAppBarScaffold(
        title = stringResource(R.string.custom_font_page_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    top = scaffoldPadding.calculateTopPadding() + 18.dp,
                    end = 16.dp,
                    bottom = navigationBarPadding + 18.dp,
                ),
        ) {
            item(key = "preview", contentType = "preview") { FontPreviewCard(selectedFont) }
            item(key = "source-category", contentType = "category") {
                SettingsCategory(title = stringResource(R.string.custom_font_source_category))
            }
            item(key = "source", contentType = "preference") {
                PreferenceRow(
                    title = stringResource(R.string.custom_font_import_title),
                    summary = stringResource(R.string.custom_font_import_summary),
                    enabled = !operationRunning,
                    position = PreferencePosition.Single,
                    iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_custom_font) },
                    trailingContent = {
                        if (operationRunning) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    onClick = { picker.launch(FONT_MIME_TYPES) },
                )
            }

            if (fonts.isNotEmpty()) {
                item(key = "available-category", contentType = "category") {
                    SettingsCategory(
                        title = stringResource(R.string.custom_font_available_category)
                    )
                }
                itemsIndexed(
                    items = fonts,
                    key = { _, font -> font.id },
                    contentType = { _, _ -> "font" },
                ) { index, font ->
                    PreferenceRow(
                        title = font.displayName,
                        summary =
                            stringResource(
                                R.string.custom_font_candidate_summary,
                                font.sourceName,
                                font.weight,
                                if (font.italic) {
                                    stringResource(R.string.custom_font_style_italic)
                                } else {
                                    stringResource(R.string.custom_font_style_normal)
                                },
                            ),
                        enabled = !operationRunning,
                        position = preferencePosition(index, fonts.lastIndex),
                        modifier = Modifier.semantics { role = Role.RadioButton },
                        trailingContent = {
                            RadioButton(selected = selectedId == font.id, onClick = null)
                        },
                        onClick = { selectedId = font.id },
                    )
                    if (index != fonts.lastIndex) PreferenceGroupSpacer()
                }
                item(key = "apply", contentType = "action") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val font = selectedFont ?: return@Button
                            scope.launch {
                                operationRunning = true
                                val result =
                                    withContext(Dispatchers.IO) {
                                        runCatching { controller.install(font) }
                                            .getOrElse {
                                                FontOperationResult.Failed(
                                                    FontManager.RESULT_ERROR_INVALID_FONT_FILE
                                                )
                                            }
                                    }
                                operationRunning = false
                                when (result) {
                                    is FontOperationResult.Success -> {
                                        activeFont = result.displayName
                                        showToast(context, R.string.custom_font_apply_success)
                                    }
                                    is FontOperationResult.Failed ->
                                        Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.custom_font_apply_failed,
                                                    result.errorCode,
                                                ),
                                                Toast.LENGTH_LONG,
                                            )
                                            .show()
                                    FontOperationResult.InvalidFile ->
                                        showToast(context, R.string.custom_font_file_invalid)
                                }
                            }
                        },
                        enabled = selectedFont != null && !operationRunning,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.custom_font_apply_action))
                    }
                }
            } else {
                item(key = "empty", contentType = "footer") {
                    SettingsFooterLegacy(stringResource(R.string.custom_font_empty_summary))
                }
            }

            if (activeFont != null) {
                item(key = "current-category", contentType = "category") {
                    SettingsCategory(title = stringResource(R.string.custom_font_current_category))
                }
                item(key = "restore", contentType = "preference") {
                    PreferenceRow(
                        title = stringResource(R.string.custom_font_restore_title),
                        summary = stringResource(R.string.custom_font_restore_summary),
                        enabled = !operationRunning,
                        position = PreferencePosition.Single,
                        onClick = {
                            scope.launch {
                                operationRunning = true
                                val result =
                                    withContext(Dispatchers.IO) {
                                        runCatching { controller.restoreDefault() }
                                            .getOrDefault(
                                                FontManager.RESULT_ERROR_FAILED_UPDATE_CONFIG
                                            )
                                    }
                                operationRunning = false
                                if (result == FontManager.RESULT_SUCCESS) {
                                    activeFont = null
                                    showToast(context, R.string.custom_font_restore_success)
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
                            }
                        },
                    )
                }
            }
            item(key = "footer", contentType = "footer") {
                SettingsFooterLegacy(stringResource(R.string.custom_font_footer))
            }
        }
    }
}

@Composable
private fun FontPreviewCard(font: FontCandidate?) {
    val fontFamily = remember(font?.id) { font?.let { FontFamily(it.typeface) } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = font?.displayName ?: stringResource(R.string.custom_font_preview_default),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(R.string.custom_font_preview_text),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fontFamily),
            )
            Text(
                text = stringResource(R.string.custom_font_preview_supporting_text),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
            )
        }
    }
}

private fun showToast(context: Context, message: Int) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private val FONT_MIME_TYPES =
    arrayOf(
        "font/ttf",
        "font/otf",
        "application/x-font-ttf",
        "application/x-font-opentype",
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
    )
