/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.uwuaosp.settingsext.apppolicies

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import org.uwuaosp.settingsext.R

class AppPolicyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target =
            Intent(intent).apply {
                component =
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.uwu.apppolicies.AppPolicyActivity",
                    )
                flags =
                    flags and
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                            .inv()
                clipData = null
            }
        try {
            startActivity(target)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, R.string.app_policy_load_failed, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.app_policy_load_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
