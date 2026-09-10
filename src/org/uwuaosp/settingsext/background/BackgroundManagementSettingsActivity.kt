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

package org.uwuaosp.settingsext.background

import android.content.Context
import android.net.Uri
import android.os.Build
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
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
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.PreferenceGroupSpacer
import org.uwuaosp.compose.settingslib.PreferencePosition
import org.uwuaosp.compose.settingslib.SettingsCategory
import org.uwuaosp.compose.settingslib.SettingsHomepageIcon
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SwitchPreferenceRow
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackgroundManagementSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                BackgroundManagementSettingsScreen(onNavigateUp = ::finish)
            }
        }
    }
}

@Composable
private fun BackgroundManagementSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSystemApps by remember {
        mutableStateOf(BackgroundListPreferences.showSystemApps(context))
    }
    var ignoreTaskRemoval by remember {
        mutableStateOf(BackgroundModeSecureSettings.isIgnoreTaskRemovalEnabled(context))
    }
    var freezerBackend by remember {
        mutableIntStateOf(BackgroundModeSecureSettings.getFreezerBackend(context))
    }
    val kernelStatus = remember { BackgroundModeSecureSettings.getKernelStatus() }
    var backendMenuExpanded by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        exporting = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { BackgroundLogExporter.export(context, uri) }
            }
            exporting = false
            Toast.makeText(
                context,
                if (result.isSuccess) {
                    R.string.background_log_exported
                } else {
                    R.string.background_log_export_failed
                },
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.background_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsCategory(title = stringResource(R.string.background_display_category))
        SwitchPreferenceRow(
            title = stringResource(R.string.background_show_system_apps),
            summary = "",
            showSummary = false,
            checked = showSystemApps,
            onCheckedChange = { show ->
                showSystemApps = show
                BackgroundListPreferences.setShowSystemApps(context, show)
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.background_behavior_category))
        SwitchPreferenceRow(
            title = stringResource(R.string.background_ignore_task_removal),
            summary = stringResource(R.string.background_ignore_task_removal_summary),
            showSummary = true,
            checked = ignoreTaskRemoval,
            position = PreferencePosition.Top,
            onCheckedChange = { enabled ->
                if (BackgroundModeSecureSettings.setIgnoreTaskRemovalEnabled(context, enabled)) {
                    ignoreTaskRemoval = enabled
                } else {
                    Toast.makeText(
                        context,
                        R.string.background_setting_update_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.background_freezer_backend_title),
            summary = "",
            showSummary = false,
            position = PreferencePosition.Bottom,
            onClick = { backendMenuExpanded = true },
            trailingContent = {
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = freezerBackendLabel(freezerBackend),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_left_down_line),
                            contentDescription = stringResource(
                                R.string.background_freezer_backend_title,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = backendMenuExpanded,
                        onDismissRequest = { backendMenuExpanded = false },
                        modifier = Modifier.widthIn(min = 200.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp,
                        shadowElevation = 3.dp,
                    ) {
                        val backends = listOf(
                            BackgroundModeSecureSettings.FREEZER_BACKEND_AUTO,
                            BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP1,
                            BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP2,
                            BackgroundModeSecureSettings.FREEZER_BACKEND_HYBRID,
                        )
                        backends.forEachIndexed { index, backend ->
                            val enabled = when (backend) {
                                BackgroundModeSecureSettings.FREEZER_BACKEND_AUTO -> true
                                BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP1 ->
                                    kernelStatus.cgroup1Freezer
                                BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP2 ->
                                    kernelStatus.cgroup2
                                BackgroundModeSecureSettings.FREEZER_BACKEND_HYBRID ->
                                    kernelStatus.cgroup1Freezer && kernelStatus.cgroup2
                                else -> false
                            }
                            ExpressiveModeMenuItem(
                                text = freezerBackendLabel(backend),
                                selected = freezerBackend == backend,
                                position = index,
                                itemCount = backends.size,
                                enabled = enabled,
                                onClick = {
                                    backendMenuExpanded = false
                                    if (BackgroundModeSecureSettings.setFreezerBackend(
                                            context,
                                            backend,
                                        )
                                    ) {
                                        freezerBackend = backend
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.background_setting_update_failed,
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                },
                            )
                        }
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategory(title = stringResource(R.string.background_diagnostics_category))
        PreferenceRow(
            title = stringResource(R.string.background_kernel_status_title),
            summary = stringResource(
                R.string.background_kernel_status_summary,
                freezerBackendLabel(kernelStatus.automaticBackend),
                if (kernelStatus.binderDevice) {
                    stringResource(R.string.background_status_available)
                } else {
                    stringResource(R.string.background_status_unavailable)
                },
                if (kernelStatus.binderStatsReadable) {
                    stringResource(R.string.background_status_readable)
                } else {
                    stringResource(R.string.background_status_restricted)
                },
            ),
            position = PreferencePosition.Top,
        )
        PreferenceGroupSpacer()
        PreferenceRow(
            title = stringResource(R.string.background_export_logs),
            summary = "",
            showSummary = false,
            enabled = !exporting,
            position = PreferencePosition.Bottom,
            iconContent = {
                SettingsHomepageIcon(iconRes = R.drawable.ic_background_log_description)
            },
            onClick = {
                val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                exportLauncher.launch("uwu-background-$timestamp.log")
            },
        )
    }
}

@Composable
private fun freezerBackendLabel(backend: Int): String {
    return stringResource(
        when (backend) {
            BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP1 ->
                R.string.background_freezer_backend_cgroup1
            BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP2 ->
                R.string.background_freezer_backend_cgroup2
            BackgroundModeSecureSettings.FREEZER_BACKEND_HYBRID ->
                R.string.background_freezer_backend_hybrid
            BackgroundModeSecureSettings.FREEZER_BACKEND_NONE ->
                R.string.background_freezer_backend_unavailable
            else -> R.string.background_freezer_backend_auto
        },
    )
}

private object BackgroundLogExporter {
    fun export(context: Context, uri: Uri) {
        val modes = BackgroundModeSecureSettings.getModes(context)
        val kernelStatus = BackgroundModeSecureSettings.getKernelStatus()
        val backend = BackgroundModeSecureSettings.getFreezerBackend(context)

        val report = buildString {
            appendLine("[INFO] uwuAOSP background management diagnostics")
            appendLine("[INFO] Generated: ${Date()}")
            appendLine("[INFO] Build: ${Build.DISPLAY}")
            appendLine("[INFO] Fingerprint: ${Build.FINGERPRINT}")
            appendLine(
                "[INFO] Ignore task removal: " +
                    BackgroundModeSecureSettings.isIgnoreTaskRemovalEnabled(context),
            )
            appendLine("[INFO] Requested freezer backend: ${backendName(backend)}")
            appendLine(
                "[INFO] Detected cgroup layout: " +
                    backendName(kernelStatus.automaticBackend),
            )
            appendLine("[INFO] Binder device: ${kernelStatus.binderDevice}")
            appendLine("[INFO] Binder stats readable: ${kernelStatus.binderStatsReadable}")
            appendLine()
            appendLine("[INFO] Per-app modes")
            if (modes.isEmpty()) {
                appendLine("[INFO]   (none)")
            } else {
                for (index in 0 until modes.size) {
                    append("[INFO]   ")
                    append(modes.keyAt(index))
                    append('=')
                    appendLine(modeName(modes.valueAt(index)))
                }
            }
            appendLine()
            appendFile("Kernel cgroup controllers", "/proc/cgroups")
            appendFile("Current cgroup membership", "/proc/self/cgroup")
            appendFilteredMountInfo()
            appendFile("CGroup v2 controllers", "/sys/fs/cgroup/cgroup.controllers")
            appendFile("CGroup v2 freezer state", "/sys/fs/cgroup/cgroup.freeze")
            appendFile("CGroup v1 freezer state", "/dev/freezer/freezer.state")
            appendBinderStatus()
            appendLine("[INFO] Framework events")
            append(runCommand(
                "logcat",
                "-d",
                "-v",
                "threadtime",
                "-s",
                "UwuAppBackground:V",
                "*:S",
            ))
        }
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(report)
        } ?: error("Unable to open export destination")
    }

    private fun modeName(mode: Int): String {
        return when (mode) {
            BackgroundModeSecureSettings.MODE_TOMBSTONE -> "TOMBSTONE"
            BackgroundModeSecureSettings.MODE_FULL -> "FULL"
            BackgroundModeSecureSettings.MODE_AUTO -> "AUTO"
            else -> "DEFAULT"
        }
    }

    private fun backendName(backend: Int): String {
        return when (backend) {
            BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP1 -> "CGROUP1"
            BackgroundModeSecureSettings.FREEZER_BACKEND_CGROUP2 -> "CGROUP2"
            BackgroundModeSecureSettings.FREEZER_BACKEND_HYBRID -> "HYBRID"
            BackgroundModeSecureSettings.FREEZER_BACKEND_NONE -> "UNAVAILABLE"
            else -> "AUTO"
        }
    }

    private fun StringBuilder.appendFile(title: String, path: String, limit: Int = 256 * 1024) {
        appendLine("[INFO] $title: $path")
        val file = File(path)
        if (!file.isFile || !file.canRead()) {
            appendLine("[WARN] $path is unavailable or not readable")
            appendLine()
            return
        }
        val content = runCatching {
            file.bufferedReader().use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(4096)
                while (output.length < limit) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, limit - output.length))
                    if (count < 0) break
                    output.append(buffer, 0, count)
                }
                output.toString()
            }
        }
        content.onSuccess {
            append(it)
            if (!it.endsWith('\n')) appendLine()
        }.onFailure {
            appendLine("[WARN] Failed to read $path: ${it.javaClass.simpleName}: ${it.message}")
        }
        appendLine()
    }

    private fun StringBuilder.appendFilteredMountInfo() {
        appendLine("[INFO] CGroup mounts: /proc/self/mountinfo")
        val lines = runCatching {
            File("/proc/self/mountinfo").useLines { sequence ->
                sequence.filter { it.contains(" - cgroup ") || it.contains(" - cgroup2 ") }
                    .toList()
            }
        }
        lines.onSuccess { mounts ->
            if (mounts.isEmpty()) {
                appendLine("[WARN] No cgroup mount found")
            } else {
                mounts.forEach { appendLine(it) }
            }
        }.onFailure {
            appendLine("[WARN] Failed to read cgroup mounts: ${it.message}")
        }
        appendLine()
    }

    private fun StringBuilder.appendBinderStatus() {
        appendLine("[INFO] Binder kernel status")
        listOf("/dev/binder", "/dev/hwbinder", "/dev/vndbinder", "/dev/binderfs/binder")
            .forEach { path -> appendLine("[INFO] $path exists=${File(path).exists()}") }
        val statsPath = listOf(
            "/dev/binderfs/binder_logs/stats",
            "/sys/kernel/debug/binder/stats",
        ).firstOrNull { File(it).canRead() }
        if (statsPath == null) {
            appendLine("[WARN] Binder stats are unavailable or restricted")
            appendLine()
        } else {
            appendFile("Binder stats", statsPath)
        }
    }

    private fun runCommand(vararg command: String): String {
        return runCatching {
            val process = ProcessBuilder(*command).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                output
            } else {
                "[ERROR] ${command.first()} exited with $exitCode\n$output"
            }
        }.getOrElse {
            "[ERROR] Failed to run ${command.first()}: ${it.javaClass.simpleName}: ${it.message}\n"
        }
    }
}
