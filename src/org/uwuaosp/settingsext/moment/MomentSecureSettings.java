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

package org.uwuaosp.settingsext.moment;

import android.content.Context;
import android.provider.Settings;

import org.uwuaosp.settingsext.util.SettingsUtils;

public final class MomentSecureSettings {
    private MomentSecureSettings() {
    }

    public static void disableAll(Context context) {
        setEnabled(context, false);
        setArcGestureEnabled(context, false);
        setNavHandleDoubleTapEnabled(context, false);
        setNotificationClickEnabled(context, false);
        setNotificationClickPortraitEnabled(context, false);
        setNotificationClickLandscapeEnabled(context, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(context, Settings.Secure.MOMENT_ENABLED, enabled);
    }

    public static boolean isEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(
                context, Settings.Secure.MOMENT_ENABLED, defaultValue);
    }

    public static boolean isFixedAspectRatio(Context context) {
        return SettingsUtils.getSecureBoolean(
                context, Settings.Secure.MOMENT_FIXED_ASPECT_RATIO, false);
    }

    public static void setFixedAspectRatio(Context context, boolean fixed) {
        SettingsUtils.putSecureBoolean(context, Settings.Secure.MOMENT_FIXED_ASPECT_RATIO, fixed);
    }

    public static void setArcGestureEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(
                context, Settings.Secure.MOMENT_ARC_GESTURE_ENABLED, enabled);
    }

    public static boolean isArcGestureEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(
                context, Settings.Secure.MOMENT_ARC_GESTURE_ENABLED, defaultValue);
    }

    public static void setNavHandleDoubleTapEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(
                context, Settings.Secure.MOMENT_NAV_HANDLE_DOUBLE_TAP_ENABLED, enabled);
    }

    public static boolean isNavHandleDoubleTapEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(
                context, Settings.Secure.MOMENT_NAV_HANDLE_DOUBLE_TAP_ENABLED, defaultValue);
    }

    public static void setNotificationClickEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(
                context, Settings.Secure.MOMENT_NOTIFICATION_CLICK_ENABLED, enabled);
    }

    public static boolean isNotificationClickEnabled(Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(
                context, Settings.Secure.MOMENT_NOTIFICATION_CLICK_ENABLED, defaultValue);
    }

    public static void setNotificationClickPortraitEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(
                context, Settings.Secure.MOMENT_NOTIFICATION_CLICK_PORTRAIT_ENABLED, enabled);
    }

    public static boolean isNotificationClickPortraitEnabled(
            Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context,
                Settings.Secure.MOMENT_NOTIFICATION_CLICK_PORTRAIT_ENABLED, defaultValue);
    }

    public static void setNotificationClickLandscapeEnabled(Context context, boolean enabled) {
        SettingsUtils.putSecureBoolean(
                context, Settings.Secure.MOMENT_NOTIFICATION_CLICK_LANDSCAPE_ENABLED, enabled);
    }

    public static boolean isNotificationClickLandscapeEnabled(
            Context context, boolean defaultValue) {
        return SettingsUtils.getSecureBoolean(context,
                Settings.Secure.MOMENT_NOTIFICATION_CLICK_LANDSCAPE_ENABLED, defaultValue);
    }
}
