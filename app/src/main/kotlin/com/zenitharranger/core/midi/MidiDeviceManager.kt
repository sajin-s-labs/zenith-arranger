package com.zenitharranger.core.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages physical USB and BLE MIDI device connectivity via Android's MidiManager.
 * Automatically discovers connected keyboards, opens output ports, and routes data.
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

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isDeviceConnected = MutableStateFlow(false)
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()

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
        midiManager?.registerDeviceCallback(deviceCallback, mainHandler)
        scanAndConnect()
    }

    fun stop() {
        midiManager?.unregisterDeviceCallback(deviceCallback)
        disconnectCurrent()
    }

    fun scanAndConnect() {
        if (midiManager == null) return
        val devices = midiManager.devices
        val target = devices.firstOrNull { it.outputPortCount > 0 }
        if (target != null && activeDevice?.info?.id != target.id) {
            connectToDevice(target)
        } else if (target == null && _isDeviceConnected.value) {
            disconnectCurrent()
        }
    }

    private fun connectToDevice(info: MidiDeviceInfo) {
        disconnectCurrent()
        midiManager?.openDevice(info, { device ->
            if (device != null) {
                activeDevice = device
                val port = device.openOutputPort(0)
                if (port != null) {
                    activeOutputPort = port
                    port.connect(midiReceiver)
                    val name = getDeviceDisplayName(info)
                    _connectedDeviceName.value = name
                    _isDeviceConnected.value = true
                }
            }
        }, mainHandler)
    }

    private fun disconnectCurrent() {
        try {
            activeOutputPort?.onDisconnect(midiReceiver)
            activeOutputPort?.close()
            activeDevice?.close()
        } catch (_: Exception) {}
        activeOutputPort = null
        activeDevice = null
        _connectedDeviceName.value = null
        _isDeviceConnected.value = false
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
