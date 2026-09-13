/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.uwuaosp.settingsext.clipboard;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONException;
import org.json.JSONObject;

public final class ClipboardPolicySecureSettings {
  public static final int POLICY_ALLOW = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_ALLOW;
  public static final int POLICY_ASK = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_ASK;
  public static final int POLICY_DENY = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_DENY;

  private ClipboardPolicySecureSettings() {}

  public static synchronized ArrayMap<String, Integer> getPolicies(Context context) {
    final ArrayMap<String, Integer> result = new ArrayMap<>();
    final String value =
        Settings.Secure.getStringForUser(
            context.getContentResolver(),
            Settings.Secure.UWU_APP_CLIPBOARD_POLICIES,
            UserHandle.myUserId());
    if (value == null || value.isBlank()) return result;
    try {
      final JSONObject object = new JSONObject(value);
      final Iterator<String> keys = object.keys();
      while (keys.hasNext()) {
        final String packageName = keys.next();
        final int policy = object.optInt(packageName, POLICY_ALLOW);
        if (policy == POLICY_ASK || policy == POLICY_DENY) {
          result.put(packageName, policy);
        }
      }
    } catch (JSONException ignored) {
    }
    return result;
  }

  public static synchronized boolean setPolicy(Context context, String packageName, int policy) {
    final TreeMap<String, Integer> policies = new TreeMap<>();
    policies.putAll(getPolicies(context));
    if (policy == POLICY_ASK || policy == POLICY_DENY) {
      policies.put(packageName, policy);
    } else {
      policies.remove(packageName);
    }

    final JSONObject object = new JSONObject();
    try {
      for (Map.Entry<String, Integer> entry : policies.entrySet()) {
        object.put(entry.getKey(), entry.getValue());
      }
    } catch (JSONException impossible) {
      throw new AssertionError(impossible);
    }
    return Settings.Secure.putStringForUser(
        context.getContentResolver(),
        Settings.Secure.UWU_APP_CLIPBOARD_POLICIES,
        policies.isEmpty() ? null : object.toString(),
        UserHandle.myUserId());
  }
}
