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
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsIllustrationHeader
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.rememberSettingsTypography
import org.uwuaosp.settingsext.attestation.KeyAttestationSettingsActivity
import org.uwuaosp.settingsext.appjump.AppJumpSettingsActivity
import org.uwuaosp.settingsext.background.BackgroundManagementActivity
import org.uwuaosp.settingsext.lyric.LyricSecureSettings
import org.uwuaosp.settingsext.lyric.LyricSettingsActivity
import org.uwuaosp.settingsext.moment.MomentSecureSettings
import org.uwuaosp.settingsext.moment.MomentSettingsActivity
import org.uwuaosp.settingsext.qs.QuickSettingsActivity
import org.uwuaosp.settingsext.smartsuggestions.SmartSuggestionsSettingsActivity
import org.uwuaosp.settingsext.sensors.SensorPolicyActivity
import org.uwuaosp.settingsext.util.FeatureUtils

class SettingsExtActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FeatureUtils.isMomentSettingsEnabled(this)) {
            MomentSecureSettings.disableAll(this)
        }

        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                SettingsExtHomeScreen(onNavigateUp = ::finish)
            }
        }
    }

}

private const val AI_CORE_PACKAGE = "org.uwuaosp.aicore"
private const val AI_CORE_ACTIVITY = "org.uwuaosp.aicore.AiSettingsActivity"
private const val AI_ENTRY_UNLOCK_TAPS = 7
private const val QS_ENTRY_UNLOCK_HOLD_MILLIS = 10_000L
private const val SETTINGS_EXT_PREFS = "settings_ext_prefs"
private const val PREF_AI_ENTRY_UNLOCKED = "ai_entry_unlocked"
private const val LAUNCHER_PACKAGE = "com.android.launcher3"
private const val PRISM_PACKAGE = "org.uwuaosp.prism"
private const val PRISM_SETTINGS_ACTIVITY = "org.uwuaosp.prism.PrismSettingsActivity"

@Composable
internal fun SettingsExtTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberSettingsTypography(),
        content = content,
    )
}

@Composable
private fun SettingsExtHomeScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(SETTINGS_EXT_PREFS, MODE_PRIVATE)
    }
    var lyricEnabled by remember {
        mutableStateOf(LyricSecureSettings.isEnabled(context, false))
    }
    var momentEnabled by remember {
        mutableStateOf(MomentSecureSettings.isEnabled(context, false))
    }
    val momentSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        momentEnabled = MomentSecureSettings.isEnabled(context, false)
    }
    var aiEntryUnlocked by remember {
        mutableStateOf(preferences.isAiEntryUnlocked())
    }
    var headerTapCount by remember { mutableIntStateOf(0) }
    var qsEntryVisible by rememberSaveable { mutableStateOf(false) }
    val launcherSettingsIntent = remember {
        Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(LAUNCHER_PACKAGE)
    }

    SettingsScaffold(
        title = stringResource(R.string.app_name),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsIllustrationHeader(
            imageRes = R.drawable.settings_ext_header_image,
            modifier = Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var completed = false
                    var up: PointerInputChange? = null
                    withTimeoutOrNull(QS_ENTRY_UNLOCK_HOLD_MILLIS) {
                        up = waitForUpOrCancellation()
                        completed = true
                    }
                    if (!completed) {
                        qsEntryVisible = true
                        waitForUpOrCancellation()
                    } else if (up != null) {
                        if (!aiEntryUnlocked) {
                            headerTapCount += 1
                            if (headerTapCount >= AI_ENTRY_UNLOCK_TAPS) {
                                preferences.setAiEntryUnlocked(true)
                                aiEntryUnlocked = true
                            }
                        }
                    }
                }
            },
            height = 180.dp,
        )

        SettingsCategory(title = stringResource(R.string.settings_ext_category_system_interface))
        if (qsEntryVisible) {
            PreferenceRow(
                title = stringResource(R.string.quick_settings_title),
                summary = "",
                showSummary = false,
                position = PreferencePosition.Top,
                iconContent = {
                    SettingsHomepageIcon(iconRes = R.drawable.ic_quick_settings)
                },
                onClick = {
                    context.startActivity(Intent(context, QuickSettingsActivity::class.java))
                },
            )
            PreferenceGroupSpacer()
        }
        PreferenceRow(
            title = stringResource(R.string.background_management_title),
            summary = "",
            showSummary = false,
            position = if (qsEntryVisible) PreferencePosition.Middle else PreferencePosition.Top,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_background_management)
            },
            onClick = {
                context.startActivity(Intent(context, BackgroundManagementActivity::class.java))
            },
        )
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.sensor_policy_title),
            summary = "",
            showSummary = false,
            position = PreferencePosition.Middle,
            iconContent = { SettingsHomepageIcon(iconRes = R.drawable.ic_sensor_policy) },
            onClick = { context.startActivity(Intent(context, SensorPolicyActivity::class.java)) },
        )
        PreferenceGroupSpacer()
        if (FeatureUtils.isMomentSettingsEnabled(context)) {
            PrimarySwitchPreferenceRow(
                title = stringResource(R.string.moment_settings_title),
                summary = "",
                showSummary = false,
                checked = momentEnabled,
                onCheckedChange = { enabled ->
                    momentEnabled = enabled
                    MomentSecureSettings.setEnabled(context, enabled)
                },
                position = PreferencePosition.Middle,
                iconContent = {
                    SettingsHomepageIcon(iconRes = R.drawable.ic_moment)
                },
                onClick = {
                    momentSettingsLauncher.launch(
                        Intent(context, MomentSettingsActivity::class.java),
                    )
                },
            )
            PreferenceGroupSpacer()
        }
        PreferenceRow(
            title = stringResource(R.string.settings_ext_launcher_settings_title),
            summary = "",
            showSummary = false,
            position = PreferencePosition.Middle,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_launcher_settings)
            },
            onClick = { context.startActivity(launcherSettingsIntent) },
        )
        PreferenceGroupSpacer()
        PrimarySwitchPreferenceRow(
            title = stringResource(R.string.settings_ext_lyric_fetch_title),
            summary = "",
            showSummary = false,
            checked = lyricEnabled,
            position = PreferencePosition.Bottom,
            onCheckedChange = { enabled ->
                lyricEnabled = enabled
                LyricSecureSettings.setEnabled(context, enabled)
            },
            onClick = {
                context.startActivity(Intent(context, LyricSettingsActivity::class.java))
            },
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_statusbarlyric)
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.settings_ext_category_privacy_security))
        PreferenceRow(
            title = stringResource(R.string.settings_ext_key_attestation_title),
            summary = "",
            showSummary = false,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_spoofing)
            },
            position = PreferencePosition.Top,
            onClick = {
                context.startActivity(Intent(context, KeyAttestationSettingsActivity::class.java))
            },
        )
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.settings_ext_app_jump_title),
            summary = "",
            showSummary = false,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_appjump)
            },
            position = PreferencePosition.Bottom,
            onClick = {
                context.startActivity(AppJumpSettingsActivity.createIntent(context))
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.settings_ext_category_intelligence))
        PreferenceRow(
            title = stringResource(R.string.settings_ext_smart_suggestions_title),
            summary = "",
            showSummary = false,
            position = if (aiEntryUnlocked) {
                PreferencePosition.Top
            } else {
                PreferencePosition.Top
            },
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_smart_suggestions)
            },
            onClick = {
                context.startActivity(Intent(context, SmartSuggestionsSettingsActivity::class.java))
            },
        )
        if (aiEntryUnlocked) {
            PreferenceGroupSpacer()
            PreferenceRow(
                title = stringResource(R.string.settings_ext_ai_core_title),
                summary = "",
                showSummary = false,
                position = PreferencePosition.Middle,
                iconContent = {
                    SettingsHomepageIcon(iconRes = R.drawable.ic_ai)
                },
                onClick = {
                    context.startActivity(
                        Intent().setComponent(ComponentName(AI_CORE_PACKAGE, AI_CORE_ACTIVITY)),
                    )
                },
            )
        }
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.settings_ext_prism_title),
            summary = "",
            showSummary = false,
            position = PreferencePosition.Bottom,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_phone_camera)
            },
            onClick = {
                context.startActivity(
                    Intent().setComponent(
                        ComponentName(PRISM_PACKAGE, PRISM_SETTINGS_ACTIVITY),
                    ),
                )
            },
        )
    }
}

private fun SharedPreferences.isAiEntryUnlocked(): Boolean {
    return getBoolean(PREF_AI_ENTRY_UNLOCKED, false)
}

private fun SharedPreferences.setAiEntryUnlocked(unlocked: Boolean) {
    edit().putBoolean(PREF_AI_ENTRY_UNLOCKED, unlocked).apply()
}
