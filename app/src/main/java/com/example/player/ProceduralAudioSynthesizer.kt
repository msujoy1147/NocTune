package com.example.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

class ProceduralAudioSynthesizer {
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    @Volatile private var synthThread: Thread? = null
    
    private val sampleRate = 44100
    private var currentPreset: String = "mocha_breeze"
    
    // Low-pass filter state for rain
    private var rainFilterPrev = 0.0f
    
    @Synchronized
    fun start(preset: String) {
        try {
            stop()
            currentPreset = preset
            isPlaying = true
            
            var minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize <= 0) {
                minBufferSize = 4096 // Safe default fallback
            }
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                
            audioTrack?.play()
            
            synthThread = Thread {
                renderLoop(minBufferSize)
            }.apply {
                priority = Thread.NORM_PRIORITY
                start()
            }
            Log.d("NocTuneSynth", "Procedural generator started with preset: $preset")
        } catch (e: Exception) {
            Log.e("NocTuneSynth", "Failed to start AudioTrack or synth thread", e)
            isPlaying = false
        }
    }
    
    @Synchronized
    fun stop() {
        isPlaying = false
        
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("NocTuneSynth", "Error releasing AudioTrack", e)
        }
        audioTrack = null
        
        try {
            synthThread?.join(100)
        } catch (e: Exception) {
            Log.e("NocTuneSynth", "Error joining synth thread", e)
        }
        synthThread = null
    }

    private fun renderLoop(bufferSize: Int) {
        try {
            val random = Random()
            val numSamples = bufferSize / 2 // in frames (2 bytes per sample, stereo)
            val shortBuffer = ShortArray(numSamples * 2) // Interleaved stereo channel
            
            var timeInSamples = 0L
        
        // Chord note frequencies (Hz) for presets
        // Mocha Breeze chords (lasts ~4 seconds per chord)
        val mochaChords = listOf(
            // Fmaj7: F3, A3, C4, E4
            listOf(174.61f, 220.00f, 261.63f, 329.63f),
            // Cmaj7: C3, E3, G3, B3
            listOf(130.81f, 164.81f, 196.00f, 246.94f),
            // Dm9: D3, F3, A3, C4, E4
            listOf(146.83f, 174.61f, 220.00f, 261.63f),
            // G13: G3, B3, F4, A4
            listOf(196.00f, 246.94f, 349.23f, 440.00f)
        )
        
        // Espresso Accent chords (Rhodes theme)
        val espressoChords = listOf(
            // Dm7
            listOf(146.83f, 261.63f, 293.66f, 349.23f),
            // G7alt
            listOf(196.00f, 246.94f, 311.13f, 349.23f),
            // Cmaj9
            listOf(130.81f, 246.94f, 293.66f, 329.63f),
            // A7alt
            listOf(110.00f, 220.00f, 277.18f, 311.13f)
        )

        while (isPlaying && synthThread == Thread.currentThread()) {
            val secondsPerChord = 4.0f
            val samplesPerChord = (sampleRate * secondsPerChord).toLong()
            
            for (i in 0 until numSamples) {
                val absoluteSampleIndex = timeInSamples + i
                val chordIndex = ((absoluteSampleIndex / samplesPerChord) % 4).toInt()
                
                // 1. CHORD SYNTHESIST (Jazz Pad)
                var chordLeft = 0.0f
                var chordRight = 0.0f
                
                // Compute envelope (Attack/Release to prevent clicking)
                val positionInChord = (absoluteSampleIndex % samplesPerChord).toFloat() / samplesPerChord
                val envelope = if (positionInChord < 0.2f) {
                    positionInChord / 0.2f // Attack (0.0 to 1.0)
                } else if (positionInChord > 0.8f) {
                    (1.0f - positionInChord) / 0.2f // Release (1.0 to 0.0)
                } else {
                    1.0f // Sustain
                }
                
                val currentChordFreqs = when (currentPreset) {
                    "espresso_accent" -> espressoChords[chordIndex]
                    "coffee_rain" -> listOf(110.00f, 164.81f, 220.00f) // Soft low drone
                    "caramel_latte" -> listOf(130.81f, 196.00f, 261.63f) // Calm open fifths
                    "velvet_morning" -> listOf(146.83f, 220.00f, 293.66f) // Morning drone
                    else -> mochaChords[chordIndex] // "mocha_breeze"
                }
                
                for (freq in currentChordFreqs) {
                    // Triangle or Sine wave synthesis
                    val angle = 2.0 * PI * freq * (absoluteSampleIndex.toDouble() / sampleRate)
                    var wave = sin(angle).toFloat()
                    
                    // Add slight soft saturation / warm harmonics
                    wave = (wave * 0.8f) + (sin(angle * 2.0).toFloat() * 0.1f)
                    
                    chordLeft += wave * 0.12f
                    chordRight += wave * 0.12f
                }
                
                // Filter pads based on envelope
                chordLeft *= envelope
                chordRight *= envelope
                
                // Add tremolo/modulation for "espresso_accent" (Rhodes theme)
                if (currentPreset == "espresso_accent") {
                    val tremoloAngle = 2.0 * PI * 6.0 * (absoluteSampleIndex.toDouble() / sampleRate) // 6Hz
                    val tremolo = (sin(tremoloAngle).toFloat() * 0.2f) + 0.8f
                    chordLeft *= tremolo
                    chordRight *= tremolo
                }

                // 2. RAIN FILTER (Dynamic Low Pass Noise)
                var rainNoise = random.nextFloat() * 2.0f - 1.0f // White noise
                // Low-pass filter implementation (simple RC filter)
                val filterAlpha = when (currentPreset) {
                    "coffee_rain" -> 0.08f // Heavier mud/rain noise (lower cutoff)
                    "mocha_breeze" -> 0.05f // Soft distant background chatter/rain
                    "caramel_latte" -> 0.03f // Soft ocean wind drift
                    else -> 0.00f // None
                }
                
                var filteredRain = 0.0f
                if (filterAlpha > 0.00f) {
                    rainFilterPrev = rainFilterPrev + filterAlpha * (rainNoise - rainFilterPrev)
                    filteredRain = rainFilterPrev * when (currentPreset) {
                        "coffee_rain" -> 0.22f
                        "mocha_breeze" -> 0.14f
                        else -> 0.08f
                    }
                }

                // 3. RETRO VINYL CRACKLE (Dust Pops)
                var crackle = 0.0f
                if (currentPreset == "mocha_breeze" || currentPreset == "espresso_accent" || currentPreset == "coffee_rain") {
                    val crackleChance = 0.00015 // Very rare short impulses
                    if (random.nextDouble() < crackleChance) {
                        crackle = (random.nextFloat() * 2.0f - 1.0f) * 0.45f // Sharp spike
                    } else {
                        crackle = (random.nextFloat() * 2.0f - 1.0f) * 0.005f // Low surface hiss
                    }
                }
                
                // 4. RANDOM INTEGRATION EXTRAS (Chimes & Chirps)
                var chimeLeft = 0.0f
                var chimeRight = 0.0f
                
                if (currentPreset == "caramel_latte") {
                    // Random glass chime triggered around every 5 seconds
                    val chimeTriggerChance = 1.0 / (sampleRate * 5.5)
                    if (random.nextDouble() < chimeTriggerChance) {
                        // Start a high chime sweep
                        timeOfChime = absoluteSampleIndex
                        chimeFreq = 900f + random.nextFloat() * 800f
                    }
                    if (timeOfChime > 0 && absoluteSampleIndex - timeOfChime < sampleRate) {
                        val chimeAge = (absoluteSampleIndex - timeOfChime).toFloat() / sampleRate
                        val chimeChirp = sin(2.0 * PI * chimeFreq * chimeAge).toFloat()
                        // Fast exponential decay envelope
                        val chimeEnv = Math.exp(-chimeAge.toDouble() * 9.0).toFloat()
                        chimeLeft = chimeChirp * chimeEnv * 0.15f
                        chimeRight = chimeChirp * chimeEnv * 0.12f
                    }
                } else if (currentPreset == "velvet_morning") {
                    // Synthesise bird chirps: high sweeping chirps every 3 seconds
                    val birdTriggerChance = 1.0 / (sampleRate * 3.0)
                    if (random.nextDouble() < birdTriggerChance) {
                        timeOfChime = absoluteSampleIndex
                        chimeFreq = 1800f + random.nextFloat() * 400f
                    }
                    if (timeOfChime > 0 && absoluteSampleIndex - timeOfChime < sampleRate * 0.25) {
                        val chirpAge = (absoluteSampleIndex - timeOfChime).toFloat() / (sampleRate * 0.25f)
                        // Frequency sweeps upward and downward rapidly
                        val currentBirdFreq = chimeFreq + sin(chirpAge * PI) * 500f
                        val birdAngle = 2.0 * PI * currentBirdFreq * (absoluteSampleIndex.toDouble() / sampleRate)
                        val birdChirp = sin(birdAngle).toFloat()
                        val birdEnv = sin(chirpAge * PI).toFloat() // Rounded envelope
                        chimeLeft = birdChirp * birdEnv * 0.05f
                        chimeRight = birdChirp * birdEnv * 0.04f
                    }
                }

                // Assemble stereo samples
                var outLeft = chordLeft + filteredRain + crackle + chimeLeft
                var outRight = chordRight + filteredRain + crackle + chimeRight
                
                // Apply global gain and clipper
                outLeft = outLeft.coerceIn(-1.0f, 1.0f)
                outRight = outRight.coerceIn(-1.0f, 1.0f)
                
                // Convert to PCM 16bit (-32768 to 32767)
                shortBuffer[i * 2] = (outLeft * 32767.0f).toInt().toShort()
                shortBuffer[i * 2 + 1] = (outRight * 32767.0f).toInt().toShort()
            }
            
            // Write block to AudioTrack safely inside a try-catch to prevent crash if stopped/released from another thread
            val result = try {
                audioTrack?.write(shortBuffer, 0, shortBuffer.size) ?: -1
            } catch (e: Exception) {
                Log.e("NocTuneSynth", "Error writing to AudioTrack", e)
                -1
            }
            if (result <= 0) {
                Log.e("NocTuneSynth", "AudioTrack write returned status $result. Stopping render loop.")
                isPlaying = false
                break
            }
            
            timeInSamples += numSamples
        }
        } catch (e: Exception) {
            Log.e("NocTuneSynth", "Error in renderLoop", e)
        }
    }
    
    // Variables for reactive chimes/birds sound triggers
    private var timeOfChime = 0L
    private var chimeFreq = 1000f
}
