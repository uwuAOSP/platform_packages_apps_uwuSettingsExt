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

package org.uwuaosp.settingsext.smartsuggestions

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.PrimarySwitchPreferenceRow
import org.uwuaosp.compose.settingslib.SettingsSection
import org.uwuaosp.compose.settingslib.SettingsIllustrationHeader
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SettingsTopIntro
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import org.uwuaosp.settingsext.apppicker.AppSelectionActivity
import org.uwuaosp.settingsext.apppicker.LaunchableAppPicker
import org.uwuaosp.settingsext.smartsuggestions.clipboard.ClipboardRuleStore
import org.uwuaosp.settingsext.smartsuggestions.sms.SmsCodeRuleStore

class SmartSuggestionsSettingsActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ClipboardRuleStore.ensureInitialized(this)
        SmsCodeRuleStore.ensureInitialized(this)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                SmartSuggestionsScreen(
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
private fun SmartSuggestionsScreen(refreshToken: Int, onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var torchEnabled by remember(refreshToken) {
        mutableStateOf(SmartSuggestionsSecureSettings.isTorchEnabled(context, false))
    }
    var musicEnabled by remember(refreshToken) {
        mutableStateOf(SmartSuggestionsSecureSettings.isMusicEnabled(context, false))
    }
    var smsEnabled by remember(refreshToken) {
        mutableStateOf(SmsCodeRuleStore.isEnabled(context, false))
    }
    var clipboardEnabled by remember(refreshToken) {
        mutableStateOf(ClipboardRuleStore.isEnabled(context, false))
    }
    val musicPackage = SmartSuggestionsSecureSettings.getMusicPackage(
        context,
        context.getString(R.string.default_music_app),
    )
    val musicLabel = LaunchableAppPicker.resolveAppName(context, musicPackage)
    val customClipboardCount = ClipboardRuleStore.loadRules(context).count { !it.isPreset() }
    val customSmsCount = SmsCodeRuleStore.loadRules(context).count { !it.isPreset() }

    SettingsScaffold(
        title = stringResource(R.string.smart_suggestions_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
        contentTopPadding = 0.dp,
    ) {
        SettingsTopIntro(stringResource(R.string.smart_suggestions_settings_description))
        SettingsIllustrationHeader(
            imageRes = R.drawable.smart_suggestions_header_image,
            height = 240.dp,
        )

        SettingsSection(title = stringResource(R.string.suggestion_section_flashlight)) {
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.switch_torch_suggestion_title),
            summary = stringResource(R.string.switch_torch_suggestion_desc),
            checked = torchEnabled,
            onCheckedChange = {
                torchEnabled = it
                SmartSuggestionsSecureSettings.setTorchEnabled(context, it)
            },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.suggestion_section_music)) {
            item {
                PrimarySwitchPreferenceRow(
            title = stringResource(R.string.switch_music_suggestion_title),
            summary = if (musicEnabled) {
                stringResource(R.string.switch_music_suggestion_summary_on, musicLabel)
            } else {
                stringResource(R.string.switch_music_suggestion_summary_off)
            },
            checked = musicEnabled,
            onCheckedChange = {
                musicEnabled = it
                SmartSuggestionsSecureSettings.setMusicEnabled(context, it)
            },
            onClick = {
                context.startActivity(
                    Intent(context, AppSelectionActivity::class.java).putExtra(
                        AppSelectionActivity.EXTRA_SELECTION_MODE,
                        AppSelectionActivity.SELECTION_MODE_MUSIC_SUGGESTION,
                    ),
                )
            },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.suggestion_section_sms)) {
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.switch_sms_code_title),
            summary = stringResource(R.string.switch_sms_code_desc),
            checked = smsEnabled,
            onCheckedChange = {
                smsEnabled = it
                SmsCodeRuleStore.setEnabled(context, it)
            },
                )
            }
            item {
                PreferenceRow(
            title = stringResource(R.string.sms_code_rule_settings_title),
            summary = ruleCountSummary(
                context = context,
                emptyRes = R.string.sms_code_rule_custom_summary_empty,
                countRes = R.string.sms_code_rule_custom_summary_count,
                count = customSmsCount,
            ),
            onClick = {
                context.startActivity(Intent(context, SmartSuggestionsSmsRulesActivity::class.java))
            },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.suggestion_section_links)) {
            item {
                SwitchPreferenceRow(
            title = stringResource(R.string.switch_url_suggestion_title),
            summary = stringResource(R.string.switch_url_suggestion_desc),
            checked = clipboardEnabled,
            onCheckedChange = {
                clipboardEnabled = it
                ClipboardRuleStore.setEnabled(context, it)
            },
                )
            }
            item {
                PreferenceRow(
            title = stringResource(R.string.clipboard_rule_settings_title),
            summary = ruleCountSummary(
                context,
                R.string.clipboard_rule_custom_summary_empty,
                R.string.clipboard_rule_custom_summary_count,
                customClipboardCount,
            ),
            onClick = {
                context.startActivity(Intent(context, SmartSuggestionsClipboardRulesActivity::class.java))
            },
                )
            }
        }

    }
}

private fun ruleCountSummary(
    context: android.content.Context,
    emptyRes: Int,
    countRes: Int,
    count: Int,
): String {
    return if (count == 0) {
        context.getString(emptyRes)
    } else {
        context.getString(countRes, count)
    }
}
