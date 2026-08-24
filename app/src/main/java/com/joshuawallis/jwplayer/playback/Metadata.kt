package com.joshuawallis.jwplayer.playback

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object Metadata {
    fun readArtist(
        context: Context,
        uri: Uri,
    ): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty()
        } catch (e: Exception) {
            ""
        } finally {
            retriever.release()
        }
    }
}
