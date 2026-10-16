package com.zenitharranger.core.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

/**
 * Manages physical USB and BLE MIDI device connectivity via Android's MidiManager.
 * Automatically discovers connected keyboards, connects bidirectional ports (Input & Output),
 * and routes data to clock parser, note dispatcher, and clock transmitter.
 */
class MidiDeviceManager(
    private val context: Context,
    private val clockParser: MidiClockParser,
    private val inputDispatcher: MidiInputDispatcher
) {
    private val midiManager: MidiManager? = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var activeDevice: MidiDevice? = null
    private var activeOutputPort: MidiOutputPort? = null
    private var activeInputPort: MidiInputPort? = null

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isDeviceConnected = MutableStateFlow(false)
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()

    private val _hasMidiOutSupport = MutableStateFlow(false)
    val hasMidiOutSupport: StateFlow<Boolean> = _hasMidiOutSupport.asStateFlow()

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo?) {
            scanAndConnect()
        }
        override fun onDeviceRemoved(device: MidiDeviceInfo?) {
            if (activeDevice?.info?.id == device?.id) {
                disconnectCurrent()
            }
        }
    }

    private val midiReceiver = object : MidiReceiver() {
        override fun onSend(msg: ByteArray?, offset: Int, count: Int, timestamp: Long) {
            if (msg == null || count <= 0) return
            for (i in offset until (offset + count)) {
                val b = msg[i]
                if (b == MidiClockParser.CLOCK_BYTE || b == MidiClockParser.START_BYTE ||
                    b == MidiClockParser.CONTINUE_BYTE || b == MidiClockParser.STOP_BYTE) {
                    clockParser.onByteReceived(b)
                }
            }
            inputDispatcher.processMidiBytes(msg, offset, count)
        }
    }

    fun start() {
        try {
            midiManager?.registerDeviceCallback(deviceCallback, mainHandler)
            scanAndConnect()
        } catch (_: Throwable) {}
    }

    fun stop() {
        try {
            midiManager?.unregisterDeviceCallback(deviceCallback)
            disconnectCurrent()
        } catch (_: Throwable) {}
    }

    fun scanAndConnect() {
        try {
            if (midiManager == null) return
            val devices = midiManager.devices ?: return
            val target = devices.firstOrNull { it.outputPortCount > 0 || it.inputPortCount > 0 }
            if (target != null && activeDevice?.info?.id != target.id) {
                connectToDevice(target)
            } else if (target == null && _isDeviceConnected.value) {
                disconnectCurrent()
            }
        } catch (_: Throwable) {}
    }

    private fun connectToDevice(info: MidiDeviceInfo) {
        disconnectCurrent()
        midiManager?.openDevice(info, { device ->
            if (device != null) {
                activeDevice = device

                // 1. Open device output port (data from keyboard into phone)
                if (info.outputPortCount > 0) {
                    val outPort = device.openOutputPort(0)
                    if (outPort != null) {
                        activeOutputPort = outPort
                        outPort.connect(midiReceiver)
                    }
                }

                // 2. Open device input port (data from phone to keyboard)
                if (info.inputPortCount > 0) {
                    try {
                        activeInputPort = device.openInputPort(0)
                        _hasMidiOutSupport.value = activeInputPort != null
                    } catch (_: Throwable) {
                        activeInputPort = null
                        _hasMidiOutSupport.value = false
                    }
                }

                val name = getDeviceDisplayName(info)
                _connectedDeviceName.value = name
                _isDeviceConnected.value = (activeOutputPort != null || activeInputPort != null)
            }
        }, mainHandler)
    }

    fun sendMidiByte(byte: Byte) {
        val port = activeInputPort ?: return
        try {
            port.send(byteArrayOf(byte), 0, 1)
        } catch (_: IOException) {}
    }

    fun sendMidi(bytes: ByteArray, offset: Int = 0, count: Int = bytes.size) {
        val port = activeInputPort ?: return
        try {
            port.send(bytes, offset, count)
        } catch (_: IOException) {}
    }

    private fun disconnectCurrent() {
        try {
            activeOutputPort?.onDisconnect(midiReceiver)
            activeOutputPort?.close()
        } catch (_: Exception) {}
        try {
            activeInputPort?.close()
        } catch (_: Exception) {}
        try {
            activeDevice?.close()
        } catch (_: Exception) {}

        activeOutputPort = null
        activeInputPort = null
        activeDevice = null
        _connectedDeviceName.value = null
        _isDeviceConnected.value = false
        _hasMidiOutSupport.value = false
        clockParser.resetClock()
        inputDispatcher.clearNotes()
    }

    private fun getDeviceDisplayName(info: MidiDeviceInfo): String {
        val props = info.properties
        val name = props.getString(MidiDeviceInfo.PROPERTY_NAME)
        val product = props.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
        val manufacturer = props.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER)
        return name ?: product ?: manufacturer ?: "USB MIDI Keyboard"
    }
}
