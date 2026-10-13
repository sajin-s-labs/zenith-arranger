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
        const double beatSec = (60.0 * (4.0 / den)) / bpm;
        const int framesPerBeat = static_cast<int>(beatSec * sampleRate);
        const int totalBeats = bars * num;
        const int totalFrames = totalBeats * framesPerBeat;

        std::vector<float> pcm(totalFrames * 2, 0.0f);

        for (int b = 0; b < totalBeats; ++b) {
            int start = b * framesPerBeat;
            int hitFrames = std::min(sampleRate / 4, totalFrames - start);
            int beatInBar = (b % num) + 1;

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
                    if (beatInBar == 1) {
                        val = std::sin(2.0 * M_PI * 55.0 * f / sampleRate) * 0.5f * env;
                    } else if (beatInBar % 2 == 0) {
                        float noise = (static_cast<float>(rand()) / static_cast<float>(RAND_MAX) - 0.5f) * 0.35f;
                        val = noise * env;
                    } else {
                        val = std::sin(2.0 * M_PI * 75.0 * f / sampleRate) * 0.3f * env;
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
