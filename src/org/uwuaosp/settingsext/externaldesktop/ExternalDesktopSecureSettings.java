/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.uwuaosp.settingsext.externaldesktop;

import android.content.Context;
import android.provider.Settings;

public final class ExternalDesktopSecureSettings {
    private ExternalDesktopSecureSettings() {
    }

    public static boolean isEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.UWU_EXTERNAL_DESKTOP_ENABLED, 0) != 0;
    }

    public static void setEnabled(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.UWU_EXTERNAL_DESKTOP_ENABLED, enabled ? 1 : 0);
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.MIRROR_BUILT_IN_DISPLAY, enabled ? 0 : 1);
    }

    public static boolean shouldBlankInternalDisplay(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.UWU_EXTERNAL_DESKTOP_BLANK_INTERNAL_DISPLAY, 1) != 0;
    }

    public static void setBlankInternalDisplay(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.UWU_EXTERNAL_DESKTOP_BLANK_INTERNAL_DISPLAY, enabled ? 1 : 0);
    }

    public static boolean allowScrcpyVirtualDisplay(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.UWU_EXTERNAL_DESKTOP_ALLOW_SCRCPY_VIRTUAL_DISPLAY, 1) != 0;
    }

    public static void setAllowScrcpyVirtualDisplay(Context context, boolean enabled) {
        Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.UWU_EXTERNAL_DESKTOP_ALLOW_SCRCPY_VIRTUAL_DISPLAY,
                enabled ? 1 : 0);
    }
}
