#pragma once

#include <vector>
#include <string>
#include <fstream>
#include <cstdint>
#include <cstring>
#include <cmath>

struct DecodedWav {
    std::vector<float> pcmData; // Interleaved stereo float
    int sampleRate = 44100;
    int channels = 2;
    int totalFrames = 0;
    bool valid = false;
};

class WavDecoder {
public:
    static DecodedWav decodeFile(const std::string& filePath) {
        DecodedWav result;
        std::ifstream file(filePath, std::ios::binary);
        if (!file.is_open()) return result;

        char header[12];
        if (!file.read(header, 12)) return result;
        if (std::memcmp(header, "RIFF", 4) != 0 || std::memcmp(header + 8, "WAVE", 4) != 0) {
            return result;
        }

        uint16_t audioFormat = 0;
        uint16_t numChannels = 0;
        uint32_t sampleRate = 0;
        uint16_t bitsPerSample = 0;
        bool foundFmt = false;
        bool foundData = false;
        uint32_t dataSize = 0;

        while (file) {
            char chunkId[4];
            uint32_t chunkSize = 0;
            if (!file.read(chunkId, 4) || !file.read(reinterpret_cast<char*>(&chunkSize), 4)) break;

            if (std::memcmp(chunkId, "fmt ", 4) == 0 && chunkSize >= 16) {
                file.read(reinterpret_cast<char*>(&audioFormat), 2);
                file.read(reinterpret_cast<char*>(&numChannels), 2);
                file.read(reinterpret_cast<char*>(&sampleRate), 4);
                file.seekg(6, std::ios::cur); // Skip byteRate and blockAlign
                file.read(reinterpret_cast<char*>(&bitsPerSample), 2);
                if (chunkSize > 16) file.seekg(chunkSize - 16, std::ios::cur);
                foundFmt = true;
            } else if (std::memcmp(chunkId, "data", 4) == 0) {
                foundData = true;
                dataSize = chunkSize;
                break;
            } else {
                file.seekg(chunkSize, std::ios::cur);
            }
        }

        if (!foundFmt || !foundData || dataSize == 0 || numChannels == 0) return result;

        result.sampleRate = static_cast<int>(sampleRate);
        result.channels = 2; // Always normalize to stereo

        const int bytesPerSample = bitsPerSample / 8;
        if (bytesPerSample < 2 || bytesPerSample > 4) return result;

        const size_t totalSourceSamples = dataSize / bytesPerSample;
        const size_t numInputFrames = totalSourceSamples / numChannels;

        result.pcmData.resize(numInputFrames * 2, 0.0f);
        std::vector<uint8_t> rawBuffer(dataSize);
        if (!file.read(reinterpret_cast<char*>(rawBuffer.data()), dataSize)) return result;

        const uint8_t* ptr = rawBuffer.data();
        for (size_t f = 0; f < numInputFrames; ++f) {
            float left = 0.0f, right = 0.0f;
            for (int ch = 0; ch < numChannels; ++ch) {
                float sample = 0.0f;
                if (bitsPerSample == 16) {
                    int16_t val = *reinterpret_cast<const int16_t*>(ptr);
                    sample = static_cast<float>(val) / 32768.0f;
                    ptr += 2;
                } else if (bitsPerSample == 24) {
                    int32_t val = (ptr[0] << 8) | (ptr[1] << 16) | (ptr[2] << 24);
                    sample = static_cast<float>(val) / 2147483648.0f;
                    ptr += 3;
                } else if (bitsPerSample == 32 && audioFormat == 3) { // IEEE Float
                    sample = *reinterpret_cast<const float*>(ptr);
                    ptr += 4;
                } else if (bitsPerSample == 32) { // 32-bit int PCM
                    int32_t val = *reinterpret_cast<const int32_t*>(ptr);
                    sample = static_cast<float>(val) / 2147483648.0f;
                    ptr += 4;
                }

                if (ch == 0) left = sample;
                if (ch == 1) right = sample;
            }

            if (numChannels == 1) right = left; // Duplicate mono to stereo
            result.pcmData[f * 2 + 0] = left;
            result.pcmData[f * 2 + 1] = right;
        }

        result.totalFrames = static_cast<int>(numInputFrames);
        result.valid = result.totalFrames > 0;
        return result;
    }
};
