package com.joshuawallis.jwplayer.playback

import androidx.media3.common.Player

/**
 * Media IDs tag each MediaItem with what kind of playback it belongs to, so a
 * ViewModel connecting to an already-running session can tell library tracks
 * apart from white noise. A library ID also carries the file's URI, since a
 * MediaController does not receive the item's local configuration.
 */
object MediaIds {
    const val WHITE_NOISE = "white_noise"
    private const val LIBRARY_PREFIX = "library:"

    fun forLibraryFile(uriString: String): String = LIBRARY_PREFIX + uriString

    /** The library file's URI string, or null if [mediaId] is not a library item. */
    fun libraryUriString(mediaId: String?): String? =
        if (mediaId != null && mediaId.startsWith(LIBRARY_PREFIX)) {
            mediaId.removePrefix(LIBRARY_PREFIX)
        } else {
            null
        }

    /** The playback mode implied by the player's current item and state. Idle or ended players are NONE. */
    fun modeFor(
        mediaId: String?,
        playbackState: Int,
    ): PlaybackMode {
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) return PlaybackMode.NONE
        return when {
            mediaId == WHITE_NOISE -> PlaybackMode.WHITE_NOISE
            libraryUriString(mediaId) != null -> PlaybackMode.LIBRARY
            else -> PlaybackMode.NONE
        }
    }
}
