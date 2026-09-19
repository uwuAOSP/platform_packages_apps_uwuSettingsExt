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

package org.uwuaosp.settingsext.navigation;

import android.content.Context;
import android.provider.Settings;

import org.uwuaosp.settingsext.util.SettingsUtils;

public final class NavigationSecureSettings {
    private static final String NAVIGATION_BAR_HINT_KEY = "navigation_bar_hint";

    private NavigationSecureSettings() {
    }

    public static void setNavigationBarHintEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, NAVIGATION_BAR_HINT_KEY, enabled);
    }

    public static boolean isNavigationBarHintEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, NAVIGATION_BAR_HINT_KEY, defaultValue);
    }
}
