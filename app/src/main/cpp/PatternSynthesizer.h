#pragma once

#include <vector>
#include <memory>
#include <cmath>
#include <cstdlib>
#include <string>
#include "SampleBuffer.h"
#include "TransitionManager.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

class PatternSynthesizer {
public:
    static std::shared_ptr<SampleBuffer> synthesize(
        const std::string& name,
        float bpm,
        int bars,
        SectionType type,
        int num = 4,
        int den = 4,
        int sampleRate = 48000
    ) {
        if (bars < 1) bars = 1;
        if (bpm < 20.0f) bpm = 120.0f;
        const int beatsPerBar = (num == 6 && den == 8) ? 12 : num;
        const double beatSec = (num == 6 && den == 8) ? ((60.0 / bpm) / 3.0) : ((60.0 * (4.0 / den)) / bpm);
        const int framesPerBeat = static_cast<int>(beatSec * sampleRate);
        const int totalBeats = bars * beatsPerBar;
        const int totalFrames = totalBeats * framesPerBeat;

        std::vector<float> pcm(totalFrames * 2, 0.0f);

        for (int b = 0; b < totalBeats; ++b) {
            int start = b * framesPerBeat;
            int hitFrames = std::min(sampleRate / 4, totalFrames - start);
            int beatInBar = (b % beatsPerBar);

            for (int f = 0; f < hitFrames; ++f) {
                float env = 1.0f - static_cast<float>(f) / hitFrames;
                float val = 0.0f;

                if (type == SectionType::TRANSITIONAL_FILL) {
                    float freq = 200.0f - (b * 12.0f);
                    if (freq < 80.0f) freq = 80.0f;
                    val = std::sin(2.0 * M_PI * freq * f / sampleRate) * 0.4f * env;
                    val += (static_cast<float>(rand()) / static_cast<float>(RAND_MAX) - 0.5f) * 0.2f * env;
                } else if (type == SectionType::BREAK) {
                    if (b == 0) {
                        val = std::sin(2.0 * M_PI * 60.0 * f / sampleRate) * 0.5f * env;
                        val += (static_cast<float>(rand()) / static_cast<float>(RAND_MAX) - 0.5f) * 0.3f * env;
                    }
                } else if (type == SectionType::OUTRO_ENDING && b == totalBeats - 1) {
                    float noise = (static_cast<float>(rand()) / static_cast<float>(RAND_MAX) - 0.5f) * 0.5f;
                    val = noise * env;
                } else {
                    bool isMainBeat = false;
                    bool isDownbeat = (beatInBar == 0);

                    if (num == 6 && den == 8) {
                        isMainBeat = (beatInBar == 0 || beatInBar == 3 || beatInBar == 6 || beatInBar == 9);
                    } else if (num == 7 && den == 8) {
                        isMainBeat = (beatInBar == 0 || beatInBar == 3 || beatInBar == 5);
                    } else if (num == 5 && den == 8) {
                        isMainBeat = (beatInBar == 0 || beatInBar == 3);
                    } else {
                        isMainBeat = true;
                    }

                    if (isDownbeat) {
                        val = std::sin(2.0 * M_PI * 55.0 * f / sampleRate) * 0.5f * env;
                    } else if (isMainBeat) {
                        if (beatInBar % 2 == 1 && num == 4) {
                            float noise = (static_cast<float>(rand()) / static_cast<float>(RAND_MAX) - 0.5f) * 0.35f;
                            val = noise * env;
                        } else {
                            val = std::sin(2.0 * M_PI * 75.0 * f / sampleRate) * 0.3f * env;
                        }
                    } else {
                        // Sub-beat helper dot
                        float noise = (static_cast<float>(rand()) / static_cast<float>(RAND_MAX) - 0.5f) * 0.15f;
                        val = noise * env * 0.4f;
                    }
                }
                pcm[(start + f) * 2 + 0] = val;
                pcm[(start + f) * 2 + 1] = val;
            }
        }

        auto buf = std::make_shared<SampleBuffer>();
        buf->load(std::move(pcm), sampleRate, bpm, bars, name);
        return buf;
    }
};
