package com.joshuawallis.jwplayer.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

class SettingsRepository(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRootFolderUri(): Uri? = prefs.getString(KEY_ROOT_FOLDER_URI, null)?.toUri()

    fun setRootFolderUri(uri: Uri) {
        prefs.edit().putString(KEY_ROOT_FOLDER_URI, uri.toString()).apply()
    }

    fun getWhiteNoiseUri(): Uri? = prefs.getString(KEY_WHITE_NOISE_URI, null)?.toUri()

    fun setWhiteNoiseUri(uri: Uri) {
        prefs.edit().putString(KEY_WHITE_NOISE_URI, uri.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "mp3player_settings"
        const val KEY_ROOT_FOLDER_URI = "root_folder_uri"
        const val KEY_WHITE_NOISE_URI = "white_noise_uri"
    }
}
