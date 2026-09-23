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

package org.uwuaosp.settingsext.apppicker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.settingslib.AppListEmpty
import org.uwuaosp.compose.settingslib.AppListError
import org.uwuaosp.compose.settingslib.AppListItem
import org.uwuaosp.compose.settingslib.AppListLoading
import org.uwuaosp.compose.settingslib.AppListScaffold
import org.uwuaosp.compose.settingslib.SettingsSectionHeader
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import org.uwuaosp.settingsext.attestation.KeyAttestationSecureSettings
import org.uwuaosp.settingsext.smartsuggestions.SmartSuggestionsSecureSettings

class AppSelectionActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SELECTION_MODE = "selection_mode"
        const val EXTRA_INITIAL_PACKAGE = "initial_package"
        const val EXTRA_RESULT_PACKAGE = "result_package"
        const val EXTRA_RESULT_LABEL = "result_label"
        const val SELECTION_MODE_KEYBOX_EXCLUSION = "keybox_exclusion"
        const val SELECTION_MODE_MUSIC_SUGGESTION = "music_suggestion"
        const val SELECTION_MODE_SINGLE_APP = "single_app"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mode = intent.getStringExtra(EXTRA_SELECTION_MODE) ?: SELECTION_MODE_SINGLE_APP
        setContent {
            SettingsExtTheme {
                AppSelectionScreen(
                    mode = mode,
                    initialPackage = intent.getStringExtra(EXTRA_INITIAL_PACKAGE),
                    onNavigateUp = ::finish,
                    onSingleAppSelected = { app ->
                        when (mode) {
                            SELECTION_MODE_MUSIC_SUGGESTION -> {
                                SmartSuggestionsSecureSettings.setMusicPackage(
                                    this,
                                    app.packageName,
                                )
                                SmartSuggestionsSecureSettings.setMusicEnabled(this, true)
                                setResult(RESULT_OK)
                            }
                            else -> {
                                setResult(
                                    RESULT_OK,
                                    Intent()
                                        .putExtra(EXTRA_RESULT_PACKAGE, app.packageName)
                                        .putExtra(EXTRA_RESULT_LABEL, app.label),
                                )
                            }
                        }
                        finish()
                    },
                )
            }
        }
    }
}

private data class AppPickerEntry(val label: String, val packageName: String, val icon: Bitmap)

@Composable
private fun AppSelectionScreen(
    mode: String,
    initialPackage: String?,
    onNavigateUp: () -> Unit,
    onSingleAppSelected: (AppPickerEntry) -> Unit,
) {
    val context = LocalContext.current
    val multiple = mode == AppSelectionActivity.SELECTION_MODE_KEYBOX_EXCLUSION
    var apps by remember { mutableStateOf<List<AppPickerEntry>>(emptyList()) }
    var selectedPackages by remember {
        mutableStateOf(
            when (mode) {
                AppSelectionActivity.SELECTION_MODE_KEYBOX_EXCLUSION ->
                    KeyAttestationSecureSettings.getExcludedPackages(context).toSet()
                AppSelectionActivity.SELECTION_MODE_MUSIC_SUGGESTION ->
                    setOf(
                        SmartSuggestionsSecureSettings.getMusicPackage(
                            context,
                            context.getString(R.string.default_music_app),
                        )
                    )
                else -> setOfNotNull(initialPackage?.takeIf { it.isNotBlank() })
            }
        )
    }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(reloadToken) {
        loading = true
        loadFailed = false
        runCatching {
                withContext(Dispatchers.IO) {
                    if (mode == AppSelectionActivity.SELECTION_MODE_KEYBOX_EXCLUSION) {
                        loadInstalledApps(context)
                    } else {
                        loadLaunchableApps(context)
                    }
                }
            }
            .onSuccess { loaded ->
                apps = loaded
                if (multiple) {
                    val validSelection =
                        selectedPackages.filterTo(mutableSetOf()) { packageName ->
                            runCatching {
                                    context.packageManager.getApplicationInfo(packageName, 0)
                                }
                                .isSuccess
                        }
                    if (validSelection != selectedPackages) {
                        selectedPackages = validSelection
                        saveMultipleSelection(context, mode, validSelection)
                    }
                }
            }
            .onFailure { loadFailed = true }
        loading = false
    }

    val filteredApps =
        remember(apps, searchQuery) {
            val query = searchQuery.trim()
            if (query.isEmpty()) apps
            else
                apps.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
        }
    val title =
        when (mode) {
            AppSelectionActivity.SELECTION_MODE_KEYBOX_EXCLUSION ->
                stringResource(R.string.key_attestation_excluded_apps_title)
            AppSelectionActivity.SELECTION_MODE_MUSIC_SUGGESTION ->
                stringResource(R.string.switch_music_suggestion_title)
            else -> stringResource(R.string.app_picker_title)
        }

    AppListScaffold(
        title = title,
        searchQuery = searchQuery,
        searchPlaceholder = stringResource(R.string.background_search_apps),
        clearSearchContentDescription = stringResource(R.string.background_search_close),
        onSearchQueryChange = { searchQuery = it },
        onNavigateUp = onNavigateUp,
    ) {
        item(key = "apps_category") {
            SettingsSectionHeader(title = stringResource(R.string.app_picker_all_apps))
        }
        when {
            loading -> item(key = "loading") { AppListLoading() }
            loadFailed ->
                item(key = "error") {
                    AppListError(
                        text = stringResource(R.string.background_load_failed),
                        retryText = stringResource(R.string.background_retry),
                        onRetry = { reloadToken += 1 },
                    )
                }
            filteredApps.isEmpty() ->
                item(key = "empty") {
                    AppListEmpty(text = stringResource(R.string.background_no_apps))
                }
            else ->
                items(count = filteredApps.size, key = { filteredApps[it].packageName }) { index ->
                    val app = filteredApps[index]
                    val selected = app.packageName in selectedPackages
                    androidx.compose.foundation.layout.Column {
                        AppListItem(
                            label = app.label,
                            packageName = app.packageName,
                            icon = app.icon.asImageBitmap(),
                            index = index,
                            itemCount = filteredApps.size,
                            onClick = {
                                if (multiple) {
                                    selectedPackages =
                                        if (selected) {
                                            selectedPackages - app.packageName
                                        } else {
                                            selectedPackages + app.packageName
                                        }
                                    saveMultipleSelection(context, mode, selectedPackages)
                                } else {
                                    onSingleAppSelected(app)
                                }
                            },
                        ) {
                            Checkbox(checked = selected, onCheckedChange = null)
                        }
                    }
                }
        }
    }
}

private fun saveMultipleSelection(context: Context, mode: String, packages: Set<String>) {
    when (mode) {
        AppSelectionActivity.SELECTION_MODE_KEYBOX_EXCLUSION ->
            KeyAttestationSecureSettings.setExcludedPackages(context, packages.toList())
    }
}

private fun loadLaunchableApps(context: Context): List<AppPickerEntry> {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val iconSize = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val apps = LinkedHashMap<String, AppPickerEntry>()
    for (resolveInfo in
        packageManager.queryIntentActivities(
            intent,
            PackageManager.GET_META_DATA or PackageManager.MATCH_ALL,
        )) {
        val packageName = resolveInfo.activityInfo?.packageName ?: continue
        if (packageName in apps) continue
        val label =
            resolveInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank { packageName }
        val icon =
            runCatching {
                    resolveInfo
                        .loadIcon(packageManager)
                        .toBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                }
                .getOrElse {
                    packageManager.defaultActivityIcon.toBitmap(
                        iconSize,
                        iconSize,
                        Bitmap.Config.ARGB_8888,
                    )
                }
        apps[packageName] = AppPickerEntry(label, packageName, icon)
    }
    return apps.values.sortedWith(appEntryComparator())
}

private fun loadInstalledApps(context: Context): List<AppPickerEntry> {
    val packageManager = context.packageManager
    val iconSize = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    return packageManager
        .getInstalledApplications(PackageManager.MATCH_ALL)
        .asSequence()
        .filter { it.packageName != context.packageName }
        .map { applicationInfo ->
            val packageName = applicationInfo.packageName
            val label =
                applicationInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank {
                    packageName
                }
            val icon =
                runCatching {
                        applicationInfo
                            .loadIcon(packageManager)
                            .toBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                    }
                    .getOrElse {
                        packageManager.defaultActivityIcon.toBitmap(
                            iconSize,
                            iconSize,
                            Bitmap.Config.ARGB_8888,
                        )
                    }
            AppPickerEntry(label, packageName, icon)
        }
        .sortedWith(appEntryComparator())
        .toList()
}

private fun appEntryComparator(): Comparator<AppPickerEntry> {
    val collator = Collator.getInstance()
    return Comparator { first, second ->
        val labelOrder = collator.compare(first.label, second.label)
        if (labelOrder != 0) labelOrder else first.packageName.compareTo(second.packageName)
    }
}
