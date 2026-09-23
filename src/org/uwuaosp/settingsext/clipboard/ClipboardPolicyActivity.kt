/* Copyright (C) 2026 The uwuAOSP Project */

package org.uwuaosp.settingsext.clipboard

import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import org.uwuaosp.settingsext.background.ExpressiveModeMenuItem

class ClipboardPolicyActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)
    private val observer =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshToken.intValue++
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsExtTheme { ClipboardPolicyScreen(refreshToken.intValue, ::finish) } }
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.UWU_APP_CLIPBOARD_POLICIES),
            false,
            observer,
        )
        refreshToken.intValue++
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(observer)
        super.onStop()
    }
}

@Composable
private fun ClipboardPolicyScreen(refreshToken: Int, onNavigateUp: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { ClipboardPolicyAppRepository(context) }
    var apps by remember { mutableStateOf<List<ClipboardPolicyAppEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var retryToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshToken, retryToken) {
        loading = true
        failed = false
        runCatching { withContext(Dispatchers.IO) { repository.loadApps() } }
            .onSuccess { apps = it }
            .onFailure { failed = true }
        loading = false
    }

    val filtered =
        apps.filter {
            query.isBlank() ||
                it.label.contains(query, true) ||
                it.packageName.contains(query, true)
        }
    AppListScaffold(
        title = stringResource(R.string.clipboard_policy_title),
        searchQuery = query,
        searchPlaceholder = stringResource(R.string.clipboard_policy_search_apps),
        clearSearchContentDescription = stringResource(R.string.clipboard_policy_search_close),
        onSearchQueryChange = { query = it },
        onNavigateUp = onNavigateUp,
    ) {
        item { SettingsSectionHeader(title = stringResource(R.string.clipboard_policy_apps_category)) }
        when {
            loading -> item { AppListLoading() }
            failed ->
                item {
                    AppListError(
                        text = stringResource(R.string.clipboard_policy_load_failed),
                        retryText = stringResource(R.string.clipboard_policy_retry),
                        onRetry = { retryToken++ },
                    )
                }
            filtered.isEmpty() ->
                item { AppListEmpty(text = stringResource(R.string.clipboard_policy_no_apps)) }
            else ->
                items(filtered.size, key = { filtered[it].packageName }) { index ->
                    val app = filtered[index]
                    Column {
                        ClipboardPolicyRow(
                            app = app,
                            index = index,
                            itemCount = filtered.size,
                        ) { policy ->
                            if (
                                ClipboardPolicySecureSettings.setPolicy(
                                    context,
                                    app.packageName,
                                    policy,
                                )
                            ) {
                                apps =
                                    apps.map { entry ->
                                        if (entry.packageName == app.packageName) {
                                            entry.copy(policy = policy)
                                        } else {
                                            entry
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                        context,
                                        R.string.clipboard_policy_update_failed,
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun ClipboardPolicyRow(
    app: ClipboardPolicyAppEntry,
    index: Int,
    itemCount: Int,
    onPolicySelected: (Int) -> Unit,
) {
    var expanded by remember(app.packageName) { mutableStateOf(false) }
    val policyLabel = clipboardPolicyLabel(app.policy)
    AppListItem(
        label = app.label,
        packageName = app.packageName,
        icon = app.icon.asImageBitmap(),
        index = index,
        itemCount = itemCount,
        onClick = { expanded = true },
    ) {
        Box {
            Row(
                modifier =
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClick = { expanded = true })
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = policyLabel,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left_down_line),
                    contentDescription = stringResource(R.string.clipboard_policy_app_policy),
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
                val policies =
                    listOf(
                        ClipboardPolicySecureSettings.POLICY_ALLOW,
                        ClipboardPolicySecureSettings.POLICY_ASK,
                        ClipboardPolicySecureSettings.POLICY_DENY,
                    )
                policies.forEachIndexed { index, policy ->
                    ExpressiveModeMenuItem(
                        text = clipboardPolicyLabel(policy),
                        selected = app.policy == policy,
                        position = index,
                        itemCount = policies.size,
                        onClick = {
                            expanded = false
                            onPolicySelected(policy)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun clipboardPolicyLabel(policy: Int): String {
    return stringResource(
        when (policy) {
            ClipboardPolicySecureSettings.POLICY_ASK -> R.string.clipboard_policy_ask
            ClipboardPolicySecureSettings.POLICY_DENY -> R.string.clipboard_policy_deny
            else -> R.string.clipboard_policy_allow
        }
    )
}
