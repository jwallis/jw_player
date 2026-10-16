package com.joshuawallis.jwplayer

import com.joshuawallis.jwplayer.playback.isForwardStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueTest {
    @Test
    fun `moving to the next index is a forward step`() {
        assertTrue(isForwardStep(oldIndex = 2, newIndex = 3))
    }

    @Test
    fun `staying on the same index is not a forward step`() {
        assertFalse(isForwardStep(oldIndex = 2, newIndex = 2))
    }

    @Test
    fun `moving back is not a forward step`() {
        assertFalse(isForwardStep(oldIndex = 2, newIndex = 1))
    }

    @Test
    fun `jumping ahead by more than one is not a forward step`() {
        assertFalse(isForwardStep(oldIndex = 2, newIndex = 4))
    }
}
