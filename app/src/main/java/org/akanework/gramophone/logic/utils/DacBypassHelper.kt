package org.akanework.gramophone.logic.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import org.akanework.gramophone.logic.getBooleanStrict

object DacBypassHelper {

    private var audioDeviceCallback: AudioDeviceCallback? = null

    fun register(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerApi34(context)
        }
    }

    fun unregister(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            unregisterApi34(context)
        }
    }

    fun shouldLockVolume(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dacBypass = prefs.getBooleanStrict("dac_bypass", false)
        val volumeMode = prefs.getString("usb_volume_mode", "0") ?: "0"
        return dacBypass && volumeMode == "1"
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun registerApi34(context: Context) {
        if (audioDeviceCallback != null) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                if (addedDevices == null) return
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                if (!prefs.getBooleanStrict("dac_bypass", false)) return

                for (device in addedDevices) {
                    if (isUsbDevice(device)) {
                        applyBitPerfectForDevice(context, audioManager, device)
                    }
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                // System automatically cleans up or we can clear
            }
        }

        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        audioDeviceCallback = callback

        // Apply immediately if USB device is already connected
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getBooleanStrict("dac_bypass", false)) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (device in devices) {
                if (isUsbDevice(device)) {
                    applyBitPerfectForDevice(context, audioManager, device)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun unregisterApi34(context: Context) {
        val callback = audioDeviceCallback ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.unregisterAudioDeviceCallback(callback)
        audioDeviceCallback = null

        // Clear preferred mixer attributes on USB devices
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (isUsbDevice(device)) {
                try {
                    val attr = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                    audioManager.clearPreferredMixerAttributes(attr, device)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun isUsbDevice(device: AudioDeviceInfo): Boolean {
        // Broaden detection to also include WIRED headset and headphones
        // because many USB-C DAC dongles report as TYPE_WIRED_HEADSET or TYPE_WIRED_HEADPHONES
        return device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun applyBitPerfectForDevice(context: Context, audioManager: AudioManager, device: AudioDeviceInfo) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val exclusiveEnabled = prefs.getBooleanStrict("usb_exclusive_access", false)
            val fixedRateStr = prefs.getString("usb_fixed_output", "0") ?: "0"
            val fixedRate = fixedRateStr.toIntOrNull() ?: 0

            val mixerAttributesList = audioManager.getSupportedMixerAttributes(device)
            var selectedAttr: AudioMixerAttributes? = null

            if (fixedRate > 0) {
                selectedAttr = mixerAttributesList.firstOrNull {
                    it.format.sampleRate == fixedRate &&
                    (!exclusiveEnabled || it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
                } ?: mixerAttributesList.firstOrNull {
                    it.format.sampleRate == fixedRate
                }
            }

            if (selectedAttr == null) {
                selectedAttr = mixerAttributesList.firstOrNull {
                    it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
                }
            }

            if (selectedAttr != null) {
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                audioManager.setPreferredMixerAttributes(attr, device, selectedAttr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
