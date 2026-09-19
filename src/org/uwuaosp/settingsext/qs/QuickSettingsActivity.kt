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

package org.uwuaosp.settingsext.qs

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme

class QuickSettingsActivity : ComponentActivity() {
    private val requestedStyle = mutableIntStateOf(STYLE_A16)
    private val appliedStyle = mutableIntStateOf(STYLE_A16)
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshStyles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                QuickSettingsScreen(
                    requestedStyle = requestedStyle.intValue,
                    appliedStyle = appliedStyle.intValue,
                    onStyleSelected = ::setStyle,
                    onNavigateUp = ::finish,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.QS_UI_STYLE),
            false,
            observer,
        )
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(QS_UI_STYLE_APPLIED_KEY),
            false,
            observer,
        )
        refreshStyles()
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(observer)
        super.onStop()
    }

    private fun refreshStyles() {
        requestedStyle.intValue = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.QS_UI_STYLE,
            STYLE_A16,
        ).coerceIn(STYLE_UWU, STYLE_A16)
        appliedStyle.intValue = Settings.Secure.getInt(
            contentResolver,
            QS_UI_STYLE_APPLIED_KEY,
            STYLE_A16,
        ).coerceIn(STYLE_UWU, STYLE_A16)
    }

    private fun setStyle(style: Int) {
        if (style == appliedStyle.intValue || requestedStyle.intValue != appliedStyle.intValue) {
            return
        }
        if (!Settings.Secure.putInt(contentResolver, Settings.Secure.QS_UI_STYLE, style)) {
            Toast.makeText(this, R.string.quick_settings_style_update_failed, Toast.LENGTH_SHORT).show()
        }
    }

}

@Composable
private fun QuickSettingsScreen(
    requestedStyle: Int,
    appliedStyle: Int,
    onStyleSelected: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    SettingsScaffold(
        title = stringResource(R.string.quick_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsCategory(title = stringResource(R.string.quick_settings_style_category))
        QuickSettingsStyleSelector(
            requestedStyle = requestedStyle,
            appliedStyle = appliedStyle,
            onStyleSelected = onStyleSelected,
        )
    }
}

@Composable
private fun QuickSettingsStyleSelector(
    requestedStyle: Int,
    appliedStyle: Int,
    onStyleSelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val pending = requestedStyle != appliedStyle
    val label = if (pending) {
        stringResource(R.string.quick_settings_style_applying)
    } else {
        quickSettingsStyleLabel(appliedStyle)
    }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (pending) 0.55f else 1f)
                .clip(RoundedCornerShape(24.dp))
                .clickable(
                    enabled = !pending,
                    role = Role.Button,
                    onClick = { expanded = true },
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.quick_settings_style_selector),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left_down_line),
                            contentDescription = stringResource(R.string.quick_settings_style_selector),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.widthIn(min = 180.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                        shadowElevation = 3.dp,
                    ) {
                        listOf(STYLE_UWU, STYLE_A16).forEachIndexed { index, style ->
                            QuickSettingsStyleMenuItem(
                                text = quickSettingsStyleLabel(style),
                                selected = appliedStyle == style,
                                position = index,
                                itemCount = 2,
                                onClick = {
                                    expanded = false
                                    onStyleSelected(style)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsStyleMenuItem(
    text: String,
    selected: Boolean,
    position: Int,
    itemCount: Int,
    onClick: () -> Unit,
) {
    val shape = if (selected) {
        RoundedCornerShape(12.dp)
    } else if (position == 0) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
    } else if (position == itemCount - 1) {
        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    } else {
        RoundedCornerShape(4.dp)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, color = contentColor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun quickSettingsStyleLabel(style: Int): String =
    stringResource(
        if (style == STYLE_UWU) R.string.quick_settings_style_uwu else R.string.quick_settings_style_default,
    )

private const val STYLE_UWU = 0
private const val STYLE_A16 = 1
private const val QS_UI_STYLE_APPLIED_KEY = "qs_ui_style_applied"
