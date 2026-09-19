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

package org.uwuaosp.settingsext.launcher;

import android.content.Context;
import android.provider.Settings;

import org.uwuaosp.settingsext.util.SettingsUtils;

public final class LauncherSecureSettings {

    private static final String ALL_APPS_THEMED_ICONS_KEY = "launcher_allapps_themed_icons";
    private static final String LENS_ICON_KEY = "launcher_lens_icon";

    private LauncherSecureSettings() {
    }

    public static void setAllAppsThemedIconsEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, ALL_APPS_THEMED_ICONS_KEY, enabled);
    }

    public static boolean isAllAppsThemedIconsEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, ALL_APPS_THEMED_ICONS_KEY, defaultValue);
    }

    public static void setLensIconEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, LENS_ICON_KEY, enabled);
    }

    public static boolean isLensIconEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, LENS_ICON_KEY, defaultValue);
    }
}
