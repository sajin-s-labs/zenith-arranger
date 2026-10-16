#include <jni.h>
#include <string>
#include "AudioEngine.h"

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeStart(JNIEnv*, jobject) {
    return static_cast<jboolean>(AudioEngine::instance().start());
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeStop(JNIEnv*, jobject) {
    AudioEngine::instance().stop();
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativePlay(JNIEnv*, jobject) {
    AudioEngine::instance().play();
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativePause(JNIEnv*, jobject) {
    AudioEngine::instance().pause();
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeSetBpm(JNIEnv*, jobject, jfloat bpm) {
    AudioEngine::instance().setBpm(bpm);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeSetTimeSignature(
    JNIEnv*, jobject, jint numerator, jint denominator) {
    AudioEngine::instance().setTimeSignature(numerator, denominator);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeTriggerSection(
    JNIEnv *env, jobject, jstring jSlotId) {
    const char *slotId = env->GetStringUTFChars(jSlotId, nullptr);
    AudioEngine::instance().triggerSection(slotId);
    env->ReleaseStringUTFChars(jSlotId, slotId);
}

JNIEXPORT jboolean JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeAddOrUpdateSection(
    JNIEnv *env, jobject,
    jstring jSlotId,
    jstring jName,
    jint typeInt,
    jint bars,
    jstring jFilePath,
    jfloat bpm) {

    const char *slotId = env->GetStringUTFChars(jSlotId, nullptr);
    const char *name = jName ? env->GetStringUTFChars(jName, nullptr) : "";
    const char *filePath = jFilePath ? env->GetStringUTFChars(jFilePath, nullptr) : "";

    bool success = AudioEngine::instance().addOrUpdateSection(
        slotId, name, typeInt, bars, filePath, bpm);

    env->ReleaseStringUTFChars(jSlotId, slotId);
    if (jName) env->ReleaseStringUTFChars(jName, name);
    if (jFilePath) env->ReleaseStringUTFChars(jFilePath, filePath);
    return static_cast<jboolean>(success);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeRemoveSection(
    JNIEnv *env, jobject, jstring jSlotId) {
    const char *slotId = env->GetStringUTFChars(jSlotId, nullptr);
    AudioEngine::instance().removeSection(slotId);
    env->ReleaseStringUTFChars(jSlotId, slotId);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeClearAllSections(
    JNIEnv*, jobject) {
    AudioEngine::instance().clearAllSections();
}

JNIEXPORT jobjectArray JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeGetArrangerState(
    JNIEnv *env, jobject, jintArray jStateArray, jfloatArray jBpmArray) {

    int isPlaying = 0, currentBeat = 1, currentBar = 1, tickIndex = 0;
    int timeSigNum = 4, timeSigDen = 4;
    float bpm = 120.0f;
    std::string activeSlot, queuedSlot;

    AudioEngine::instance().getArrangerState(
        &isPlaying, &currentBeat, &currentBar, &bpm,
        &timeSigNum, &timeSigDen, activeSlot, queuedSlot, &tickIndex);

    jint stateBuf[6] = { isPlaying, currentBeat, currentBar, timeSigNum, timeSigDen, tickIndex };
    env->SetIntArrayRegion(jStateArray, 0, 6, stateBuf);

    jfloat bpmBuf[1] = { bpm };
    env->SetFloatArrayRegion(jBpmArray, 0, 1, bpmBuf);

    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray strArray = env->NewObjectArray(2, strClass, nullptr);
    env->SetObjectArrayElement(strArray, 0, env->NewStringUTF(activeSlot.c_str()));
    env->SetObjectArrayElement(strArray, 1, env->NewStringUTF(queuedSlot.c_str()));

    return strArray;
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeNoteOn(
    JNIEnv*, jobject, jint noteNumber, jfloat velocity) {
    AudioEngine::instance().noteOn(noteNumber, velocity);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeNoteOff(
    JNIEnv*, jobject, jint noteNumber) {
    AudioEngine::instance().noteOff(noteNumber);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeAllNotesOff(
    JNIEnv*, jobject) {
    AudioEngine::instance().allNotesOff();
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeSetPitchBend(
    JNIEnv*, jobject, jfloat bendSemitones) {
    AudioEngine::instance().setPitchBend(bendSemitones);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeSetModulation(
    JNIEnv*, jobject, jfloat amount) {
    AudioEngine::instance().setModulation(amount);
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeSetSyncLatencyOffset(
    JNIEnv*, jobject, jint latencyMs) {
    AudioEngine::instance().setSyncLatencyOffset(latencyMs);
}

JNIEXPORT jint JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeGetSyncLatencyOffset(
    JNIEnv*, jobject) {
    return AudioEngine::instance().getSyncLatencyOffset();
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeOnMidiClockPulse(
    JNIEnv*, jobject) {
    AudioEngine::instance().onMidiClockPulse();
}

JNIEXPORT void JNICALL
Java_com_zenitharranger_core_audio_NativeAudioBridge_nativeSetMidiClockLocked(
    JNIEnv*, jobject, jboolean locked) {
    AudioEngine::instance().setMidiClockLocked(static_cast<bool>(locked));
}

} // extern "C"
