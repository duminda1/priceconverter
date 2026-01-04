package com.pocketcurrency.ocr

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class TextRecognizerSessionTest {

    @Test
    fun acquire_returnsSameInstanceUntilClosed() {
        val created = mutableListOf<TextRecognizerHelper>()
        val session = TextRecognizerSession {
            val helper = mockk<TextRecognizerHelper>(relaxed = true)
            created.add(helper)
            helper
        }

        val first = session.acquire()
        val second = session.acquire()

        assertSame(first, second)
        assertEquals(1, created.size)
    }

    @Test
    fun close_releasesRecognizerAndAllowsNewSession() {
        val created = mutableListOf<TextRecognizerHelper>()
        val session = TextRecognizerSession {
            val helper = mockk<TextRecognizerHelper>(relaxed = true)
            created.add(helper)
            helper
        }

        val first = session.acquire()

        session.close()

        verify(exactly = 1) { first.close() }

        val second = session.acquire()

        assertNotSame(first, second)
        assertEquals(2, created.size)
    }
}
