/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 *
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

package org.uwuaosp.settingsext.qsstyle;

import android.content.Context;
import android.provider.Settings;

import org.uwuaosp.settingsext.util.SettingsUtils;

/**
 * Persists the Quick Settings tile style consumed by SystemUI.
 *
 * <p>The value is stored in {@link Settings.Secure#UWU_QS_STYLE} and observed by SystemUI through a
 * reactive settings repository, so changing it applies immediately without restarting SystemUI.
 */
public final class QSStyleSecureSettings {
    /** Default AOSP tile style. */
    public static final int STYLE_DEFAULT = 0;

    /** Circular tile style with a five column Quick QS panel. */
    public static final int STYLE_CIRCULAR = 1;

    private QSStyleSecureSettings() {
    }

    public static boolean setStyle(Context context, int style) {
        if (style != STYLE_DEFAULT && style != STYLE_CIRCULAR) {
            return false;
        }
        try {
            return Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.UWU_QS_STYLE, style);
        } catch (SecurityException e) {
            return false;
        }
    }

    public static int getStyle(Context context, int defaultValue) {
        return SettingsUtils.getSecureInt(context, Settings.Secure.UWU_QS_STYLE, defaultValue);
    }

    public static boolean isCircular(Context context) {
        return getStyle(context, STYLE_DEFAULT) == STYLE_CIRCULAR;
    }

    public static boolean setCircular(Context context, boolean enabled) {
        return setStyle(context, enabled ? STYLE_CIRCULAR : STYLE_DEFAULT);
    }
}
