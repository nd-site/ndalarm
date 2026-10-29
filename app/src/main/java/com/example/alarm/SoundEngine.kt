package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * SoundEngine: Produces synthesized, harmonious wake-up tones and supports system alarm ringtone fallback.
 */
class SoundEngine(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var systemRingtone: Ringtone? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    fun startPlaying(ringtoneType: String, volume: Float = 1.0f) {
        stopPlaying()
        isPlaying = true

        if (ringtoneType == "system") {
            playSystemAlarm()
            return
        }

        synthJob = scope.launch {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize, sampleRate * 2)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_ALARM,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }

                audioTrack?.setVolume(volume.coerceIn(0.1f, 1.0f))
                audioTrack?.play()

                val notes = when (ringtoneType) {
                    "gentle_breeze" -> doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 783.99, 659.25) // C5, E5, G5, C6
                    "morning_ocean" -> doubleArrayOf(440.0, 554.37, 659.25, 880.0, 659.25) // A4, C#5, E5, A5
                    "synth_melody" -> doubleArrayOf(587.33, 739.99, 880.0, 1174.66, 880.0) // D5, F#5, A5, D6
                    else -> doubleArrayOf(659.25, 783.99, 987.77, 1318.51, 987.77) // E5, G5, B5, E6 (digital_pulse)
                }

                val noteDurationMs = 280
                val samplesPerNote = (sampleRate * noteDurationMs) / 1000
                val buffer = ShortArray(samplesPerNote)

                var noteIndex = 0
                while (isActive && isPlaying) {
                    val freq = notes[noteIndex % notes.size]
                    noteIndex++

                    // Generate sine wave with gentle envelope (attack & decay)
                    for (i in 0 until samplesPerNote) {
                        val t = i.toDouble() / sampleRate
                        val angle = 2.0 * Math.PI * freq * t
                        val rawSine = sin(angle)

                        // Envelope: 10% attack, 20% decay to sustain, release
                        val progress = i.toDouble() / samplesPerNote
                        val envelope = when {
                            progress < 0.1 -> progress / 0.1
                            progress < 0.8 -> 1.0 - (progress - 0.1) * 0.3
                            else -> (1.0 - progress) / 0.2 * 0.7
                        }

                        // Add subtle harmonic octave overtone for warmth
                        val harmonic = 0.25 * sin(2.0 * angle)
                        val sample = ((rawSine + harmonic) * envelope * 0.7 * Short.MAX_VALUE).toInt()
                        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    audioTrack?.write(buffer, 0, buffer.size)

                    // Small pause between motifs
                    if (noteIndex % notes.size == 0) {
                        val silence = ShortArray(sampleRate / 4)
                        audioTrack?.write(silence, 0, silence.size)
                    }
                }
            } catch (e: Exception) {
                Log.e("SoundEngine", "Synthesizer playback error, falling back to system ringtone", e)
                playSystemAlarm()
            }
        }
    }

    private fun playSystemAlarm() {
        try {
            var alertUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            systemRingtone = RingtoneManager.getRingtone(context, alertUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                systemRingtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            systemRingtone?.play()
        } catch (e: Exception) {
            Log.e("SoundEngine", "Failed to play system alarm", e)
        }
    }

    fun stopPlaying() {
        isPlaying = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null

        try {
            systemRingtone?.stop()
        } catch (ignored: Exception) {}
        systemRingtone = null
    }
}
