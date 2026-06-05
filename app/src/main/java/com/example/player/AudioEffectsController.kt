package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log

object AudioEffectsController {
    private var activeSessionId: Int? = null
    
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    @Synchronized
    fun attachSession(context: Context, sessionId: Int) {
        Log.d("AudioEffectsController", "Attaching audio effects to session $sessionId")
        release()
        activeSessionId = sessionId
        
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Failed to init Equalizer on session $sessionId", e)
        }
        
        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Failed to init BassBoost on session $sessionId", e)
        }
        
        try {
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Failed to init Virtualizer on session $sessionId", e)
        }
        
        try {
            presetReverb = PresetReverb(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Failed to init PresetReverb on session $sessionId", e)
        }
        
        try {
            // LoudnessEnhancer takes session ID directly
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.e("AudioEffectsController", "Failed to init LoudnessEnhancer on session $sessionId", e)
        }
        
        applyAllEffects(context)
    }

    @Synchronized
    fun release() {
        try { equalizer?.release() } catch (e: Exception) {}
        try { bassBoost?.release() } catch (e: Exception) {}
        try { virtualizer?.release() } catch (e: Exception) {}
        try { presetReverb?.release() } catch (e: Exception) {}
        try { loudnessEnhancer?.release() } catch (e: Exception) {}
        
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
        activeSessionId = null
    }

    @Synchronized
    fun applyAllEffects(context: Context) {
        val prefs = context.getSharedPreferences("noctune_equalizer_prefs", Context.MODE_PRIVATE)
        val bandGainsStr = prefs.getString("band_gains", null)
        val bassBoostEnabled = prefs.getBoolean("bass_boost", true)
        val virtualizerEnabled = prefs.getBoolean("virtualizer", false)
        val reverbEnabled = prefs.getBoolean("reverb", false)
        val loudnessEnabled = prefs.getBoolean("loudness", false)

        val bandGains = if (!bandGainsStr.isNullOrEmpty()) {
            try {
                bandGainsStr.split(",").map { it.toFloat() }
            } catch (e: Exception) {
                listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            }
        } else {
            listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        }

        // Apply Equalizer
        equalizer?.let { eq ->
            try {
                eq.enabled = true
                val numBands = eq.numberOfBands
                val uiFrequencies = listOf(32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
                val bandRange = eq.bandLevelRange
                val minLevel = bandRange[0]
                val maxLevel = bandRange[1]

                for (i in 0 until numBands) {
                    val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000
                    var closestUiIndex = 0
                    var minDiff = Int.MAX_VALUE
                    for (uiIndex in uiFrequencies.indices) {
                        val diff = Math.abs(uiFrequencies[uiIndex] - centerFreqHz)
                        if (diff < minDiff) {
                            minDiff = diff
                            closestUiIndex = uiIndex
                        }
                    }
                    val uiGain = bandGains.getOrNull(closestUiIndex) ?: 0f
                    // Convert gain (-12dB to +12dB) to millibels (1dB = 100 mB)
                    val targetMillibels = (uiGain * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                    eq.setBandLevel(i.toShort(), targetMillibels.toShort())
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsController", "Error applying Equalizer band gains", e)
            }
        }

        // Apply Bass Boost
        bassBoost?.let { bb ->
            try {
                bb.enabled = bassBoostEnabled
                if (bassBoostEnabled) {
                    // Set strength between 0 and 1000 millibels
                    bb.setStrength(1000.toShort())
                } else {
                    bb.setStrength(0.toShort())
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsController", "Error applying Bass Boost", e)
            }
        }

        // Apply Virtualizer
        virtualizer?.let { vz ->
            try {
                vz.enabled = virtualizerEnabled
                if (virtualizerEnabled) {
                    vz.setStrength(1000.toShort())
                } else {
                    vz.setStrength(0.toShort())
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsController", "Error applying Virtualizer", e)
            }
        }

        // Apply Reverb
        presetReverb?.let { pr ->
            try {
                pr.enabled = reverbEnabled
                if (reverbEnabled) {
                    pr.preset = PresetReverb.PRESET_LARGEROOM
                } else {
                    pr.preset = PresetReverb.PRESET_NONE
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsController", "Error applying Reverb", e)
            }
        }

        // Apply Loudness Enhancer
        loudnessEnhancer?.let { le ->
            try {
                le.enabled = loudnessEnabled
                if (loudnessEnabled) {
                    // target gains are in millibels (positive values to boost quiet sounds)
                    le.setTargetGain(1000) // +10dB boost
                } else {
                    le.setTargetGain(0)
                }
            } catch (e: Exception) {
                Log.e("AudioEffectsController", "Error applying Loudness", e)
            }
        }
    }
}
