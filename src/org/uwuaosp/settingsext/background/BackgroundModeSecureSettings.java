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

package org.uwuaosp.settingsext.background;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public final class BackgroundModeSecureSettings {
    /** UI-only value for apps without an explicit mode: they follow the default mode. */
    public static final int MODE_FOLLOW_DEFAULT = -1;

    public static final int MODE_DEFAULT =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_DEFAULT;
    public static final int MODE_TOMBSTONE =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_TOMBSTONE;
    public static final int MODE_FULL =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_FULL;
    public static final int MODE_AUTO =
            Settings.Secure.UWU_APP_BACKGROUND_MODE_AUTO;

    public static final int FREEZER_BACKEND_AUTO =
            Settings.Global.UWU_APP_BACKGROUND_FREEZER_BACKEND_AUTO;
    public static final int FREEZER_BACKEND_CGROUP1 =
            Settings.Global.UWU_APP_BACKGROUND_FREEZER_BACKEND_CGROUP1;
    public static final int FREEZER_BACKEND_CGROUP2 =
            Settings.Global.UWU_APP_BACKGROUND_FREEZER_BACKEND_CGROUP2;
    public static final int FREEZER_BACKEND_HYBRID =
            Settings.Global.UWU_APP_BACKGROUND_FREEZER_BACKEND_HYBRID;
    public static final int FREEZER_BACKEND_NONE = -1;

    private BackgroundModeSecureSettings() {
    }

    public static boolean isIgnoreTaskRemovalEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_IGNORE_TASK_REMOVAL, 0,
                UserHandle.myUserId()) != 0;
    }

    public static boolean setIgnoreTaskRemovalEnabled(Context context, boolean enabled) {
        return Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_IGNORE_TASK_REMOVAL, enabled ? 1 : 0,
                UserHandle.myUserId());
    }

    public static int getFreezerBackend(Context context) {
        return normalizeFreezerBackend(Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.UWU_APP_BACKGROUND_FREEZER_BACKEND, FREEZER_BACKEND_AUTO));
    }

    public static boolean setFreezerBackend(Context context, int backend) {
        return Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.UWU_APP_BACKGROUND_FREEZER_BACKEND,
                normalizeFreezerBackend(backend));
    }

    public static synchronized ArrayMap<String, Integer> getModes(Context context) {
        final ArrayMap<String, Integer> modes = new ArrayMap<>();
        final String value = Settings.Secure.getStringForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_MODES, UserHandle.myUserId());
        if (value == null || value.isBlank()) {
            return modes;
        }
        try {
            final JSONObject object = new JSONObject(value);
            final Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                final String packageName = keys.next();
                final int mode = object.optInt(packageName, MODE_DEFAULT);
                if (mode == MODE_DEFAULT || mode == MODE_TOMBSTONE || mode == MODE_FULL
                        || mode == MODE_AUTO) {
                    modes.put(packageName, mode == MODE_AUTO ? MODE_TOMBSTONE : mode);
                }
            }
        } catch (JSONException ignored) {
            // system_server normalizes malformed values; the UI treats them as default.
        }
        return modes;
    }

    public static synchronized boolean setMode(
            Context context, String packageName, int mode) {
        final TreeMap<String, Integer> modes = new TreeMap<>();
        modes.putAll(getModes(context));
        if (mode == MODE_DEFAULT || mode == MODE_TOMBSTONE || mode == MODE_FULL
                || mode == MODE_AUTO) {
            modes.put(packageName, mode == MODE_AUTO ? MODE_TOMBSTONE : mode);
        } else {
            modes.remove(packageName);
        }

        final JSONObject object = new JSONObject();
        for (Map.Entry<String, Integer> entry : modes.entrySet()) {
            try {
                object.put(entry.getKey(), entry.getValue());
            } catch (JSONException impossible) {
                throw new AssertionError(impossible);
            }
        }
        final String value = modes.isEmpty() ? null : object.toString();
        return Settings.Secure.putStringForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_MODES, value, UserHandle.myUserId());
    }

    /** Returns the mode applied to apps without an explicit per-app mode. */
    public static synchronized int getDefaultMode(Context context) {
        final int value = Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_DEFAULT_MODE, MODE_DEFAULT,
                UserHandle.myUserId());
        return value == MODE_TOMBSTONE || value == MODE_FULL ? value : MODE_DEFAULT;
    }

    public static synchronized boolean setDefaultMode(Context context, int mode) {
        if (mode != MODE_DEFAULT && mode != MODE_TOMBSTONE && mode != MODE_FULL) {
            return false;
        }
        return Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.UWU_APP_BACKGROUND_DEFAULT_MODE, mode, UserHandle.myUserId());
    }

    public static KernelStatus getKernelStatus() {
        final String mountInfo = readTextFile("/proc/self/mountinfo", 1024 * 1024);
        boolean cgroup1Freezer = false;
        boolean cgroup2 = false;
        if (mountInfo != null) {
            for (String line : mountInfo.split("\\n")) {
                final int separator = line.indexOf(" - ");
                if (separator < 0) {
                    continue;
                }
                final String mount = line.substring(0, separator);
                final String filesystem = line.substring(separator + 3);
                if (filesystem.startsWith("cgroup2 ")) {
                    cgroup2 = true;
                } else if (filesystem.startsWith("cgroup ")
                        && (filesystem.contains("freezer") || mount.contains("/freezer"))) {
                    cgroup1Freezer = true;
                }
            }
        }
        final boolean binderDevice = new File("/dev/binder").exists()
                || new File("/dev/binderfs/binder").exists();
        final boolean binderStats = new File("/dev/binderfs/binder_logs/stats").canRead()
                || new File("/sys/kernel/debug/binder/stats").canRead();
        return new KernelStatus(cgroup1Freezer, cgroup2, binderDevice, binderStats);
    }

    private static int normalizeFreezerBackend(int backend) {
        switch (backend) {
            case FREEZER_BACKEND_AUTO:
            case FREEZER_BACKEND_CGROUP1:
            case FREEZER_BACKEND_CGROUP2:
            case FREEZER_BACKEND_HYBRID:
                return backend;
            default:
                return FREEZER_BACKEND_AUTO;
        }
    }

    private static String readTextFile(String path, int maxChars) {
        final StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            final char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) > 0 && output.length() < maxChars) {
                output.append(buffer, 0, Math.min(count, maxChars - output.length()));
            }
            return output.toString();
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    public static final class KernelStatus {
        public final boolean cgroup1Freezer;
        public final boolean cgroup2;
        public final boolean binderDevice;
        public final boolean binderStatsReadable;

        KernelStatus(boolean cgroup1Freezer, boolean cgroup2, boolean binderDevice,
                boolean binderStatsReadable) {
            this.cgroup1Freezer = cgroup1Freezer;
            this.cgroup2 = cgroup2;
            this.binderDevice = binderDevice;
            this.binderStatsReadable = binderStatsReadable;
        }

        public int getAutomaticBackend() {
            if (cgroup1Freezer && cgroup2) {
                return FREEZER_BACKEND_HYBRID;
            }
            if (cgroup1Freezer) {
                return FREEZER_BACKEND_CGROUP1;
            }
            return cgroup2 ? FREEZER_BACKEND_CGROUP2 : FREEZER_BACKEND_NONE;
        }
    }
}
