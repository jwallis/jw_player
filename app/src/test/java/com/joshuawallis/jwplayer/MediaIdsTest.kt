package com.joshuawallis.jwplayer

import androidx.media3.common.Player
import com.joshuawallis.jwplayer.playback.MediaIds
import com.joshuawallis.jwplayer.playback.PlaybackMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaIdsTest {
    private val uri = "content://tree/primary%3AMusic/document/primary%3AMusic%2FSong.mp3"

    @Test
    fun `library media id round-trips the file uri`() {
        assertEquals(uri, MediaIds.libraryUriString(MediaIds.forLibraryFile(uri)))
    }

    @Test
    fun `white noise and missing ids carry no library uri`() {
        assertNull(MediaIds.libraryUriString(MediaIds.WHITE_NOISE))
        assertNull(MediaIds.libraryUriString(null))
    }

    @Test
    fun `ready library item is LIBRARY mode`() {
        assertEquals(PlaybackMode.LIBRARY, MediaIds.modeFor(MediaIds.forLibraryFile(uri), Player.STATE_READY))
    }

    @Test
    fun `buffering white noise item is WHITE_NOISE mode`() {
        assertEquals(PlaybackMode.WHITE_NOISE, MediaIds.modeFor(MediaIds.WHITE_NOISE, Player.STATE_BUFFERING))
    }

    @Test
    fun `idle player is NONE mode even with a library item`() {
        assertEquals(PlaybackMode.NONE, MediaIds.modeFor(MediaIds.forLibraryFile(uri), Player.STATE_IDLE))
    }

    @Test
    fun `untagged item is NONE mode`() {
        assertEquals(PlaybackMode.NONE, MediaIds.modeFor("", Player.STATE_READY))
        assertEquals(PlaybackMode.NONE, MediaIds.modeFor(null, Player.STATE_READY))
    }
}
