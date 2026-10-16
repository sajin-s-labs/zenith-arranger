#include "AudioEngine.h"
#include "WavDecoder.h"
#include "PatternSynthesizer.h"
#include <algorithm>
#include <cstring>

AudioEngine& AudioEngine::instance() {
    static AudioEngine engine;
    return engine;
}

AudioEngine::AudioEngine() {
    initDefaultSlots();
}

bool AudioEngine::start() {
    if (mIsRunning.load()) return true;

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)->setPerformanceMode(oboe::PerformanceMode::LowLatency)
           ->setSharingMode(oboe::SharingMode::Shared)->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)->setDataCallback(this)->setErrorCallback(this);

    oboe::Result result = builder.openStream(mStream);
    if (result != oboe::Result::OK) return false;

    mSampleRate = mStream->getSampleRate();
    mSynthVoice.init(mSampleRate);

    result = mStream->requestStart();
    if (result != oboe::Result::OK) {
        mStream->close();
        return false;
    }

    mIsRunning.store(true);
    return true;
}

void AudioEngine::stop() {
    if (!mIsRunning.load()) return;
    mIsPlaying.store(false);
    mIsRunning.store(false);
    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}

bool AudioEngine::isRunning() const {
    return mIsRunning.load();
}

void AudioEngine::play() {
    if (!mIsRunning.load()) start();

    if (!mTransitionManager.getActiveSlot()) {
        std::lock_guard<std::mutex> lock(mSlotsMutex);
        auto it = mSlots.find("main_1");
        if (it != mSlots.end()) {
            mTransitionManager.setActiveSlot(it->second);
        } else {
            for (const auto& p : mSlots) {
                if (p.second->type != SectionType::BREAK) {
                    mTransitionManager.setActiveSlot(p.second);
                    break;
                }
            }
        }
    }

    mCurrentBeat.store(1);
    mCurrentBar.store(1);
    mCurrentTick.store(0);
    mEmptyBreakPlayhead = 0.0;
    
    int offsetMs = mSyncLatencyOffsetMs.load(std::memory_order_relaxed);
    double offsetFrames = (offsetMs > 0 && mSampleRate > 0) ? (offsetMs * 0.001 * mSampleRate) : 0.0;
    mTransitionManager.setPlayhead(offsetFrames);
    mIsPlaying.store(true);
}

void AudioEngine::pause() { 
    mIsPlaying.store(false); 
}

void AudioEngine::setBpm(float bpm) { 
    mMasterBpm.store(std::clamp(bpm, 30.0f, 300.0f)); 
}

void AudioEngine::setTimeSignature(int numerator, int denominator) {
    mTimeSigNum.store(std::clamp(numerator, 1, 16));
    mTimeSigDen.store((denominator > 0) ? denominator : 4);
    if (mCurrentBeat.load() > mTimeSigNum.load()) mCurrentBeat.store(1);

    std::lock_guard<std::mutex> lock(mSlotsMutex);
    for (auto& pair : mSlots) {
        auto& slot = pair.second;
        if (slot && slot->type != SectionType::BREAK) {
            if (slot->filePath.empty()) {
                slot->sample = PatternSynthesizer::synthesize(
                    slot->displayName, mMasterBpm.load(), slot->bars, slot->type,
                    mTimeSigNum.load(), mTimeSigDen.load(), mSampleRate);
            }
        }
    }
}

void AudioEngine::getTimeSignature(int* num, int* den) const {
    if (num) *num = mTimeSigNum.load();
    if (den) *den = mTimeSigDen.load();
}

void AudioEngine::triggerSection(const std::string& slotId) {
    std::shared_ptr<SectionSlot> slot;
    {
        std::lock_guard<std::mutex> lock(mSlotsMutex);
        auto it = mSlots.find(slotId);
        if (it != mSlots.end()) slot = it->second;
    }
    if (!slot) return;

    bool playing = mIsPlaying.load(std::memory_order_relaxed);
    mTransitionManager.queueTransition(slot, playing);
    if (!playing) play();
}

bool AudioEngine::addOrUpdateSection(
    const std::string& slotId,
    const std::string& name,
    int typeInt,
    int bars,
    const std::string& filePath,
    float bpm) {

    auto type = static_cast<SectionType>(typeInt);
    std::shared_ptr<SampleBuffer> sample;

    if (!filePath.empty()) {
        DecodedWav wav = WavDecoder::decodeFile(filePath);
        if (wav.valid) {
            sample = std::make_shared<SampleBuffer>();
            sample->load(std::move(wav.pcmData), wav.sampleRate, bpm, bars, name);
        }
    }

    // For BREAK slots without a sample, we intentionally leave sample as nullptr for empty 1-bar break
    if (!sample && type != SectionType::BREAK) {
        sample = PatternSynthesizer::synthesize(
            name, bpm, bars, type, mTimeSigNum.load(), mTimeSigDen.load(), mSampleRate);
    }

    auto slot = std::make_shared<SectionSlot>();
    slot->id = slotId;
    slot->displayName = name;
    slot->type = type;
    slot->bars = (bars > 0) ? bars : 1;
    slot->filePath = filePath;
    slot->sample = sample;

    std::lock_guard<std::mutex> lock(mSlotsMutex);
    mSlots[slotId] = slot;

    if (!mTransitionManager.getActiveSlot()) {
        mTransitionManager.setActiveSlot(slot);
    }
    return true;
}

void AudioEngine::removeSection(const std::string& slotId) {
    std::lock_guard<std::mutex> lock(mSlotsMutex);
    mSlots.erase(slotId);
}

void AudioEngine::clearAllSections() {
    std::lock_guard<std::mutex> lock(mSlotsMutex);
    mSlots.clear();
    mTransitionManager.reset();

    // Ensure default 1-bar break slot is always available
    auto breakSlot = std::make_shared<SectionSlot>();
    breakSlot->id = "break_1";
    breakSlot->displayName = "Break";
    breakSlot->type = SectionType::BREAK;
    breakSlot->bars = 1;
    breakSlot->sample = nullptr;
    mSlots["break_1"] = breakSlot;
}

// Native Polyphonic Piano Synth API
void AudioEngine::noteOn(int midiNote, float velocity) {
    if (!mIsRunning.load()) start();
    mSynthVoice.noteOn(midiNote, velocity);
}

void AudioEngine::noteOff(int midiNote) {
    mSynthVoice.noteOff(midiNote);
}

void AudioEngine::allNotesOff() {
    mSynthVoice.allNotesOff();
}

void AudioEngine::setPitchBend(float bendFactor) {
    mSynthVoice.setPitchBend(bendFactor);
}

void AudioEngine::setModulation(float modulation) {
    mSynthVoice.setModulation(modulation);
}

void AudioEngine::setSyncLatencyOffset(int latencyMs) {
    mSyncLatencyOffsetMs.store(std::clamp(latencyMs, -100, 200), std::memory_order_relaxed);
}

int AudioEngine::getSyncLatencyOffset() const {
    return mSyncLatencyOffsetMs.load(std::memory_order_relaxed);
}

void AudioEngine::setMidiClockLocked(bool locked) {
    mMidiClockLocked.store(locked, std::memory_order_relaxed);
    if (!locked) {
        mPhaseCorrectionRatio.store(1.0, std::memory_order_relaxed);
    }
}

void AudioEngine::onMidiClockPulse() {
    int64_t count = mMidiPulseCount.fetch_add(1, std::memory_order_relaxed) + 1;
    if (!mMidiClockLocked.load(std::memory_order_relaxed) || !mIsPlaying.load(std::memory_order_relaxed)) {
        mPhaseCorrectionRatio.store(1.0, std::memory_order_relaxed);
        return;
    }

    if (count % 24 == 0) {
        auto activeSlot = mTransitionManager.getActiveSlot();
        if (activeSlot && activeSlot->sample && activeSlot->sample->isValid()) {
            double currentHead = mTransitionManager.getPlayhead();
            double sampleSr = activeSlot->sample->getSampleRate() > 0 ? activeSlot->sample->getSampleRate() : static_cast<double>(mSampleRate);
            double sampleBpm = activeSlot->sample->getBpm() > 0.0f ? activeSlot->sample->getBpm() : mMasterBpm.load();
            double sampleQuarterFrames = (60.0 / sampleBpm) * sampleSr;

            if (sampleQuarterFrames > 0.0) {
                double phaseInBeat = std::fmod(currentHead, sampleQuarterFrames);
                double halfBeat = sampleQuarterFrames * 0.5;
                double errorFrames = (phaseInBeat > halfBeat) ? (phaseInBeat - sampleQuarterFrames) : phaseInBeat;

                double thresholdFrames = 0.0005 * sampleSr; // 0.5 ms deadband
                if (std::abs(errorFrames) > thresholdFrames) {
                    double nudge = (errorFrames > 0.0) ? 0.998 : 1.002;
                    mPhaseCorrectionRatio.store(nudge, std::memory_order_relaxed);
                } else {
                    mPhaseCorrectionRatio.store(1.0, std::memory_order_relaxed);
                }
            }
        }
    }
}

void AudioEngine::getArrangerState(
    int* isPlaying,
    int* currentBeat,
    int* currentBar,
    float* currentBpm,
    int* timeSigNum,
    int* timeSigDen,
    std::string& activeSlot,
    std::string& queuedSlot,
    int* tickIndex) const {

    if (isPlaying) *isPlaying = mIsPlaying.load(std::memory_order_relaxed) ? 1 : 0;
    if (currentBeat) *currentBeat = mCurrentBeat.load(std::memory_order_relaxed);
    if (currentBar) *currentBar = mCurrentBar.load(std::memory_order_relaxed);
    if (currentBpm) *currentBpm = mMasterBpm.load(std::memory_order_relaxed);
    if (timeSigNum) *timeSigNum = mTimeSigNum.load(std::memory_order_relaxed);
    if (timeSigDen) *timeSigDen = mTimeSigDen.load(std::memory_order_relaxed);
    if (tickIndex) *tickIndex = mCurrentTick.load(std::memory_order_relaxed);
    activeSlot = mTransitionManager.getActiveSlotId();
    queuedSlot = mTransitionManager.getQueuedSlotId();
}

void AudioEngine::processTiming(
    double prevHead, double newHead,
    const std::shared_ptr<SectionSlot>& slot, int num) {
    if (!slot) return;
    
    const int den = mTimeSigDen.load(std::memory_order_relaxed);
    const float bpm = mMasterBpm.load(std::memory_order_relaxed);
    const double beatSec = 60.0 / (bpm < 30.0f ? 120.0 : bpm);

    // Duration of 1 bar in quarter notes:
    // 4/4: 4 quarter notes = 4.0 * beatSec
    // 3/4: 3 quarter notes = 3.0 * beatSec
    // 6/8: 4 compound beats (12 eighth notes) = 4.0 * beatSec
    // 7/8: 7 eighth notes = 3.5 * beatSec
    // 5/8: 5 eighth notes = 2.5 * beatSec
    const double barSec = (num == 3 && den == 4) ? 3.0 * beatSec :
                          ((num == 6 && den == 8) ? 4.0 * beatSec :
                          ((num == 7 && den == 8) ? 3.5 * beatSec :
                          ((num == 5 && den == 8) ? 2.5 * beatSec :
                          (static_cast<double>(num) * (4.0 / den) * beatSec))));
    const double sr = static_cast<double>(mSampleRate > 0 ? mSampleRate : 48000);

    bool loopWrap = false;
    bool downbeat = false;
    double barFrames = barSec * sr;

    if (slot->sample && slot->sample->isValid()) {
        const double totalFrames = static_cast<double>(slot->sample->getTotalFrames());
        double sampleSr = slot->sample->getSampleRate() > 0 ? slot->sample->getSampleRate() : sr;
        double sampleBpm = slot->sample->getBpm() > 0.0f ? slot->sample->getBpm() : bpm;
        double sampleBeatSec = 60.0 / sampleBpm;
        double sampleBarSec = (num == 3 && den == 4) ? 3.0 * sampleBeatSec :
                              ((num == 6 && den == 8) ? 4.0 * sampleBeatSec :
                              ((num == 7 && den == 8) ? 3.5 * sampleBeatSec :
                              ((num == 5 && den == 8) ? 2.5 * sampleBeatSec :
                              (static_cast<double>(num) * (4.0 / den) * sampleBeatSec))));
        double sampleBarFrames = sampleBarSec * sampleSr;
        int detectedBars = std::max(1, static_cast<int>(std::round(totalFrames / sampleBarFrames)));
        barFrames = totalFrames / detectedBars;

        loopWrap = (newHead < prevHead);
        downbeat = loopWrap || (detectedBars > 1 && static_cast<int>(newHead / barFrames) > static_cast<int>(prevHead / barFrames));
    } else {
        // Empty Break: purely time-calculated 1-bar loop
        loopWrap = (newHead >= barFrames);
        downbeat = loopWrap;
    }

    if (downbeat) mCurrentBar.fetch_add(1, std::memory_order_relaxed);

    int totalTicks = (num == 3) ? 6 : ((num == 6 && den == 8) ? 12 : ((num == 7) ? 7 : ((num == 5) ? 5 : 8)));
    const double progress = std::clamp(std::fmod(newHead, barFrames) / barFrames, 0.0, 0.9999);
    int tick = static_cast<int>(progress * totalTicks);
    mCurrentTick.store(std::clamp(tick, 0, totalTicks - 1), std::memory_order_relaxed);

    int beat = (num == 3) ? (tick / 2 + 1) : ((num == 6 && den == 8) ? (tick / 3 + 1) :
               ((num == 7) ? (tick < 3 ? 1 : (tick < 5 ? 2 : 3)) :
               ((num == 5) ? (tick < 3 ? 1 : 2) : (tick / 2 + 1))));
    mCurrentBeat.store(beat, std::memory_order_relaxed);

    if (!downbeat) return;
    if (mTransitionManager.hasQueuedTransition()) {
        mTransitionManager.triggerDownbeatSwitch(mSampleRate);
    } else if (slot->type == SectionType::TRANSITIONAL_FILL || slot->type == SectionType::BREAK) {
        if (loopWrap) mTransitionManager.returnFromFill(mSampleRate);
    } else if (slot->type == SectionType::LEAD_IN_INTRO) {
        if (loopWrap) {
            if (!mTransitionManager.getReturnTargetSlot()) {
                std::lock_guard<std::mutex> lock(mSlotsMutex);
                auto it = mSlots.find("main_1");
                if (it != mSlots.end()) mTransitionManager.setReturnTargetSlot(it->second);
            }
            mTransitionManager.returnFromFill(mSampleRate);
        }
    } else if (slot->type == SectionType::OUTRO_ENDING) {
        if (loopWrap) mIsPlaying.store(false, std::memory_order_relaxed);
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream*,
    void *audioData,
    int32_t numFrames) {

    auto *output = static_cast<float*>(audioData);
    std::memset(output, 0, numFrames * 2 * sizeof(float));

    if (mIsPlaying.load(std::memory_order_relaxed)) {
        auto activeSlot = mTransitionManager.getActiveSlot();
        if (activeSlot) {
            auto activeSample = activeSlot->sample;
            const float bpm = mMasterBpm.load(std::memory_order_relaxed);
            
            if (activeSample && activeSample->isValid()) {
                const double srRatio = static_cast<double>(activeSample->getSampleRate()) /
                                       static_cast<double>(mSampleRate > 0 ? mSampleRate : 48000);
                const double tempoRatio = static_cast<double>(bpm) /
                                          static_cast<double>(activeSample->getBpm());
                const double speedRatio = srRatio * tempoRatio * mPhaseCorrectionRatio.load(std::memory_order_relaxed);

                const double prevPlayhead = mTransitionManager.getPlayhead();
                mTransitionManager.render(output, numFrames, activeSample, speedRatio, 0.85f);
                const double newPlayhead = mTransitionManager.getPlayhead();

                processTiming(prevPlayhead, newPlayhead, activeSlot, mTimeSigNum.load(std::memory_order_relaxed));
            } else if (activeSlot->type == SectionType::BREAK) {
                // Empty Break 1-bar silence
                const double prevHead = mEmptyBreakPlayhead;
                mEmptyBreakPlayhead += numFrames;
                processTiming(prevHead, mEmptyBreakPlayhead, activeSlot, mTimeSigNum.load(std::memory_order_relaxed));
                if (mEmptyBreakPlayhead >= (60.0 * mTimeSigNum.load() / bpm) * mSampleRate) {
                    mEmptyBreakPlayhead = 0.0;
                }
            }
        }
    }

    // Always mix polyphonic piano synth (responsive, zero-latency, no clicks)
    mSynthVoice.render(output, numFrames);

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream*, oboe::Result) {
    mIsRunning.store(false);
    start();
}

void AudioEngine::initDefaultSlots() {
    struct SlotDef { const char* id; const char* name; int type; int bars; };
    for (const auto& s : { 
        SlotDef{"intro_1", "Intro 1", 2, 2}, SlotDef{"intro_2", "Intro 2", 2, 2},
        SlotDef{"intro_3", "Intro 3", 2, 2}, SlotDef{"intro_4", "Intro 4", 2, 2},
        SlotDef{"main_1", "Main 1", 0, 2}, SlotDef{"main_2", "Main 2", 0, 2},
        SlotDef{"main_3", "Main 3", 0, 2}, SlotDef{"main_4", "Main 4", 0, 2},
        SlotDef{"fill_1", "Fill 1", 1, 1}, SlotDef{"fill_2", "Fill 2", 1, 1},
        SlotDef{"fill_3", "Fill 3", 1, 1}, SlotDef{"fill_4", "Fill 4", 1, 1},
        SlotDef{"ending_1", "Ending 1", 3, 2}, SlotDef{"ending_2", "Ending 2", 3, 2},
        SlotDef{"ending_3", "Ending 3", 3, 2}, SlotDef{"ending_4", "Ending 4", 3, 2},
        SlotDef{"break_1", "Break", 4, 1}
    }) {
        addOrUpdateSection(s.id, s.name, s.type, s.bars, "", 120.0f);
    }
}
