package com.zenitharranger.core.audio

/**
 * Immutable snapshot of the real-time audio arranger playback state.
 *
 * @property isPlaying True if audio stream playback is running.
 * @property currentBeat Current measure beat position (1..timeSigNum).
 * @property currentBar Cumulative bar/measure counter since playback start.
 * @property timeSigNum Time signature numerator (e.g. 4 for 4/4).
 * @property timeSigDen Time signature denominator (e.g. 4 for 4/4).
 * @property bpm Master arranger tempo in beats per minute (30.0f..300.0f).
 * @property activeSlot Currently rendering section slot ID.
 * @property queuedSlot Armed section slot ID queued for transition on the downbeat.
 */
data class ArrangerState(
    val isPlaying: Boolean,
    val currentBeat: Int,
    val currentBar: Int,
    val timeSigNum: Int,
    val timeSigDen: Int,
    val bpm: Float,
    val activeSlot: String,
    val queuedSlot: String,
    val tickIndex: Int = 0
)

/**
 * Thin JNI bridge providing thread-safe communication with the C++ Oboe AudioEngine.
 */
object NativeAudioBridge {

    init {
        System.loadLibrary("zenith_arranger_native")
    }

    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativePlay()
    private external fun nativePause()
    private external fun nativeSetBpm(bpm: Float)
    private external fun nativeSetTimeSignature(numerator: Int, denominator: Int)
    private external fun nativeTriggerSection(slotId: String)
    private external fun nativeAddOrUpdateSection(
        slotId: String,
        name: String,
        typeInt: Int,
        bars: Int,
        filePath: String,
        bpm: Float
    ): Boolean
    private external fun nativeRemoveSection(slotId: String)
    private external fun nativeClearAllSections()
    private external fun nativeGetArrangerState(stateArray: IntArray, bpmArray: FloatArray): Array<String>

    fun start(): Boolean = nativeStart()
    fun stop() = nativeStop()
    fun play() = nativePlay()
    fun pause() = nativePause()
    fun setBpm(bpm: Float) = nativeSetBpm(bpm)
    fun setTimeSignature(num: Int, den: Int) = nativeSetTimeSignature(num, den)
    fun triggerSection(slotId: String) = nativeTriggerSection(slotId)

    fun addOrUpdateSection(
        slotId: String,
        name: String,
        typeInt: Int,
        bars: Int,
        filePath: String = "",
        bpm: Float = 120f
    ): Boolean {
        return nativeAddOrUpdateSection(slotId, name, typeInt, bars, filePath, bpm)
    }

    fun removeSection(slotId: String) = nativeRemoveSection(slotId)
    fun clearAllSections() = nativeClearAllSections()

    /**
     * Polls the real-time audio state from C++.
     * Allocates isolated buffers per invocation to guarantee thread safety.
     */
    fun getArrangerState(): ArrangerState {
        val stateBuf = IntArray(6)
        val bpmBuf = FloatArray(1)
        val slots = nativeGetArrangerState(stateBuf, bpmBuf)
        return ArrangerState(
            isPlaying = stateBuf[0] == 1,
            currentBeat = stateBuf[1],
            currentBar = stateBuf[2],
            timeSigNum = if (stateBuf[3] > 0) stateBuf[3] else 4,
            timeSigDen = if (stateBuf[4] > 0) stateBuf[4] else 4,
            bpm = bpmBuf[0],
            activeSlot = slots.getOrNull(0) ?: "",
            queuedSlot = slots.getOrNull(1) ?: "",
            tickIndex = stateBuf[5]
        )
    }
}
