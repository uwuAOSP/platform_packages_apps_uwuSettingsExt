/*
 * Copyright (C) 2026 The uwuAOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package org.uwuaosp.settingsext.interfaceui

import android.content.Context
import android.graphics.fonts.FontManager
import android.net.Uri
import android.provider.OpenableColumns

internal class CustomFontController(private val context: Context) {
    private val fontManager = context.getSystemService(FontManager::class.java)
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun activeFontName(): String? {
        val postScriptName = fontManager.getCustomFontName() ?: return null
        return preferences.getString(KEY_DISPLAY_NAME, null) ?: postScriptName
    }

    fun install(uri: Uri): FontOperationResult {
        val displayName = displayName(uri)
        if (!displayName.endsWith(".ttf", ignoreCase = true)) {
            return FontOperationResult.InvalidFile
        }
        val result =
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                fontManager.installCustomFont(it)
            }
                ?: return FontOperationResult.Failed(
                    FontManager.RESULT_ERROR_FAILED_TO_OPEN_FONT_FILE
                )

        if (result != FontManager.RESULT_SUCCESS) {
            return FontOperationResult.Failed(result)
        }
        val label = displayName.dropLast(4)
        preferences.edit().putString(KEY_DISPLAY_NAME, label).apply()
        return FontOperationResult.Success(label)
    }

    fun restoreDefault(): Int {
        val result = fontManager.clearCustomFont()
        if (result == FontManager.RESULT_SUCCESS) {
            preferences.edit().remove(KEY_DISPLAY_NAME).apply()
        }
        return result
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0).orEmpty()
                }
            }
        return uri.lastPathSegment.orEmpty()
    }

    private companion object {
        const val PREFERENCES = "custom_font"
        const val KEY_DISPLAY_NAME = "display_name"
    }
}

internal sealed interface FontOperationResult {
    data class Success(val displayName: String) : FontOperationResult

    data class Failed(val errorCode: Int) : FontOperationResult

    object InvalidFile : FontOperationResult
}
