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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

internal enum class SystemIconStyle {
    DEFAULT,
    PUI,
}

internal class PuiSystemIconController(context: Context) {
    private val context = context.applicationContext

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
        suspendCancellableCoroutine { continuation ->
            val intent =
                Intent(ACTION_SET_STYLE)
                    .setClassName(SETTINGS_PACKAGE, SETTINGS_RECEIVER)
                    .putExtra(EXTRA_ENABLED, style == SystemIconStyle.PUI)
            val resultReceiver =
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (continuation.isActive) {
                            continuation.resume(resultCode == Activity.RESULT_OK)
                        }
                    }
                }
            try {
                context.sendOrderedBroadcast(
                    intent,
                    null,
                    resultReceiver,
                    null,
                    Activity.RESULT_CANCELED,
                    null,
                    null,
                )
            } catch (_: RuntimeException) {
                if (continuation.isActive) continuation.resume(false)
            }
        }

    private companion object {
        const val ACTION_SET_STYLE = "org.uwuaosp.intent.action.SET_PUI_SYSTEM_ICON_STYLE"
        const val SETTINGS_PACKAGE = "com.android.settings"
        const val SETTINGS_RECEIVER = "com.android.settings.overlay.PuiSystemIconReceiver"
        const val EXTRA_ENABLED = "enabled"
        const val SETTING_STYLE = "uwu_pui_system_icon_style"
    }
}
