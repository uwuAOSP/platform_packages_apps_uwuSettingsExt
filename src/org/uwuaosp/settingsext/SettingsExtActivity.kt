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

package org.uwuaosp.settingsext

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlin.random.Random
import org.uwuaosp.compose.framework.theme.SettingsTheme
import org.uwuaosp.compose.widget.preference.PreferenceRow
import org.uwuaosp.compose.widget.preference.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.widget.preference.SettingsPreferenceGroup
import org.uwuaosp.compose.widget.scaffold.SettingsScaffold
import org.uwuaosp.settingsext.attestation.KeyAttestationSettingsActivity
import org.uwuaosp.settingsext.background.BackgroundManagementActivity
import org.uwuaosp.settingsext.externaldesktop.ExternalDesktopSecureSettings
import org.uwuaosp.settingsext.externaldesktop.ExternalDesktopSettingsActivity
import org.uwuaosp.settingsext.interfaceui.InterfaceSettingsActivity
import org.uwuaosp.settingsext.lyric.LyricSecureSettings
import org.uwuaosp.settingsext.lyric.LyricSettingsActivity
import org.uwuaosp.settingsext.moment.MomentSecureSettings
import org.uwuaosp.settingsext.moment.MomentSettingsActivity
import org.uwuaosp.settingsext.smartsuggestions.SmartSuggestionsSettingsActivity
import org.uwuaosp.settingsext.util.FeatureUtils
import org.uwuaosp.settingsext.util.SettingsHomepageIcon

class SettingsExtActivity : ComponentActivity() {
    private var categoryLaunchPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val categoryName = intent.getStringExtra(EXTRA_CATEGORY)
        val category = SettingsExtCategory.entries.firstOrNull { it.name == categoryName }
        if (category == null && !FeatureUtils.isMomentSettingsEnabled(this)) {
            MomentSecureSettings.disableAll(this)
        }

        enableEdgeToEdge()
        val initialPaletteSeed =
            if (intent.hasExtra(EXTRA_PALETTE_SEED)) intent.getIntExtra(EXTRA_PALETTE_SEED, 0)
            else null
        setContent {
            SettingsExtTheme {
                SettingsExtScreen(
                    category = category,
                    initialPaletteSeed = initialPaletteSeed,
                    onNavigateUp = ::finish,
                    onCategoryClick = ::openCategory,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        categoryLaunchPending = false
    }

    private fun openCategory(category: SettingsExtCategory, paletteSeed: Int) {
        if (categoryLaunchPending) return
        categoryLaunchPending = true
        // Use the same window transition path as Settings' SubSettings pages.
        startActivity(
            Intent(this, SettingsExtActivity::class.java)
                .putExtra(EXTRA_CATEGORY, category.name)
                .putExtra(EXTRA_PALETTE_SEED, paletteSeed)
        )
    }
}

private const val EXTRA_CATEGORY = "org.uwuaosp.settingsext.extra.CATEGORY"
private const val EXTRA_PALETTE_SEED = "org.uwuaosp.settingsext.extra.PALETTE_SEED"
private const val AI_CORE_PACKAGE = "org.uwuaosp.aicore"
private const val AI_CORE_ACTIVITY = "org.uwuaosp.aicore.AiSettingsActivity"
private const val AI_ENTRY_UNLOCK_TAPS = 7
private const val SETTINGS_EXT_PREFS = "settings_ext_prefs"
private const val PREF_AI_ENTRY_UNLOCKED = "ai_entry_unlocked"
private const val LAUNCHER_PACKAGE = "com.android.launcher3"
private const val PRISM_PACKAGE = "org.uwuaosp.prism"
private const val PRISM_SETTINGS_ACTIVITY = "org.uwuaosp.prism.PrismSettingsActivity"

@Composable
internal fun SettingsExtTheme(content: @Composable () -> Unit) {
    SettingsTheme(content = content)
}

@Composable
private fun SettingsExtScreen(
    category: SettingsExtCategory?,
    initialPaletteSeed: Int?,
    onNavigateUp: () -> Unit,
    onCategoryClick: (SettingsExtCategory, Int) -> Unit,
) {
    val context = LocalContext.current
    val preferences =
        remember(context) { context.getSharedPreferences(SETTINGS_EXT_PREFS, MODE_PRIVATE) }
    var aiEntryUnlocked by remember { mutableStateOf(preferences.isAiEntryUnlocked()) }
    var headerTapCount by rememberSaveable { mutableIntStateOf(0) }
    val paletteSeed = rememberSaveable { initialPaletteSeed ?: Random.nextInt() }

    if (category == null) {
        SettingsScaffold(
            title = stringResource(R.string.app_name),
            showBackButton = true,
            onNavigateUp = onNavigateUp,
        ) {
            SettingsExtCategoryHeader(
                modifier =
                    Modifier.pointerInput(aiEntryUnlocked) {
                        detectTapGestures {
                            if (aiEntryUnlocked) return@detectTapGestures
                            headerTapCount += 1
                            if (headerTapCount >= AI_ENTRY_UNLOCK_TAPS) {
                                preferences.setAiEntryUnlocked(true)
                                aiEntryUnlocked = true
                            }
                        }
                    }
            )
            SettingsExtCategoryCards(
                paletteSeed = paletteSeed,
                onCategoryClick = { onCategoryClick(it, paletteSeed) },
            )
        }
    } else {
        SettingsExtCategoryPage(
            category = category,
            paletteSeed = paletteSeed,
            onNavigateUp = onNavigateUp,
        ) {
            when (category) {
                SettingsExtCategory.System -> SystemSettings()
                SettingsExtCategory.Intelligence -> IntelligenceSettings(aiEntryUnlocked)
                SettingsExtCategory.Privacy -> PrivacySettings()
            }
        }
    }
}

@Composable
private fun SystemSettings() {
    val context = LocalContext.current
    var lyricEnabled by remember { mutableStateOf(LyricSecureSettings.isEnabled(context, false)) }
    var momentEnabled by remember { mutableStateOf(MomentSecureSettings.isEnabled(context, false)) }
    var externalDesktopEnabled by remember {
        mutableStateOf(ExternalDesktopSecureSettings.isEnabled(context))
    }
    val momentSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            momentEnabled = MomentSecureSettings.isEnabled(context, false)
        }
    val externalDesktopSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            externalDesktopEnabled = ExternalDesktopSecureSettings.isEnabled(context)
        }
    val launcherSettingsIntent = remember {
        Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(LAUNCHER_PACKAGE)
    }
    SettingsPreferenceGroup {
        item {
            PreferenceRow(
                title = stringResource(R.string.interface_settings_title),
                summary = "",
                showSummary = false,
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_interface_settings) },
                onClick = {
                    context.startActivity(Intent(context, InterfaceSettingsActivity::class.java))
                },
            )
        }
        item {
            PreferenceRow(
                title = stringResource(R.string.background_management_title),
                summary = "",
                showSummary = false,
                iconContent = {
                    SettingsHomepageIcon(iconRes = R.drawable.ic_background_management)
                },
                onClick = {
                    context.startActivity(Intent(context, BackgroundManagementActivity::class.java))
                },
            )
        }
        if (FeatureUtils.isMomentSettingsEnabled(context)) {
            item {
                PrimarySwitchPreferenceRow(
                    title = stringResource(R.string.moment_settings_title),
                    summary = "",
                    showSummary = false,
                    checked = momentEnabled,
                    onCheckedChange = { enabled ->
                        momentEnabled = enabled
                        MomentSecureSettings.setEnabled(context, enabled)
                    },
                    iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_moment) },
                    onClick = {
                        momentSettingsLauncher.launch(
                            Intent(context, MomentSettingsActivity::class.java)
                        )
                    },
                )
            }
        }
        item {
            PreferenceRow(
                title = stringResource(R.string.settings_ext_launcher_settings_title),
                summary = "",
                showSummary = false,
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_launcher_settings) },
                onClick = { context.startActivity(launcherSettingsIntent) },
            )
        }
        item {
            PrimarySwitchPreferenceRow(
                title = stringResource(R.string.external_desktop_title),
                summary = "",
                showSummary = false,
                checked = externalDesktopEnabled,
                onCheckedChange = { enabled ->
                    externalDesktopEnabled = enabled
                    ExternalDesktopSecureSettings.setEnabled(context, enabled)
                },
                onClick = {
                    externalDesktopSettingsLauncher.launch(
                        Intent(context, ExternalDesktopSettingsActivity::class.java)
                    )
                },
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_external_desktop) },
            )
        }
        item {
            PrimarySwitchPreferenceRow(
                title = stringResource(R.string.settings_ext_lyric_fetch_title),
                summary = "",
                showSummary = false,
                checked = lyricEnabled,
                onCheckedChange = { enabled ->
                    lyricEnabled = enabled
                    LyricSecureSettings.setEnabled(context, enabled)
                },
                onClick = {
                    context.startActivity(Intent(context, LyricSettingsActivity::class.java))
                },
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_statusbarlyric) },
            )
        }
    }
}

@Composable
private fun IntelligenceSettings(aiEntryUnlocked: Boolean) {
    val context = LocalContext.current
    SettingsPreferenceGroup {
        item {
            PreferenceRow(
                title = stringResource(R.string.settings_ext_smart_suggestions_title),
                summary = "",
                showSummary = false,
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_smart_suggestions) },
                onClick = {
                    context.startActivity(
                        Intent(context, SmartSuggestionsSettingsActivity::class.java)
                    )
                },
            )
        }
        if (aiEntryUnlocked) {
            item {
                PreferenceRow(
                    title = stringResource(R.string.settings_ext_ai_core_title),
                    summary = "",
                    showSummary = false,
                    iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_ai) },
                    onClick = {
                        context.startActivity(
                            Intent().setComponent(ComponentName(AI_CORE_PACKAGE, AI_CORE_ACTIVITY))
                        )
                    },
                )
            }
        }
        item {
            PreferenceRow(
                title = stringResource(R.string.settings_ext_prism_title),
                summary = "",
                showSummary = false,
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_phone_camera) },
                onClick = {
                    context.startActivity(
                        Intent().setComponent(ComponentName(PRISM_PACKAGE, PRISM_SETTINGS_ACTIVITY))
                    )
                },
            )
        }
    }
}

@Composable
private fun PrivacySettings() {
    val context = LocalContext.current
    SettingsPreferenceGroup {
        item {
            PreferenceRow(
                title = stringResource(R.string.settings_ext_key_attestation_title),
                summary = "",
                showSummary = false,
                iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_spoofing) },
                onClick = {
                    context.startActivity(
                        Intent(context, KeyAttestationSettingsActivity::class.java)
                    )
                },
            )
        }
    }
}

private fun SharedPreferences.isAiEntryUnlocked(): Boolean {
    return getBoolean(PREF_AI_ENTRY_UNLOCKED, false)
}

private fun SharedPreferences.setAiEntryUnlocked(unlocked: Boolean) {
    edit().putBoolean(PREF_AI_ENTRY_UNLOCKED, unlocked).apply()
}
