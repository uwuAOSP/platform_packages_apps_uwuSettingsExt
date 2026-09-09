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

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.UserHandle
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator

internal data class BackgroundAppEntry(
    val label: String,
    val packageName: String,
    val icon: Bitmap,
    val systemApp: Boolean,
    val configurable: Boolean,
    val mode: Int,
)

internal class BackgroundAppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val iconSizePx =
        (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    fun loadApps(showSystemApps: Boolean): List<BackgroundAppEntry> {
        val userId = UserHandle.myUserId()
        val criticalPackages = collectCriticalPackages(userId)
        val launchablePackages = if (showSystemApps) {
            collectLaunchablePackages(userId)
        } else {
            emptySet()
        }
        val modes = BackgroundModeSecureSettings.getModes(context)
        val apps = packageManager.getInstalledApplicationsAsUser(
            PackageManager.MATCH_DISABLED_COMPONENTS,
            userId,
        )
        val entries = ArrayList<BackgroundAppEntry>(apps.size)
        for (info in apps) {
            val systemApp = info.isSystemApplication()
            if (systemApp && !showSystemApps) continue

            val packageName = info.packageName ?: continue
            val configurable = isConfigurable(info, criticalPackages)
            if (systemApp && (!configurable || packageName !in launchablePackages)) continue
            val label = info.loadLabel(packageManager).toString().ifBlank { packageName }
            val icon = runCatching {
                packageManager.getUserBadgedIcon(
                    info.loadIcon(packageManager),
                    UserHandle.of(userId),
                ).toBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888)
            }.getOrElse {
                packageManager.defaultActivityIcon.toBitmap(
                    iconSizePx,
                    iconSizePx,
                    Bitmap.Config.ARGB_8888,
                )
            }
            entries += BackgroundAppEntry(
                label = label,
                packageName = packageName,
                icon = icon,
                systemApp = systemApp,
                configurable = configurable,
                mode = if (configurable) {
                    modes[packageName] ?: BackgroundModeSecureSettings.MODE_FOLLOW_DEFAULT
                } else {
                    BackgroundModeSecureSettings.MODE_FOLLOW_DEFAULT
                },
            )
        }

        val collator = Collator.getInstance()
        entries.sortWith { first, second ->
            val labelOrder = collator.compare(first.label, second.label)
            if (labelOrder != 0) labelOrder else first.packageName.compareTo(second.packageName)
        }
        return entries
    }

    private fun isConfigurable(
        info: ApplicationInfo,
        criticalPackages: Set<String>,
    ): Boolean {
        if (!UserHandle.isApp(info.uid)) return false
        if ((info.flags and ApplicationInfo.FLAG_PERSISTENT) != 0) return false
        if (info.packageName in criticalPackages) return false
        val sharedPackages = packageManager.getPackagesForUid(info.uid)
        return !info.isSystemApplication() || sharedPackages == null || sharedPackages.size <= 1
    }

    private fun collectCriticalPackages(userId: Int): Set<String> {
        val packages = mutableSetOf(
            "android",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.settings",
            "com.android.systemui",
            context.packageName,
        )
        addResolvedPackages(
            packages,
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            userId,
        )
        addResolvedPackages(
            packages,
            Intent(Intent.ACTION_INSTALL_PACKAGE).setData(Uri.parse("package:example")),
            userId,
        )
        packageManager.permissionControllerPackageName?.let(packages::add)

        context.getSystemService(InputMethodManager::class.java)
            ?.inputMethodList
            ?.mapTo(packages) { it.packageName }
        context.getSystemService(DevicePolicyManager::class.java)
            ?.activeAdmins
            ?.mapTo(packages) { it.packageName }
        context.getSystemService(TelecomManager::class.java)
            ?.defaultDialerPackage
            ?.let(packages::add)
        return packages
    }

    private fun addResolvedPackages(packages: MutableSet<String>, intent: Intent, userId: Int) {
        packageManager.queryIntentActivitiesAsUser(intent, PackageManager.MATCH_ALL, userId)
            .mapNotNullTo(packages) { it.activityInfo?.packageName }
    }

    private fun collectLaunchablePackages(userId: Int): Set<String> {
        val packages = mutableSetOf<String>()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivitiesAsUser(intent, 0, userId)
            .mapNotNullTo(packages) { it.activityInfo?.packageName }
        return packages
    }

    private fun ApplicationInfo.isSystemApplication(): Boolean {
        return (flags and (ApplicationInfo.FLAG_SYSTEM or
            ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
    }
}

internal object BackgroundListPreferences {
    private const val FILE_NAME = "background_management"
    private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"

    fun showSystemApps(context: Context): Boolean {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_SYSTEM_APPS, false)
    }

    fun setShowSystemApps(context: Context, show: Boolean) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_SYSTEM_APPS, show)
            .apply()
    }
}
