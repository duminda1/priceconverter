package com.pocketcurrency.ocr

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class OcrThrottle(
    private val minIntervalMs: Long,
    private val timeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val running = AtomicBoolean(false)
    private val lastRunAt = AtomicLong(0L)

    fun tryStart(now: Long = timeProvider()): Boolean {
        if (running.get()) {
            return false
        }
        if (now - lastRunAt.get() < minIntervalMs) {
            return false
        }
        return running.compareAndSet(false, true)
    }

    fun onComplete(now: Long = timeProvider()) {
        lastRunAt.set(now)
        running.set(false)
    }

    fun onAbort() {
        running.set(false)
    }
}
