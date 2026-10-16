package com.joshuawallis.jwplayer.data

import android.content.Context
import android.net.Uri

data class BundledSound(
    val fileName: String,
    val displayName: String,
    val uri: Uri,
)

/** Soundscape WAV files bundled in `assets/sounds/`, played through `asset://` URIs. */
object BundledSounds {
    fun list(context: Context): List<BundledSound> =
        wavFileNames(
            context.assets
                .list("sounds")
                ?.toList()
                .orEmpty(),
        ).map { fileName ->
            BundledSound(fileName, DirectoryLister.titleFromFileName(fileName), assetUri(fileName))
        }

    fun wavFileNames(rawNames: List<String>): List<String> =
        rawNames
            .filter { it.endsWith(".wav", ignoreCase = true) }
            .sortedBy { it.lowercase() }

    private fun assetUri(fileName: String): Uri =
        Uri
            .Builder()
            .scheme("asset")
            .appendPath("sounds")
            .appendPath(fileName)
            .build()
}
