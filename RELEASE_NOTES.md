# Zenith Arranger v0.1.2-alpha

We are excited to announce **Zenith Arranger v0.1.2-alpha**, a major milestone release introducing built-in native polyphonic synthesis, bi-directional hardware MIDI clock synchronization, refined piano roll controls, and a revamped style editing workflow.

---

## 🚀 Key Highlights & New Features

### 🎹 1. Native Low-Latency Polyphonic Synthesizer
* **Direct C++ Oboe Audio Pipeline:** Introduced `SynthVoice.h` and integrated native polyphonic synth voice rendering directly inside the high-priority real-time audio stream callback.
* **Instant Touch & External Play:** Zero-latency audio response when playing via either the on-screen touch keyboard or connected USB/Bluetooth MIDI keyboards.
* **Continuous Expression:** Added support for real-time Pitch Bend and Modulation depth control directly in the DSP layer.

### ⏱️ 2. Bi-Directional MIDI Clock Synchronization (Master & Slave)
* **Master Clock Transmission (`MidiClockTransmitter`):** Zenith Arranger can now act as the master MIDI clock source, sending standard 24 PPQN (Pulses Per Quarter Note) clock ticks, Start, Stop, and Continue messages to hardware synths, drum machines, and external DAWs.
* **Slave Clock Tracking (`MidiClockParser`):** Lock arranger playback to incoming external MIDI clock with jitter-dampened phase tracking and dynamic tempo correction.
* **Sync Latency Offset Compensation:** Added configurable millisecond-accurate sync compensation slider to perfectly align Android audio output buffers with external hardware gear.

### 🎼 3. Enhanced Workstation Piano Roll & Virtual Keyboard
* **Dynamic Piano Roll Bar:** Visual overview bar spanning C0 to C8 with draggable viewport navigation and quick octave jumpers.
* **Refined Keyboard Bed:** Multi-touch polyphonic response with visual key-down states, velocity-sensitive styling, and responsive key geometry across phone and tablet screens.
* **Split Zone & Channel Support:** Pre-configured keyboard routing for Upper/Lower splits and auto-accompaniment chord trigger zones.

### 🎛️ 4. Style Creator & Section Matrix Matrix Controls
* **Complete Arranger Sections:** Streamlined transitions across Intro, Main Variations (A, B, C, D), Fill-ins (AA through DD), and Endings with beat-quantized section switching.
* **Section Edit Dialogs:** Intuitive dialogs to adjust individual section bar lengths, meter, time signatures, and pattern configurations.
* **Style Transport & Preview:** Quick audition and loop controls directly within both Portrait and Landscape Style Creator views.

### ⚙️ 5. Dedicated Hardware & MIDI Settings Hub
* **Comprehensive MIDI Matrix:** Easily select input/output MIDI interfaces, configure clock modes (Internal, Master, Slave), and route individual MIDI channels.
* **Buffer & Performance Tuning:** Real-time visibility into audio buffer frames, sample rate (48 kHz), and active I/O latency.
* **Workstation UI Polish:** Sleek dark OLED-optimized theme, responsive status LCD bar, and clean landscape/portrait adaptive ergonomics.

---

## 📦 What's Changed
* Native C++ DSP: Integrated `SynthVoice` into `AudioEngine` & `JniBridge`.
* MIDI Engine: Implemented `MidiClockTransmitter`, enhanced `MidiClockParser`, `MidiCoordinator`, and `MidiDeviceManager`.
* UI Components: Revamped `SettingsView`, `WorkstationPianoRollBar`, `ArrangerSectionsMatrixRow`, `SectionEditDialogs`, and `WorkstationLayouts`.
* Build System: Updated `versionCode = 3`, `versionName = "0.1.2-alpha"`, and automated GitHub Release asset workflows.

---

## 📲 Download & Installation
Download the pre-compiled APK asset below:
* **`zenith-arranger-v0.1.2-alpha.apk`**
* Requires Android 8.0 (API level 26) or newer. Recommended: Android 12+ with low-latency audio support.
