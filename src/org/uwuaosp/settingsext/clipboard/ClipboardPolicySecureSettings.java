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
  public static final int OPERATION_READ = 0;
  public static final int OPERATION_WRITE = 1;
  public static final int POLICY_ALLOW = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_ALLOW;
  public static final int POLICY_ASK = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_ASK;
  public static final int POLICY_DENY = Settings.Secure.UWU_APP_CLIPBOARD_POLICY_DENY;

  private ClipboardPolicySecureSettings() {}

  public static boolean isPromptEnabled(Context context) {
    return Settings.Secure.getIntForUser(
                context.getContentResolver(),
                Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
                0,
                UserHandle.myUserId())
            != 0;
  }

  public static boolean setPromptEnabled(Context context, boolean enabled) {
    return Settings.Secure.putIntForUser(
        context.getContentResolver(),
        Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
        enabled ? 1 : 0,
        UserHandle.myUserId());
  }

  public static int getDefaultPolicy(Context context) {
    return isPromptEnabled(context) ? POLICY_ASK : POLICY_ALLOW;
  }

  public static synchronized ArrayMap<String, Integer> getPolicies(Context context, int operation) {
    final ArrayMap<String, Integer> policies = readPolicies(context,
        Settings.Secure.UWU_APP_CLIPBOARD_POLICIES);
    policies.putAll(readPolicies(context, policySetting(operation)));
    return policies;
  }

  private static ArrayMap<String, Integer> readPolicies(Context context, String setting) {
    final ArrayMap<String, Integer> result = new ArrayMap<>();
    final String value =
        Settings.Secure.getStringForUser(
            context.getContentResolver(),
            setting,
            UserHandle.myUserId());
    if (value == null || value.isBlank()) return result;
    try {
      final JSONObject object = new JSONObject(value);
      final Iterator<String> keys = object.keys();
      while (keys.hasNext()) {
        final String packageName = keys.next();
        final int policy = object.optInt(packageName, POLICY_ALLOW);
        if (policy == POLICY_ALLOW || policy == POLICY_ASK || policy == POLICY_DENY) {
          result.put(packageName, policy);
        }
      }
    } catch (JSONException ignored) {
    }
    return result;
  }

  public static synchronized boolean setPolicy(
      Context context, String packageName, int operation, int policy) {
    if (policy != POLICY_ALLOW && policy != POLICY_ASK && policy != POLICY_DENY) {
      return false;
    }
    final String setting = policySetting(operation);
    final TreeMap<String, Integer> policies = new TreeMap<>();
    policies.putAll(readPolicies(context, setting));
    policies.put(packageName, policy);

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
        setting,
        policies.isEmpty() ? null : object.toString(),
        UserHandle.myUserId());
  }

  private static String policySetting(int operation) {
    if (operation == OPERATION_READ) {
      return Settings.Secure.UWU_APP_CLIPBOARD_READ_POLICIES;
    }
    if (operation == OPERATION_WRITE) {
      return Settings.Secure.UWU_APP_CLIPBOARD_WRITE_POLICIES;
    }
    throw new IllegalArgumentException("Unknown clipboard operation: " + operation);
  }
}
