package com.joshuawallis.jwplayer

import com.joshuawallis.jwplayer.data.BundledSounds
import org.junit.Assert.assertEquals
import org.junit.Test

class BundledSoundsTest {
    @Test
    fun wavFileNames_excludesNonWavEntries() {
        val result = BundledSounds.wavFileNames(listOf(".DS_Store", "pink.wav", "notes.txt", "brown.mp3"))
        assertEquals(listOf("pink.wav"), result)
    }

    @Test
    fun wavFileNames_matchesExtensionCaseInsensitively() {
        assertEquals(listOf("Pink.WAV"), BundledSounds.wavFileNames(listOf("Pink.WAV")))
    }

    @Test
    fun wavFileNames_preservesSpaces() {
        assertEquals(listOf("river 01.wav"), BundledSounds.wavFileNames(listOf("river 01.wav")))
    }

    @Test
    fun wavFileNames_sortsCaseInsensitively() {
        val result = BundledSounds.wavFileNames(listOf("waves 01.wav", "Brown.wav", "alpha.wav"))
        assertEquals(listOf("alpha.wav", "Brown.wav", "waves 01.wav"), result)
    }

    @Test
    fun wavFileNames_ordersRealBundledSet() {
        val shuffled =
            listOf(
                "waves 01.wav",
                "river 03.wav",
                ".DS_Store",
                "pink.wav",
                "river 06.wav",
                "river 01.wav",
                "brown.wav",
                "river 05.wav",
                "river 02.wav",
                "river 04.wav",
            )
        val expected =
            listOf(
                "brown.wav",
                "pink.wav",
                "river 01.wav",
                "river 02.wav",
                "river 03.wav",
                "river 04.wav",
                "river 05.wav",
                "river 06.wav",
                "waves 01.wav",
            )
        assertEquals(expected, BundledSounds.wavFileNames(shuffled))
    }
}
