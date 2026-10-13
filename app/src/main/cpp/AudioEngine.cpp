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
    mCurrentBeat.store(1);
    mCurrentBar.store(1);
    mCurrentTick.store(0);
    mTransitionManager.setPlayhead(0.0);
    mIsPlaying.store(true);
}

void AudioEngine::pause() { mIsPlaying.store(false); }
void AudioEngine::setBpm(float bpm) { mMasterBpm.store(std::clamp(bpm, 30.0f, 300.0f)); }

void AudioEngine::setTimeSignature(int numerator, int denominator) {
    mTimeSigNum.store(std::clamp(numerator, 1, 16));
    mTimeSigDen.store((denominator > 0) ? denominator : 4);
    if (mCurrentBeat.load() > mTimeSigNum.load()) mCurrentBeat.store(1);
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

    if (!sample) {
        sample = PatternSynthesizer::synthesize(
            name, bpm, bars, type, mTimeSigNum.load(), mTimeSigDen.load(), mSampleRate);
    }

    auto slot = std::make_shared<SectionSlot>();
    slot->id = slotId;
    slot->displayName = name;
    slot->type = type;
    slot->bars = bars;
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
    if (!slot || !slot->sample || !slot->sample->isValid()) return;
    const int den = mTimeSigDen.load(std::memory_order_relaxed);
    float bpm = mMasterBpm.load(std::memory_order_relaxed);
    double beatSec = 60.0 / (bpm < 30.0f ? 120.0 : bpm);
    double barSec = (num == 3 && den == 4) ? 3.0 * beatSec :
                    ((num == 6 && den == 8) ? 4.0 * beatSec :
                    ((num == 7 && den == 8) ? 3.5 * beatSec :
                    ((num == 5 && den == 8) ? 2.5 * beatSec : 4.0 * beatSec)));
    double sr = slot->sample->getSampleRate() > 0 ? slot->sample->getSampleRate() : static_cast<double>(mSampleRate);
    const double totalFrames = static_cast<double>(slot->sample->getTotalFrames());
    int bars = std::max(1, static_cast<int>(std::round(totalFrames / (barSec * sr))));
    const double barFrames = totalFrames / bars;

    bool loopWrap = (newHead < prevHead);
    bool downbeat = loopWrap || (bars > 1 && static_cast<int>(newHead / barFrames) > static_cast<int>(prevHead / barFrames));
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
    } else if (slot->type == SectionType::OUTRO_ENDING) {
        if (loopWrap) mIsPlaying.store(false, std::memory_order_relaxed);
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream*,
    void *audioData,
    int32_t numFrames) {

    auto *output = static_cast<float*>(audioData);
    if (!mIsPlaying.load(std::memory_order_relaxed)) {
        std::memset(output, 0, numFrames * 2 * sizeof(float));
        return oboe::DataCallbackResult::Continue;
    }

    auto activeSlot = mTransitionManager.getActiveSlot();
    auto activeSample = activeSlot ? activeSlot->sample : nullptr;
    if (!activeSample || !activeSample->isValid()) {
        std::memset(output, 0, numFrames * 2 * sizeof(float));
        return oboe::DataCallbackResult::Continue;
    }

    const float bpm = mMasterBpm.load(std::memory_order_relaxed);
    const double srRatio = static_cast<double>(activeSample->getSampleRate()) /
                           static_cast<double>(mSampleRate > 0 ? mSampleRate : 48000);
    const double tempoRatio = static_cast<double>(bpm) /
                              static_cast<double>(activeSample->getBpm());
    const double speedRatio = srRatio * tempoRatio;

    const double prevPlayhead = mTransitionManager.getPlayhead();
    mTransitionManager.render(output, numFrames, activeSample, speedRatio, 0.85f);
    const double newPlayhead = mTransitionManager.getPlayhead();

    processTiming(prevPlayhead, newPlayhead, activeSlot, mTimeSigNum.load(std::memory_order_relaxed));
    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream*, oboe::Result) {
    mIsRunning.store(false);
    start();
}

void AudioEngine::initDefaultSlots() {
    struct SlotDef { const char* id; const char* name; int type; int bars; };
    for (const auto& s : { SlotDef{"intro_1", "Intro 1", 2, 2}, SlotDef{"main_a", "Main A", 0, 2},
                           SlotDef{"fill_a", "Fill A", 1, 1}, SlotDef{"main_b", "Main B", 0, 2},
                           SlotDef{"fill_b", "Fill B", 1, 1}, SlotDef{"ending_1", "Ending 1", 3, 2},
                           SlotDef{"break_1", "Break", 4, 1} }) {
        addOrUpdateSection(s.id, s.name, s.type, s.bars, "", 120.0f);
    }
}
