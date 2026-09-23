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

package org.uwuaosp.settingsext.lyric;

import android.content.Context;
import android.provider.Settings;

import org.uwuaosp.settingsext.util.SettingsUtils;

public final class LyricSecureSettings {
    public static final int POSITION_OVERLAY = 0;
    public static final int POSITION_CLOCK_RIGHT = 1;

    private LyricSecureSettings() {
    }

    public static void setEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, Settings.Secure.STATUS_BAR_SHOW_LYRIC, enabled);
    }

    public static boolean isEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, Settings.Secure.STATUS_BAR_SHOW_LYRIC, defaultValue);
    }

    public static void setPosition(Context context, int position) {
        SettingsUtils.putSecureInt(context, Settings.Secure.STATUS_BAR_LYRIC_POSITION, position);
    }

    public static int getPosition(Context context, int defaultValue) {
        return SettingsUtils.getSecureInt(context, Settings.Secure.STATUS_BAR_LYRIC_POSITION, defaultValue);
    }

    public static void setShowTranslation(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, Settings.Secure.STATUS_BAR_LYRIC_SHOW_TRANSLATION, enabled);
    }

    public static boolean isShowTranslationEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, Settings.Secure.STATUS_BAR_LYRIC_SHOW_TRANSLATION, defaultValue);
    }

    public static void setWordTimingEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, Settings.Secure.STATUS_BAR_LYRIC_WORD_TIMING, enabled);
    }

    public static boolean isWordTimingEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, Settings.Secure.STATUS_BAR_LYRIC_WORD_TIMING, defaultValue);
    }

    public static void setHideIconOnClockRight(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, Settings.Secure.STATUS_BAR_LYRIC_HIDE_ICON_CLOCK_RIGHT, enabled);
    }

    public static boolean isHideIconOnClockRightEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context, Settings.Secure.STATUS_BAR_LYRIC_HIDE_ICON_CLOCK_RIGHT, defaultValue);
    }

    public static void setSources(Context context, String sources) {
        SettingsUtils.putSecureString(context, Settings.Secure.STATUS_BAR_LYRIC_SOURCES,
                sources == null ? "" : sources.trim());
    }

    public static String getSources(Context context) {
        String sources = SettingsUtils.getSecureString(
                context, Settings.Secure.STATUS_BAR_LYRIC_SOURCES);
        return sources == null ? "" : sources;
    }
}
