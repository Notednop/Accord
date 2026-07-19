package org.akanework.gramophone.logic.utils

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.preference.PreferenceManager
import org.akanework.gramophone.logic.getBooleanStrict

object EqualizerHelper {

    private var localEqualizer: Equalizer? = null

    val PRESETS = mapOf(
        "0" to shortArrayOf(0, 0, 0, 0, 0),          // Normal
        "1" to shortArrayOf(300, 200, -200, 200, 300), // Classical
        "2" to shortArrayOf(500, 0, 200, 400, -200),  // Dance
        "3" to shortArrayOf(300, 100, 0, 100, -100),   // Folk
        "4" to shortArrayOf(400, 100, 600, 300, 0),    // Heavy Metal
        "5" to shortArrayOf(500, 300, 0, 100, 300),    // Hip Hop
        "6" to shortArrayOf(400, 200, -200, 200, 500),  // Jazz
        "7" to shortArrayOf(-200, -100, 300, 100, -200), // Pop
        "8" to shortArrayOf(500, 300, -300, 200, 500)   // Rock
    )

    fun initEqualizer(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            localEqualizer?.release()
            localEqualizer = null

            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val enabled = prefs.getBooleanStrict("eq_enabled", false)

            val eq = Equalizer(0, audioSessionId)
            localEqualizer = eq
            eq.enabled = enabled

            if (enabled) {
                applySavedPreset(context, eq)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateEqualizerState(context: Context) {
        val eq = localEqualizer ?: return
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val enabled = prefs.getBooleanStrict("eq_enabled", false)
            eq.enabled = enabled
            if (enabled) {
                applySavedPreset(context, eq)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            localEqualizer?.release()
            localEqualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applySavedPreset(context: Context, eq: Equalizer) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val presetKey = prefs.getString("eq_preset", "0") ?: "0"
            val bands = PRESETS[presetKey] ?: PRESETS["0"]!!

            val numBands = eq.numberOfBands.toInt()
            for (i in 0 until numBands) {
                if (i < bands.size) {
                    eq.setBandLevel(i.toShort(), bands[i])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
