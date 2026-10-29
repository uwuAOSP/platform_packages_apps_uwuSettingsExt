/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.uwuaosp.settingsext.appjump;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class AppJumpSettingsActivity extends Activity {
    private static final String EXTRA_DESTINATION = "destination";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private static final String EXTRA_SOURCE_PACKAGE = "source_package";
    private static final String EXTRA_TARGET_PACKAGE = "target_package";

    private static final String DESTINATION_OVERVIEW = "overview";
    private static final String DESTINATION_ALL_APPS = "all_apps";
    private static final String DESTINATION_CATEGORY = "category";
    private static final String DESTINATION_APP_DETAIL = "app_detail";
    private static final String DESTINATION_SOURCE_RULES = "source_rules";
    private static final String DESTINATION_TARGET_RULES = "target_rules";
    private static final String DESTINATION_TARGET_RULE_DETAIL = "target_rule_detail";

    public static Intent createIntent(Context context) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_OVERVIEW);
    }

    public static Intent createCategoryIntent(Context context, int category) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_CATEGORY)
                .putExtra(EXTRA_CATEGORY, category);
    }

    public static Intent createAllAppsIntent(Context context) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_ALL_APPS);
    }

    public static Intent createAppDetailIntent(Context context, String packageName) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_APP_DETAIL)
                .putExtra(EXTRA_PACKAGE_NAME, packageName);
    }

    public static Intent createTargetRulesIntent(Context context, String sourcePackage) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_TARGET_RULES)
                .putExtra(EXTRA_SOURCE_PACKAGE, sourcePackage);
    }

    public static Intent createSourceRulesIntent(Context context, String targetPackage) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_SOURCE_RULES)
                .putExtra(EXTRA_TARGET_PACKAGE, targetPackage);
    }

    public static Intent createTargetRuleDetailIntent(
            Context context, String sourcePackage, String targetPackage) {
        return new Intent(context, AppJumpSettingsActivity.class)
                .putExtra(EXTRA_DESTINATION, DESTINATION_TARGET_RULE_DETAIL)
                .putExtra(EXTRA_SOURCE_PACKAGE, sourcePackage)
                .putExtra(EXTRA_TARGET_PACKAGE, targetPackage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent target =
                new Intent(getIntent())
                        .setComponent(
                                new android.content.ComponentName(
                                        "com.android.settings",
                                        "com.android.settings.uwu.appjump.AppJumpSettingsActivity"));
        target.setFlags(
                target.getFlags()
                        & ~(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION));
        target.setClipData(null);
        try {
            startActivity(target);
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            android.widget.Toast.makeText(
                            this,
                            org.uwuaosp.settingsext.R.string.app_policy_load_failed,
                            android.widget.Toast.LENGTH_SHORT)
                    .show();
        }
        finish();
    }
}
