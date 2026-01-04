package com.pocketcurrency.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrThrottleTest {

    @Test
    fun tryStart_blocksWhileRunningAndRespectsInterval() {
        var now = 1_000L
        val throttle = OcrThrottle(400L) { now }

        assertTrue(throttle.tryStart())
        assertFalse(throttle.tryStart())

        throttle.onComplete()

        now = 1_200L
        assertFalse(throttle.tryStart())
    }

    @Test
    fun tryStart_allowsAfterInterval() {
        var now = 2_000L
        val throttle = OcrThrottle(400L) { now }

        assertTrue(throttle.tryStart())

        throttle.onComplete()

        now = 2_400L
        assertTrue(throttle.tryStart())
    }

    @Test
    fun onAbort_allowsImmediateRetry() {
        var now = 3_000L
        val throttle = OcrThrottle(400L) { now }

        assertTrue(throttle.tryStart())

        throttle.onAbort()

        now = 3_001L
        assertTrue(throttle.tryStart())
    }
}
