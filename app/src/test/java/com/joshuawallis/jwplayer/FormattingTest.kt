package com.joshuawallis.jwplayer

import com.joshuawallis.jwplayer.data.DirectoryLister
import com.joshuawallis.jwplayer.ui.screens.main.formatTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {
    @Test
    fun `formatTime renders zero as 00 colon 00`() {
        assertEquals("00:00", formatTime(0))
    }

    @Test
    fun `formatTime pads minutes and seconds to two digits`() {
        assertEquals("02:05", formatTime(125_000))
    }

    @Test
    fun `formatTime includes hours once an hour has elapsed`() {
        assertEquals("1:00:05", formatTime(3_605_000))
    }

    @Test
    fun `formatTime shows MM colon SS for a time under a minute`() {
        assertEquals("00:42", formatTime(42_000))
    }

    @Test
    fun `formatTime stays MM colon SS at 59 minutes 59 seconds`() {
        assertEquals("59:59", formatTime(3_599_000))
    }

    @Test
    fun `formatTime switches to H colon MM colon SS at exactly one hour`() {
        assertEquals("1:00:00", formatTime(3_600_000))
    }

    @Test
    fun `formatTime shows H colon MM colon SS past one hour`() {
        assertEquals("1:05:09", formatTime(3_909_000))
        assertEquals("10:00:00", formatTime(36_000_000))
    }

    @Test
    fun `formatTime uses hours early in a track that is an hour or longer`() {
        assertEquals("0:02:05", formatTime(positionMs = 125_000, durationMs = 4_000_000))
    }

    @Test
    fun `formatTime stays MM colon SS for a track just under an hour`() {
        assertEquals("02:05", formatTime(positionMs = 125_000, durationMs = 3_599_000))
    }

    @Test
    fun `formatTime formats a total duration under an hour as MM colon SS`() {
        assertEquals("59:59", formatTime(positionMs = 3_599_000, durationMs = 3_599_000))
    }

    @Test
    fun `formatTime treats a negative position as zero`() {
        assertEquals("00:00", formatTime(-5_000))
    }

    @Test
    fun `titleFromFileName strips the file extension`() {
        assertEquals("Song", DirectoryLister.titleFromFileName("Song.mp3"))
    }
}
