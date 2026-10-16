#pragma once

#include <string>
#include <vector>
#include <memory>
#include <cmath>
#include <cstring>
#include <algorithm>
#include <atomic>
#include "SampleBuffer.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

enum class SectionType {
    CONTINUOUS_MAIN = 0,
    TRANSITIONAL_FILL = 1,
    LEAD_IN_INTRO = 2,
    OUTRO_ENDING = 3,
    BREAK = 4,
    CUSTOM = 5
};

struct SectionSlot {
    std::string id;
    std::string displayName;
    SectionType type = SectionType::CONTINUOUS_MAIN;
    int bars = 2;
    std::string filePath;
    std::shared_ptr<SampleBuffer> sample;
};

/**
 * TransitionManager
 * Sample-accurate musical quantization, dual-mode break, and anti-pop cosine crossfader.
 */
class TransitionManager {
public:
    TransitionManager() = default;

    void reset() {
        mQueuedSlot.store(nullptr, std::memory_order_release);
        mPendingQueuedSlot = nullptr;
        mActiveSlot = nullptr;
        mReturnTargetSlot = nullptr;
        mOutgoingSample = nullptr;
        mIncomingSample = nullptr;
        mIsCrossfading = false;
        mCrossfadeRemaining = 0;
        mCrossfadeTotal = 0;
        mPlayhead = 0.0;
        mOutgoingPlayhead = 0.0;
    }

    void setActiveSlot(const std::shared_ptr<SectionSlot>& slot) {
        mActiveSlot = slot;
        if (slot && slot->type == SectionType::CONTINUOUS_MAIN) {
            mReturnTargetSlot = slot;
        }
    }

    void setReturnTargetSlot(const std::shared_ptr<SectionSlot>& slot) {
        mReturnTargetSlot = slot;
    }

    std::shared_ptr<SectionSlot> getReturnTargetSlot() const {
        return mReturnTargetSlot;
    }

    std::shared_ptr<SectionSlot> getActiveSlot() const { return mActiveSlot; }
    
    std::string getActiveSlotId() const { 
        return mActiveSlot ? mActiveSlot->id : ""; 
    }
    
    std::string getQueuedSlotId() const { 
        SectionSlot* q = mQueuedSlot.load(std::memory_order_acquire);
        return q ? q->id : ""; 
    }

    void queueTransition(const std::shared_ptr<SectionSlot>& slot, bool isPlaying) {
        if (!isPlaying) {
            setActiveSlot(slot);
            mQueuedSlot.store(nullptr, std::memory_order_release);
            mPlayhead = 0.0;
        } else {
            mPendingQueuedSlot = slot;
            mQueuedSlot.store(slot.get(), std::memory_order_release);
        }
    }

    bool hasQueuedTransition() const {
        return mQueuedSlot.load(std::memory_order_acquire) != nullptr;
    }

    void triggerDownbeatSwitch(int sampleRate) {
        SectionSlot* queuedRaw = mQueuedSlot.exchange(nullptr, std::memory_order_acq_rel);
        if (!queuedRaw) return;

        bool isBreakWithoutSample = (queuedRaw->type == SectionType::BREAK && 
            (!queuedRaw->sample || !queuedRaw->sample->isValid()));

        if (!isBreakWithoutSample && (!queuedRaw->sample || !queuedRaw->sample->isValid())) {
            return;
        }

        mOutgoingSample = mActiveSlot ? mActiveSlot->sample : nullptr;
        mIncomingSample = queuedRaw->sample;
        mOutgoingPlayhead = mPlayhead;
        mPlayhead = 0.0;

        mCrossfadeTotal = static_cast<int>(sampleRate * 0.010); // 10ms equal-power
        if (mCrossfadeTotal < 64) mCrossfadeTotal = 64;
        mCrossfadeRemaining = mCrossfadeTotal;
        mIsCrossfading = (mOutgoingSample != nullptr);

        if (mActiveSlot && mActiveSlot->type == SectionType::CONTINUOUS_MAIN) {
            mReturnTargetSlot = mActiveSlot;
        }

        mActiveSlot = mPendingQueuedSlot;
        mPendingQueuedSlot = nullptr;
    }

    void returnFromFill(int sampleRate) {
        if (!mReturnTargetSlot) return;

        mOutgoingSample = mActiveSlot ? mActiveSlot->sample : nullptr;
        mIncomingSample = mReturnTargetSlot->sample;
        mOutgoingPlayhead = mPlayhead;
        mPlayhead = 0.0;

        mCrossfadeTotal = static_cast<int>(sampleRate * 0.010);
        if (mCrossfadeTotal < 64) mCrossfadeTotal = 64;
        mCrossfadeRemaining = mCrossfadeTotal;
        mIsCrossfading = (mOutgoingSample != nullptr);

        mActiveSlot = mReturnTargetSlot;
    }

    void render(
        float* outBuffer,
        int numFrames,
        const std::shared_ptr<SampleBuffer>& activeSample,
        double speedRatio,
        float volume) {

        if (!mIsCrossfading || !mOutgoingSample) {
            if (activeSample && activeSample->isValid()) {
                activeSample->readFramesInterpolated(outBuffer, numFrames, mPlayhead, speedRatio, volume);
            } else {
                std::memset(outBuffer, 0, numFrames * 2 * sizeof(float));
            }
            return;
        }

        for (int i = 0; i < numFrames; ++i) {
            float inL = 0.0f, inR = 0.0f;
            float outL = 0.0f, outR = 0.0f;

            if (mIncomingSample && mIncomingSample->isValid()) {
                readSingleFrame(mIncomingSample.get(), mPlayhead, speedRatio, inL, inR);
            }
            if (mOutgoingSample && mOutgoingSample->isValid()) {
                readSingleFrame(mOutgoingSample.get(), mOutgoingPlayhead, speedRatio, outL, outR);
            }

            float t = 1.0f - static_cast<float>(mCrossfadeRemaining) / static_cast<float>(mCrossfadeTotal);
            t = std::clamp(t, 0.0f, 1.0f);
            float gainIn = std::sin(t * (M_PI / 2.0));
            float gainOut = std::cos(t * (M_PI / 2.0));

            outBuffer[i * 2 + 0] = (inL * gainIn + outL * gainOut) * volume;
            outBuffer[i * 2 + 1] = (inR * gainIn + outR * gainOut) * volume;

            mCrossfadeRemaining--;
            if (mCrossfadeRemaining <= 0) {
                mIsCrossfading = false;
                mOutgoingSample = nullptr;
                mIncomingSample = nullptr;
            }
        }
    }

    double getPlayhead() const { return mPlayhead; }
    void setPlayhead(double p) { mPlayhead = p; }

private:
    static void readSingleFrame(
        const SampleBuffer* buf,
        double& playhead,
        double speedRatio,
        float& outL,
        float& outR) {

        int total = buf->getTotalFrames();
        int idx0 = static_cast<int>(playhead);
        int idx1 = (idx0 + 1 >= total) ? 0 : idx0 + 1;
        float frac = static_cast<float>(playhead - idx0);

        outL = buf->readChannelSample(idx0, idx1, frac, 0);
        outR = buf->readChannelSample(idx0, idx1, frac, 1);

        playhead += speedRatio;
        while (playhead >= total) playhead -= total;
    }

    std::shared_ptr<SectionSlot> mActiveSlot;
    std::shared_ptr<SectionSlot> mPendingQueuedSlot;
    std::atomic<SectionSlot*> mQueuedSlot{nullptr};
    std::shared_ptr<SectionSlot> mReturnTargetSlot;

    double mPlayhead = 0.0;
    double mOutgoingPlayhead = 0.0;

    bool mIsCrossfading = false;
    int mCrossfadeTotal = 480;
    int mCrossfadeRemaining = 0;
    std::shared_ptr<SampleBuffer> mOutgoingSample;
    std::shared_ptr<SampleBuffer> mIncomingSample;
};
