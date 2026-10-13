#pragma once

#include <oboe/Oboe.h>
#include <atomic>
#include <memory>
#include <string>
#include <unordered_map>
#include <mutex>
#include "SampleBuffer.h"
#include "TransitionManager.h"

/**
 * AudioEngine — Real-time Arranger Engine with dynamic sections,
 * custom time signatures, and measure-accurate quantization.
 */
class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    static AudioEngine& instance();

    bool start();
    void stop();
    bool isRunning() const;

    void play();
    void pause();
    void setBpm(float bpm);

    void setTimeSignature(int numerator, int denominator);
    void getTimeSignature(int* numerator, int* denominator) const;

    void triggerSection(const std::string& slotId);
    bool addOrUpdateSection(
        const std::string& slotId,
        const std::string& name,
        int typeInt,
        int bars,
        const std::string& filePath,
        float bpm);

    void removeSection(const std::string& slotId);
    void clearAllSections();

    void getArrangerState(
        int* isPlaying,
        int* currentBeat,
        int* currentBar,
        float* currentBpm,
        int* timeSigNum,
        int* timeSigDen,
        std::string& activeSlot,
        std::string& queuedSlot,
        int* tickIndex = nullptr) const;

    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream *oboeStream,
        void *audioData,
        int32_t numFrames) override;

    void onErrorAfterClose(
        oboe::AudioStream *oboeStream,
        oboe::Result error) override;

private:
    AudioEngine();
    ~AudioEngine() = default;

    void initDefaultSlots();
    void processTiming(
        double prevPlayhead,
        double newPlayhead,
        const std::shared_ptr<SectionSlot>& slot,
        int num);

    std::shared_ptr<oboe::AudioStream> mStream;
    std::atomic<bool> mIsRunning{false};
    std::atomic<bool> mIsPlaying{false};

    std::mutex mSlotsMutex;
    std::unordered_map<std::string, std::shared_ptr<SectionSlot>> mSlots;
    TransitionManager mTransitionManager;

    std::atomic<int> mCurrentBeat{1};
    std::atomic<int> mCurrentBar{1};
    std::atomic<int> mCurrentTick{0};
    std::atomic<float> mMasterBpm{120.0f};

    std::atomic<int> mTimeSigNum{4};
    std::atomic<int> mTimeSigDen{4};

    int mSampleRate = 48000;
    double mBeatFrameAccumulator = 0.0;
};
