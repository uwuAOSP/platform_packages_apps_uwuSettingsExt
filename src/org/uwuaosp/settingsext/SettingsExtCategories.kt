/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.uwuaosp.settingsext

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.google.android.material.R as MaterialR
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.TonalPalette
import kotlin.random.Random
import org.uwuaosp.compose.framework.theme.SettingsDimension
import org.uwuaosp.compose.widget.scaffold.SettingsAppBarScaffold
import org.uwuaosp.compose.widget.ui.SettingsContent
import org.uwuaosp.compose.widget.ui.SettingsIllustrationHeader

internal enum class SettingsExtCategory(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @DrawableRes val icon: Int,
) {
    System(
        R.string.settings_ext_category_system_interface,
        R.string.settings_ext_category_system_interface_summary,
        R.drawable.ic_category_system,
    ),
    Intelligence(
        R.string.settings_ext_category_intelligence,
        R.string.settings_ext_category_intelligence_summary,
        R.drawable.ic_category_intelligence,
    ),
    Privacy(
        R.string.settings_ext_category_privacy_security,
        R.string.settings_ext_category_privacy_security_summary,
        R.drawable.ic_category_privacy,
    ),
}

private val CategorySeeds =
    intArrayOf(
        MaterialR.color.m3_ref_palette_blue40,
        MaterialR.color.m3_ref_palette_pink40,
        MaterialR.color.m3_ref_palette_yellow40,
        MaterialR.color.m3_ref_palette_purple40,
        MaterialR.color.m3_ref_palette_cyan40,
        MaterialR.color.m3_ref_palette_green40,
    )

@Composable
internal fun SettingsExtCategoryHeader(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Size against the content pane so embedded Settings and split-screen stay compact.
        val compact = maxWidth < 600.dp
        val imageWidth =
            minOf(maxWidth * if (compact) 0.75f else 0.65f, if (compact) 226.dp else 250.dp)
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = if (compact) 12.dp else 20.dp)
                    .height(if (compact) 200.dp else 250.dp),
            contentAlignment = Alignment.Center,
        ) {
            SettingsIllustrationHeader(
                imageRes = R.drawable.settings_ext_header_image,
                modifier = Modifier.width(imageWidth),
                height = if (compact) 155.dp else 165.dp,
            )
        }
    }
}

@Composable
internal fun SettingsExtCategoryCards(
    paletteSeed: Int,
    onCategoryClick: (SettingsExtCategory) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsExtCategory.entries.forEach { category ->
            val tint = categoryIconColor(category, paletteSeed)
            Card(
                onClick = { onCategoryClick(category) },
                modifier = Modifier.fillMaxWidth().semantics { role = Role.Button },
                shape = MaterialTheme.shapes.extraLarge,
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(min = 128.dp)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        CategoryIcon(category, tint, 32.dp)
                        CategoryArrow()
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(category.title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(category.summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsExtCategoryPage(
    category: SettingsExtCategory,
    paletteSeed: Int,
    onNavigateUp: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsAppBarScaffold(
        title = stringResource(category.title),
        titleContent = {},
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsContent(
                Modifier.padding(
                    start = SettingsDimension.pagePadding,
                    end = SettingsDimension.pagePadding,
                    bottom = SettingsDimension.pageBottomPadding,
                )
            ) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 30.dp)
                            .semantics(mergeDescendants = true) { heading() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CategoryIcon(category, categoryIconColor(category, paletteSeed), 40.dp)
                    Text(
                        text = stringResource(category.title),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.weight(1f),
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun CategoryIcon(category: SettingsExtCategory, tint: Color, size: Dp) {
    Icon(
        painter = painterResource(category.icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun CategoryArrow() {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun categoryIconColor(category: SettingsExtCategory, paletteSeed: Int): Color {
    val seeds = remember(paletteSeed) { CategorySeeds.toList().shuffled(Random(paletteSeed)) }
    val seed = colorResource(seeds[category.ordinal])
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    return remember(seed, scheme.primary, scheme.surfaceBright, scheme.surfaceContainer, dark) {
        val palette = TonalPalette.fromInt(Blend.harmonize(seed.toArgb(), scheme.primary.toArgb()))
        // Tone 50 is lighter than the demo's light icons; retain 3:1 on cards and page headers.
        val tones = if (dark) intArrayOf(80, 85, 90, 95, 100) else intArrayOf(50, 45, 40, 35, 30, 0)
        val backgrounds =
            intArrayOf(scheme.surfaceBright.toArgb(), scheme.surfaceContainer.toArgb())
        val color =
            tones
                .asSequence()
                .map { palette.tone(it) }
                .first { candidate ->
                    backgrounds.all { ColorUtils.calculateContrast(candidate, it) >= 3.0 }
                }
        Color(color)
    }
}
