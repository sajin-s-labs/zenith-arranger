package com.zenitharranger.core.midi

import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport

/**
 * Transmits real-time 24 PPQN MIDI Clock (0xF8), Start (0xFA), and Stop (0xFC) messages
 * from Zenith Arranger to connected external hardware keyboards via Android USB MIDI OUT.
 *
 * Runs on a dedicated daemon thread with THREAD_PRIORITY_AUDIO and hybrid park/spin-wait
 * timing to achieve sub-millisecond clock stability with zero cumulative tempo drift.
 */
class MidiClockTransmitter(
    private val sendByte: (Byte) -> Unit
) {
    companion object {
        const val PPQN = 24
        const val CLOCK_BYTE: Byte = 0xF8.toByte()
        const val START_BYTE: Byte = 0xFA.toByte()
        const val CONTINUE_BYTE: Byte = 0xFB.toByte()
        const val STOP_BYTE: Byte = 0xFC.toByte()
    }

    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null

    @Volatile
    var isEnabled: Boolean = false
        set(value) {
            field = value
            if (value && !isRunning.get()) {
                start()
            }
        }

    @Volatile
    var bpm: Float = 120.0f
        set(value) {
            field = value.coerceIn(30.0f, 300.0f)
        }

    @Volatile
    var isPlaying: Boolean = false

    @Volatile
    var sendClockWhenStopped: Boolean = false

    fun start() {
        if (isRunning.getAndSet(true)) return

        workerThread = Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Throwable) {}

            var nextPulseTime = System.nanoTime()

            while (isRunning.get()) {
                if (!isEnabled || (!isPlaying && !sendClockWhenStopped)) {
                    LockSupport.parkNanos(10_000_000L) // 10ms wait when idle
                    nextPulseTime = System.nanoTime()
                    continue
                }

                val currentBpm = bpm.coerceIn(30.0f, 300.0f)
                val pulseIntervalNanos = (60_000_000_000.0 / (currentBpm * PPQN.toDouble())).toLong()

                val now = System.nanoTime()
                val sleepNanos = nextPulseTime - now

                if (sleepNanos > 1_500_000L) {
                    // Park for majority of duration to keep CPU cool
                    LockSupport.parkNanos(sleepNanos - 1_000_000L)
                } else if (sleepNanos <= 0L) {
                    // Fire 24 PPQN timing clock pulse
                    sendByte(CLOCK_BYTE)

                    // Increment target timestamp without cumulative drift
                    nextPulseTime += pulseIntervalNanos

                    // Catch up if OS scheduled thread late
                    if (now - nextPulseTime > pulseIntervalNanos * 2) {
                        nextPulseTime = now + pulseIntervalNanos
                    }
                } else {
                    // Spin-wait / yield for the final fraction of a millisecond
                    Thread.yield()
                }
            }
        }, "ZenithMidiClockOutThread").apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun onTransportStart() {
        if (isEnabled) {
            sendByte(START_BYTE)
            isPlaying = true
        }
    }

    fun onTransportStop() {
        if (isEnabled) {
            sendByte(STOP_BYTE)
            isPlaying = false
        }
    }

    fun stop() {
        isRunning.set(false)
        workerThread?.interrupt()
        workerThread = null
    }
}
