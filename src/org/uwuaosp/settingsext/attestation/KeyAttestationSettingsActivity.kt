/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.uwuaosp.settingsext.attestation

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.StringReader
import org.uwuaosp.compose.settingslib.PreferenceRow
import org.uwuaosp.compose.settingslib.SettingsFooterLegacy
import org.uwuaosp.compose.settingslib.SettingsScaffold
import org.uwuaosp.compose.settingslib.SettingsSection
import org.uwuaosp.settingsext.R
import org.uwuaosp.settingsext.SettingsExtTheme
import org.uwuaosp.settingsext.apppicker.AppSelectionActivity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class KeyAttestationSettingsActivity : ComponentActivity() {
    private val refreshToken = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SettingsExtTheme {
                KeyAttestationSettingsScreen(
                    refreshToken = refreshToken.intValue,
                    onNavigateUp = ::finish,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshToken.intValue += 1
    }
}

@Composable
private fun KeyAttestationSettingsScreen(refreshToken: Int, onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    var keyboxData by
        remember(refreshToken) {
            mutableStateOf(KeyAttestationSecureSettings.getKeyboxData(context))
        }
    var pifData by
        remember(refreshToken) { mutableStateOf(KeyAttestationSecureSettings.getPifData(context)) }
    val excludedPackageCount =
        remember(refreshToken) { KeyAttestationSecureSettings.getExcludedPackages(context).size }
    val keyboxPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val xml = readDocument(context, uri) ?: return@rememberLauncherForActivityResult
            if (!validateKeyboxXml(xml)) {
                Toast.makeText(context, R.string.key_attestation_invalid_xml, Toast.LENGTH_SHORT)
                    .show()
                return@rememberLauncherForActivityResult
            }
            KeyAttestationSecureSettings.setKeyboxData(context, xml)
            keyboxData = xml
            Toast.makeText(context, R.string.key_attestation_xml_loaded, Toast.LENGTH_SHORT).show()
        }
    val pifPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val json = readDocument(context, uri) ?: return@rememberLauncherForActivityResult
            KeyAttestationSecureSettings.setPifData(context, json)
            pifData = json
            forceStopPackages(context, PIF_TARGET_PACKAGES)
            Toast.makeText(context, R.string.key_attestation_json_loaded, Toast.LENGTH_SHORT).show()
        }

    SettingsScaffold(
        title = stringResource(R.string.key_attestation_settings_title),
        showBackButton = true,
        onNavigateUp = onNavigateUp,
    ) {
        SettingsSection(title = stringResource(R.string.key_attestation_keybox_category)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.keybox_data_title),
            summary = stringResource(R.string.keybox_data_summary),
            onClick = { keyboxPicker.launch(KEYBOX_MIME_TYPES) },
                )
            }
            item {
                PreferenceRow(
            title = stringResource(R.string.key_attestation_clear_keybox_title),
            summary = stringResource(R.string.key_attestation_clear_keybox_summary),
            enabled = !keyboxData.isNullOrBlank(),
            onClick = {
                KeyAttestationSecureSettings.setKeyboxData(context, null)
                keyboxData = null
                Toast.makeText(context, R.string.key_attestation_xml_cleared, Toast.LENGTH_SHORT)
                    .show()
            },
                )
            }
            item {
                SettingsFooterLegacy(
                    KeyAttestationSummaryUtils.buildKeyboxFooterSummary(context, keyboxData).toString()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.key_attestation_apps_category)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.key_attestation_excluded_apps_title),
            summary =
                if (excludedPackageCount == 0) {
                    stringResource(R.string.key_attestation_excluded_apps_empty)
                } else {
                    pluralStringResource(
                        R.plurals.key_attestation_excluded_apps_count,
                        excludedPackageCount,
                        excludedPackageCount,
                    )
                },
            onClick = {
                context.startActivity(
                    Intent(context, AppSelectionActivity::class.java)
                        .putExtra(
                            AppSelectionActivity.EXTRA_SELECTION_MODE,
                            AppSelectionActivity.SELECTION_MODE_KEYBOX_EXCLUSION,
                        )
                )
            },
                )
            }
            item {
                SettingsFooterLegacy(stringResource(R.string.key_attestation_excluded_apps_footer))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsSection(title = stringResource(R.string.key_attestation_pif_category)) {
            item {
                PreferenceRow(
            title = stringResource(R.string.pif_data_title),
            summary = stringResource(R.string.pif_data_summary),
            onClick = { pifPicker.launch(PIF_MIME_TYPES) },
                )
            }
            item {
                PreferenceRow(
            title = stringResource(R.string.key_attestation_clear_pif_title),
            summary = stringResource(R.string.key_attestation_clear_pif_summary),
            enabled = !pifData.isNullOrBlank(),
            onClick = {
                KeyAttestationSecureSettings.setPifData(context, null)
                pifData = null
                forceStopPackages(context, PIF_TARGET_PACKAGES)
                Toast.makeText(context, R.string.key_attestation_json_cleared, Toast.LENGTH_SHORT)
                    .show()
            },
                )
            }
            item {
                SettingsFooterLegacy(
                    KeyAttestationSummaryUtils.buildPifFooterSummary(context, pifData).toString()
                )
            }
        }
    }
}

private fun readDocument(context: Context, uri: Uri): String? =
    runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Document has no readable stream")
        }
        .onFailure {
            Log.e(TAG, "Failed to read selected document", it)
            Toast.makeText(context, R.string.key_attestation_file_read_failed, Toast.LENGTH_SHORT)
                .show()
        }
        .getOrNull()

private fun validateKeyboxXml(xml: String): Boolean {
    var numberOfKeyboxes = -1
    val algorithms = mutableSetOf<String>()
    val privateKeyAlgorithms = mutableSetOf<String>()
    val certificateCounts = mutableMapOf<String, Int>()
    return runCatching {
            val parser =
                XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(StringReader(xml))
                }
            var currentAlgorithm: String? = null
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG ->
                        when (parser.name) {
                            "NumberOfKeyboxes" ->
                                numberOfKeyboxes = parser.nextText().trim().toInt()
                            "Key" -> {
                                currentAlgorithm =
                                    parser
                                        .getAttributeValue(null, "algorithm")
                                        ?.lowercase()
                                        ?.takeIf { it == "ecdsa" || it == "rsa" }
                                currentAlgorithm?.let(algorithms::add)
                            }
                            "PrivateKey" ->
                                if (
                                    parser
                                        .getAttributeValue(null, "format")
                                        .equals("pem", ignoreCase = true)
                                ) {
                                    currentAlgorithm?.let(privateKeyAlgorithms::add)
                                } else {
                                    return false
                                }
                            "Certificate" ->
                                if (
                                    parser
                                        .getAttributeValue(null, "format")
                                        .equals("pem", ignoreCase = true)
                                ) {
                                    currentAlgorithm?.let { algorithm ->
                                        certificateCounts[algorithm] =
                                            certificateCounts.getOrDefault(algorithm, 0) + 1
                                    }
                                } else {
                                    return false
                                }
                        }
                    XmlPullParser.END_TAG -> if (parser.name == "Key") currentAlgorithm = null
                }
            }
            numberOfKeyboxes == 1 &&
                algorithms.containsAll(REQUIRED_KEYBOX_ALGORITHMS) &&
                privateKeyAlgorithms.containsAll(REQUIRED_KEYBOX_ALGORITHMS) &&
                REQUIRED_KEYBOX_ALGORITHMS.all { certificateCounts.getOrDefault(it, 0) > 0 }
        }
        .getOrDefault(false)
}

private fun forceStopPackages(context: Context, packageNames: Array<String>) {
    val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
    packageNames.forEach { packageName ->
        runCatching {
                activityManager.javaClass
                    .getMethod("forceStopPackage", String::class.java)
                    .invoke(activityManager, packageName)
            }
            .onFailure { Log.e(TAG, "Failed to stop $packageName", it) }
    }
}

private const val TAG = "KeyAttestationSettings"
private val KEYBOX_MIME_TYPES = arrayOf("text/xml", "application/xml", "text/plain")
private val PIF_MIME_TYPES = arrayOf("application/json", "text/json", "text/plain")
private val REQUIRED_KEYBOX_ALGORITHMS = setOf("ecdsa", "rsa")
private val PIF_TARGET_PACKAGES = arrayOf("com.google.android.gms", "com.android.vending")
