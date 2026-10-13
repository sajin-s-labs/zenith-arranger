#pragma once

#include <vector>
#include <string>
#include <algorithm>
#include <cstring>
#include <cmath>

/**
 * SampleBuffer
 * Pre-decoded stereo float PCM audio in RAM.
 *
 * REAL-TIME THREAD SAFETY:
 * - readFramesInterpolated() performs ZERO heap allocations and ZERO locks.
 */
class SampleBuffer {
public:
    SampleBuffer() = default;

    void load(std::vector<float>&& pcmData, int sampleRate, float bpm, int bars, const std::string& name = "") {
        mData = std::move(pcmData);
        mTotalFrames = static_cast<int>(mData.size() / 2);
        mSampleRate = sampleRate > 0 ? sampleRate : 44100;
        mBpm = bpm > 20.0f ? bpm : 120.0f;
        mBars = bars > 0 ? bars : 4;
        mName = name;
        applyLoopSeamCrossfade();
    }

    bool isValid() const {
        return mTotalFrames > 0 && !mData.empty();
    }

    int getTotalFrames() const { return mTotalFrames; }
    int getSampleRate() const { return mSampleRate; }
    float getBpm() const { return mBpm; }
    int getBars() const { return mBars; }
    float readChannelSample(int idx0, int idx1, float frac, int ch) const {
        if (mData.empty()) return 0.0f;
        float s0 = mData[idx0 * 2 + ch];
        float s1 = mData[idx1 * 2 + ch];
        return s0 + (s1 - s0) * frac;
    }

    /**
     * Reads frames with linear interpolation and sample-rate / tempo matching.
     * ZERO allocations, zero locks.
     */
    void readFramesInterpolated(
        float* outBuffer,
        int numFrames,
        double& playhead,
        double speedRatio,
        float volume
    ) const {
        if (!isValid()) {
            std::memset(outBuffer, 0, numFrames * 2 * sizeof(float));
            return;
        }

        const int totalFrames = mTotalFrames;
        const float* data = mData.data();

        for (int i = 0; i < numFrames; ++i) {
            int index0 = static_cast<int>(playhead);
            int index1 = index0 + 1;
            if (index1 >= totalFrames) index1 = 0;

            float frac = static_cast<float>(playhead - index0);

            // Left Channel
            float l0 = data[index0 * 2 + 0];
            float l1 = data[index1 * 2 + 0];
            outBuffer[i * 2 + 0] = (l0 + (l1 - l0) * frac) * volume;

            // Right Channel
            float r0 = data[index0 * 2 + 1];
            float r1 = data[index1 * 2 + 1];
            outBuffer[i * 2 + 1] = (r0 + (r1 - r0) * frac) * volume;

            playhead += speedRatio;
            while (playhead >= totalFrames) {
                playhead -= totalFrames;
            }
        }
    }

private:
    void applyLoopSeamCrossfade() {
        if (mTotalFrames < 256) return;
        const int xfadeLen = std::min(64, mTotalFrames / 16);
        for (int i = 0; i < xfadeLen; ++i) {
            float gain = static_cast<float>(i) / static_cast<float>(xfadeLen);
            for (int ch = 0; ch < 2; ++ch) {
                float tail = mData[(mTotalFrames - xfadeLen + i) * 2 + ch];
                float head = mData[i * 2 + ch];
                mData[i * 2 + ch] = head * gain + tail * (1.0f - gain);
            }
        }
    }

    std::vector<float> mData;
    int mTotalFrames = 0;
    int mSampleRate = 44100;
    float mBpm = 120.0f;
    int mBars = 4;
    std::string mName;
};
