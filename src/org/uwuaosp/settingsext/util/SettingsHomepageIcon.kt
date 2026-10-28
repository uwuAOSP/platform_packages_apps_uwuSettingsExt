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

package org.uwuaosp.settingsext.util

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.TonalPalette
import org.uwuaosp.settingsext.R
import com.google.android.material.R as MaterialR
import org.uwuaosp.compose.widget.ui.SettingsHomepageIcon as ComposeSettingsHomepageIcon

@Composable
internal fun SettingsHomepageIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    val designColor = colorResource(homepageIconPalette(iconRes).seed).toArgb()
    val themeColor = MaterialTheme.colorScheme.primary.toArgb()
    val darkTheme = isSystemInDarkTheme()
    // Google's harmonization limits the hue shift to 15 degrees, retaining category colors.
    val palette = remember(designColor, themeColor) {
        TonalPalette.fromInt(Blend.harmonize(designColor, themeColor))
    }
    val backgroundColor = remember(palette, darkTheme) {
        Color(palette.tone(if (darkTheme) 80 else 90))
    }
    val foregroundColor = remember(palette) { Color(palette.tone(30)) }

    ComposeSettingsHomepageIcon(
        iconRes = iconRes,
        modifier = modifier,
        backgroundColor = backgroundColor,
        foregroundColor = foregroundColor,
    )
}

// Settings' reference colors remain the seeds; Monet only harmonizes their hue and tones.
private enum class HomepageIconPalette(
    @ColorRes val seed: Int,
) {
    Orange(MaterialR.color.m3_ref_palette_orange40),
    Green(MaterialR.color.m3_ref_palette_green40),
    Cyan(MaterialR.color.m3_ref_palette_cyan40),
    Pink(MaterialR.color.m3_ref_palette_pink40),
    Purple(MaterialR.color.m3_ref_palette_purple40),
    BlueVariant(MaterialR.color.m3_ref_palette_blue_variant40),
    Blue(MaterialR.color.m3_ref_palette_blue40),
    Yellow(MaterialR.color.m3_ref_palette_yellow40),
}

private fun homepageIconPalette(@DrawableRes iconRes: Int): HomepageIconPalette = when (iconRes) {
    R.drawable.ic_interface_settings,
    R.drawable.ic_custom_font,
    R.drawable.ic_system_small_icons -> HomepageIconPalette.Orange

    R.drawable.ic_background_management,
    R.drawable.ic_background_log_description -> HomepageIconPalette.Green

    R.drawable.ic_sensor_policy,
    R.drawable.ic_spoofing,
    R.drawable.ic_clipboard_policy -> HomepageIconPalette.Cyan

    R.drawable.ic_moment,
    R.drawable.ic_moment_arc_edit,
    R.drawable.ic_statusbarlyric -> HomepageIconPalette.Pink

    R.drawable.ic_launcher_settings -> HomepageIconPalette.Purple
    R.drawable.ic_external_desktop -> HomepageIconPalette.BlueVariant
    R.drawable.ic_appjump -> HomepageIconPalette.Blue
    R.drawable.ic_smart_suggestions -> HomepageIconPalette.Yellow
    R.drawable.ic_ai -> HomepageIconPalette.Purple
    R.drawable.ic_phone_camera -> HomepageIconPalette.Blue
    else -> HomepageIconPalette.Purple
}
