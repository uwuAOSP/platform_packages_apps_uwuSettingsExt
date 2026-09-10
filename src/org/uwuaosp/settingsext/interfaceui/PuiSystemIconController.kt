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

package org.uwuaosp.settingsext.interfaceui

import android.content.Context
import android.content.om.OverlayIdentifier
import android.content.om.OverlayInfo
import android.content.om.OverlayManager
import android.content.om.OverlayManagerTransaction
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class SystemIconStyle {
    DEFAULT,
    PUI,
}

internal class PuiSystemIconController(context: Context) {
    private val context = context.applicationContext
    private val overlayManager = context.getSystemService(OverlayManager::class.java)

    fun activeStyle(): SystemIconStyle {
        val enabled =
            Settings.Secure.getIntForUser(
                context.contentResolver,
                SETTING_STYLE,
                0,
                UserHandle.myUserId(),
            ) == 1
        return if (enabled) SystemIconStyle.PUI else SystemIconStyle.DEFAULT
    }

    suspend fun setStyle(style: SystemIconStyle): Boolean =
        withContext(Dispatchers.IO) {
            val enabled = style == SystemIconStyle.PUI
            val userId = UserHandle.myUserId()
            val user = UserHandle.of(userId)
            val transaction = OverlayManagerTransaction.Builder()
            var compatibleCount = 0
            var requestCount = 0

            try {
                OVERLAY_PACKAGES.forEach { packageName ->
                    val info = overlayManager.getOverlayInfo(packageName, user) ?: return@forEach
                    if (
                        !info.isMutable ||
                            (info.state != OverlayInfo.STATE_DISABLED &&
                                info.state != OverlayInfo.STATE_ENABLED)
                    ) {
                        return@forEach
                    }
                    compatibleCount++
                    if (info.isEnabled != enabled) {
                        transaction.setEnabled(OverlayIdentifier(packageName), enabled, userId)
                        requestCount++
                    }
                }

                check(compatibleCount > 0) { "No compatible PUI system icon overlays are installed" }
                if (requestCount > 0) overlayManager.commit(transaction.build())
                Settings.Secure.putIntForUser(
                    context.contentResolver,
                    SETTING_STYLE,
                    if (enabled) 1 else 0,
                    userId,
                )
            } catch (exception: RuntimeException) {
                Log.e(TAG, "Unable to update PUI system icon overlays", exception)
                return@withContext false
            }
        }

    private companion object {
        const val TAG = "PuiSystemIconController"
        const val SETTING_STYLE = "uwu_pui_system_icon_style"

        val OVERLAY_PACKAGES =
            arrayOf(
                "com.android.settings.PUIThemeAPPDetailsIcon",
                "com.launcher.PUIThemeSystemUIScreenRecordeIcon",
                "com.android.systemui.PUIThemeAluminumOSQuickPanelIcon",
                "com.android.settings.PUIThemeAluminumOSSettingIcon",
                "com.android.PUIThemeAndroidLOGOIcon",
                "com.android.PUIThemeAndroidQuickPanelIcon",
                "com.android.PUIThemeAndroidStatIcon",
                "com.android.systemui.PUIThemeBrightIcon",
                "com.android.systemui.PUIThemeBubbleIcon",
                "com.android.settings.PUIThemeDarkThemeIcon",
                "com.android.systemui.PUIThemeFingerprintIcon",
                "com.android.systemui.PUIThemeLockScreenIcon",
                "com.android.systemui.PUIThemeLunarisAOSPVolumeIcon",
                "com.android.systemui.PUIThemeMIADBrightIcon",
                "com.android.systemui.PUIThemeMIADStatIcon",
                "com.android.systemui.PUIThemeNeotericOSBrightnessIcon",
                "com.android.systemui.PUIThemeNoInternetWifiSignal",
                "com.android.systemui.PUIThemeOneHandedIcon",
                "com.android.PUIThemePowerPanelIconFramework",
                "com.android.systemui.PUIThemePowerPanelIconSystemUI",
                "com.android.systemui.PUIThemePrivacyIcon",
                "com.android.settings.PUIThemeSettingsPermissionsIcon",
                "com.android.PUIThemeSignalIcon",
                "com.android.systemui.PUIThemeSignalIconA16QPRBeta",
                "com.android.systemui.PUIThemeSignalLOGOA16QPRBeta",
                "com.android.systemui.PUIThemeSignalLogoIcon",
                "com.android.systemui.PUISonyHDIcon",
                "com.android.launcher3.PUIThemeSysbarIconAOSP",
                "com.android.systemui.PUIThemeSystemUILockScreenIcon",
                "com.android.systemui.PUIThemeSystemUIQuickPanelIcon",
                "com.android.systemui.PUIThemeSystemUIScreenRecordeIcon",
                "com.android.systemui.PUIThemeSystemUIStatIcon",
                "com.android.systemui.PUIThemeVolumePanelIcon",
                "com.android.systemui.PUIThemedLineagesBrightIcon",
                "com.android.systemui.PUIThemedLineagesQuickPanelIcon",
                "com.android.systemui.PUIThemedLineagesRingIcon",
                "com.android.settings.PUIThemedLineagesSettingsIcon",
                "com.android.PUIThemedLineagesUSBAndroidIcons",
                "com.android.settings.PUIThemedNX1SettingsIcon",
                "com.android.systemui.PUIThemedQSIconCrd",
                "com.android.settings.PUIThemedSettingsIcon",
                "com.android.settings.PUIThemedSettingsIconBaklava",
                "com.android.settings.PUIThemedSettingsIconCrd",
                "com.android.launcher3.PUIThemedcrDroidHomeIcon",
                "com.android.settings.PUIThemeduwuAOSPSettingsIcon",
                "org.uwuaosp.settingsext.PUIThemeuwuAOSPExpandSettingsIcon",
                "com.android.systemui.PUIThemeuwuAOSPSystemUIIcon",
                "com.android.PUIThmemBatteryIcon",
                "com.android.systemui.PuiThemeMIADBatteryIcon",
            )
    }
}
