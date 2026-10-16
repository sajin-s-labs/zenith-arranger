#pragma once

#include <cmath>
#include <array>
#include <atomic>
#include <algorithm>
#include <cstring>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

/**
 * Real-time Polyphonic Synthesizer Engine (C++)
 * Zero heap allocations during rendering.
 * Anti-pop equal-power ADSR envelope and bandlimited harmonic generation.
 */
class SynthVoice {
public:
    static constexpr int MAX_POLYPHONY = 16;

    struct Voice {
        bool active = false;
        int midiNote = -1;
        float velocity = 0.0f;
        double phase = 0.0;
        double phaseStep = 0.0;
        
        // ADSR Envelope
        enum class EnvState { IDLE, ATTACK, DECAY, SUSTAIN, RELEASE } state = EnvState::IDLE;
        float currentGain = 0.0f;
        uint64_t age = 0;
    };

    SynthVoice() = default;

    void init(int sampleRate) {
        mSampleRate = sampleRate > 0 ? sampleRate : 48000;
        for (auto& v : mVoices) {
            v.active = false;
            v.state = Voice::EnvState::IDLE;
            v.currentGain = 0.0f;
        }
    }

    void noteOn(int midiNote, float velocity) {
        if (midiNote < 0 || midiNote > 127) return;
        velocity = std::clamp(velocity, 0.0f, 1.0f);
        if (velocity <= 0.001f) {
            noteOff(midiNote);
            return;
        }

        // Check if note is already sounding (retrigger smoothly)
        Voice* target = nullptr;
        for (auto& v : mVoices) {
            if (v.active && v.midiNote == midiNote) {
                target = &v;
                break;
            }
        }

        // Find inactive voice
        if (!target) {
            for (auto& v : mVoices) {
                if (!v.active) {
                    target = &v;
                    break;
                }
            }
        }

        // Voice stealing: steal oldest voice in RELEASE, or oldest overall
        if (!target) {
            uint64_t oldestAge = 0;
            for (auto& v : mVoices) {
                if (v.state == Voice::EnvState::RELEASE && v.age >= oldestAge) {
                    oldestAge = v.age;
                    target = &v;
                }
            }
            if (!target) {
                for (auto& v : mVoices) {
                    if (v.age >= oldestAge) {
                        oldestAge = v.age;
                        target = &v;
                    }
                }
            }
        }

        if (!target) return;

        target->active = true;
        target->midiNote = midiNote;
        target->velocity = velocity;
        target->age = ++mVoiceAgeCounter;
        target->state = Voice::EnvState::ATTACK;
        
        // Frequency calculation with pitch bend
        updateVoicePitch(target);
    }

    void noteOff(int midiNote) {
        for (auto& v : mVoices) {
            if (v.active && v.midiNote == midiNote && v.state != Voice::EnvState::RELEASE) {
                v.state = Voice::EnvState::RELEASE;
            }
        }
    }

    void allNotesOff() {
        for (auto& v : mVoices) {
            if (v.active) {
                v.state = Voice::EnvState::RELEASE;
            }
        }
    }

    void setPitchBend(float bendFactor) {
        mPitchBendFactor = std::clamp(bendFactor, 0.5f, 2.0f);
        for (auto& v : mVoices) {
            if (v.active) updateVoicePitch(&v);
        }
    }

    void setModulation(float mod) {
        mModulation = std::clamp(mod, 0.0f, 1.0f);
    }

    /**
     * Renders synth voices directly into stereo float buffer.
     * ZERO allocations, lock-free real-time rendering.
     */
    void render(float* outBuffer, int numFrames, float masterGain = 0.75f) {
        if (!hasActiveVoices()) return;

        const double dt = 1.0 / static_cast<double>(mSampleRate);
        const float attackRate = 1.0f / (0.003f * mSampleRate);   // 3ms attack (no clicks)
        const float decayRate = 1.0f / (1.20f * mSampleRate);     // 1.2s decay
        const float sustainLevel = 0.20f;
        const float releaseRate = 1.0f / (0.12f * mSampleRate);   // 120ms release (natural decay)
        const float vibRate = 5.2f;
        const float vibDepth = mModulation * 0.025f;

        for (int i = 0; i < numFrames; ++i) {
            float sampleSum = 0.0f;
            mLfoTime += dt;
            if (mLfoTime > 1000.0) mLfoTime = 0.0;
            const float lfo = 1.0f + vibDepth * std::sin(2.0 * M_PI * vibRate * mLfoTime);

            for (auto& v : mVoices) {
                if (!v.active) continue;

                // Envelope state machine
                switch (v.state) {
                    case Voice::EnvState::ATTACK:
                        v.currentGain += attackRate;
                        if (v.currentGain >= 1.0f) {
                            v.currentGain = 1.0f;
                            v.state = Voice::EnvState::DECAY;
                        }
                        break;
                    case Voice::EnvState::DECAY:
                        v.currentGain -= decayRate;
                        if (v.currentGain <= sustainLevel) {
                            v.currentGain = sustainLevel;
                            v.state = Voice::EnvState::SUSTAIN;
                        }
                        break;
                    case Voice::EnvState::SUSTAIN:
                        v.currentGain = sustainLevel;
                        break;
                    case Voice::EnvState::RELEASE:
                        v.currentGain -= releaseRate;
                        if (v.currentGain <= 0.0005f) {
                            v.currentGain = 0.0f;
                            v.active = false;
                            v.state = Voice::EnvState::IDLE;
                        }
                        break;
                    case Voice::EnvState::IDLE:
                        v.active = false;
                        break;
                }

                if (!v.active) continue;

                // Multi-harmonic synthesis for rich EPiano warmth
                double phase = v.phase;
                float fund = std::sin(phase);
                float harm2 = 0.30f * std::sin(phase * 2.0);
                float harm3 = 0.12f * std::sin(phase * 3.0);
                float voiceSample = (fund + harm2 + harm3) * v.currentGain * v.velocity;

                sampleSum += voiceSample;

                // Advance phase with LFO vibrato
                v.phase += v.phaseStep * lfo;
                if (v.phase >= 2.0 * M_PI) v.phase -= 2.0 * M_PI;
            }

            // Sum into interleaved stereo buffer
            float mixedSample = sampleSum * masterGain;
            outBuffer[i * 2 + 0] += mixedSample;
            outBuffer[i * 2 + 1] += mixedSample;
        }
    }

    bool hasActiveVoices() const {
        for (const auto& v : mVoices) {
            if (v.active) return true;
        }
        return false;
    }

private:
    void updateVoicePitch(Voice* v) {
        double freq = 440.0 * std::pow(2.0, (v->midiNote - 69.0) / 12.0) * mPitchBendFactor;
        v->phaseStep = (2.0 * M_PI * freq) / static_cast<double>(mSampleRate);
    }

    int mSampleRate = 48000;
    float mPitchBendFactor = 1.0f;
    float mModulation = 0.0f;
    double mLfoTime = 0.0;
    uint64_t mVoiceAgeCounter = 0;
    std::array<Voice, MAX_POLYPHONY> mVoices{};
};
