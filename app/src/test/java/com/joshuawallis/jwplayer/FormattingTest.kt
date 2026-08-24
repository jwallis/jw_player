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
    fun `titleFromFileName strips the file extension`() {
        assertEquals("Song", DirectoryLister.titleFromFileName("Song.mp3"))
    }
}
