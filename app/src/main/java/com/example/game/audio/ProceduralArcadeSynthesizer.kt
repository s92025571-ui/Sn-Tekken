package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.game.model.FighterId
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundCue {
    MENU_CLICK,
    FIGHT_ANNOUNCE,
    LIGHT_HIT,
    HEAVY_HIT,
    BLOCK_CLANG,
    WHOOSH_DODGE,
    PROJECTILE_LAUNCH,
    PILLAR_SHATTER,
    FLOOR_CRACK,
    SMOKE_BURST,
    ULTIMATE_BUFFALO,
    ULTIMATE_RHINO,
    ULTIMATE_TIGER,
    ULTIMATE_DRAGON,
    BOSS_ROAR,
    VICTORY_FANFARE,
    DEFEAT_THUD
}

class ProceduralArcadeSynthesizer(context: Context) {

    @Volatile
    var soundEnabled: Boolean = true

    @Volatile
    var hapticsEnabled: Boolean = true

    private val appContext = context.applicationContext
    private val audioExecutor = Executors.newFixedThreadPool(3)
    private val sampleRate = 22050

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun play(cue: SoundCue) {
        if (hapticsEnabled) {
            triggerHaptic(cue)
        }
        if (!soundEnabled) return

        audioExecutor.execute {
            try {
                val samples = synthesizeSamples(cue)
                playPcmBuffer(samples)
            } catch (_: Throwable) {
                // Safe offline fallback on headless/JVM environments
            }
        }
    }

    fun playUltimateFor(fighterId: FighterId) {
        val cue = when (fighterId) {
            FighterId.IRON_BUFFALO -> SoundCue.ULTIMATE_BUFFALO
            FighterId.TITAN_RHINO -> SoundCue.ULTIMATE_RHINO
            FighterId.SHADOW_TIGER -> SoundCue.ULTIMATE_TIGER
            FighterId.INFERNO_DRAGON -> SoundCue.ULTIMATE_DRAGON
            FighterId.SHADOW_DRAGON_OVERLORD -> SoundCue.BOSS_ROAR
        }
        play(cue)
    }

    private fun triggerHaptic(cue: SoundCue) {
        val vib = vibrator ?: return
        try {
            val durationMs = when (cue) {
                SoundCue.LIGHT_HIT, SoundCue.BLOCK_CLANG -> 18L
                SoundCue.HEAVY_HIT, SoundCue.FLOOR_CRACK -> 42L
                SoundCue.PILLAR_SHATTER -> 65L
                SoundCue.ULTIMATE_BUFFALO,
                SoundCue.ULTIMATE_RHINO,
                SoundCue.ULTIMATE_TIGER,
                SoundCue.ULTIMATE_DRAGON,
                SoundCue.BOSS_ROAR -> 95L
                else -> 0L
            }
            if (durationMs <= 0L) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(durationMs)
            }
        } catch (_: Throwable) {
        }
    }

    private fun synthesizeSamples(cue: SoundCue): ShortArray {
        val durationSec = when (cue) {
            SoundCue.MENU_CLICK -> 0.07f
            SoundCue.WHOOSH_DODGE -> 0.14f
            SoundCue.LIGHT_HIT -> 0.12f
            SoundCue.BLOCK_CLANG -> 0.15f
            SoundCue.PROJECTILE_LAUNCH -> 0.22f
            SoundCue.SMOKE_BURST -> 0.28f
            SoundCue.HEAVY_HIT -> 0.25f
            SoundCue.FLOOR_CRACK -> 0.36f
            SoundCue.PILLAR_SHATTER -> 0.45f
            SoundCue.FIGHT_ANNOUNCE -> 0.42f
            SoundCue.ULTIMATE_BUFFALO,
            SoundCue.ULTIMATE_RHINO,
            SoundCue.ULTIMATE_TIGER,
            SoundCue.ULTIMATE_DRAGON,
            SoundCue.BOSS_ROAR -> 0.65f
            SoundCue.VICTORY_FANFARE -> 0.60f
            SoundCue.DEFEAT_THUD -> 0.50f
        }
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(256)
        val out = ShortArray(count)
        val rng = Random(cue.ordinal * 997 + 13)

        var phase1 = 0.0
        var phase2 = 0.0

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / count
            val sample: Double = when (cue) {
                SoundCue.MENU_CLICK -> {
                    val freq = if (progress < 0.5) 680.0 else 960.0
                    phase1 += 2.0 * PI * freq / sampleRate
                    sin(phase1) * exp(-t * 28.0) * 0.55
                }
                SoundCue.LIGHT_HIT -> {
                    val freq = 210.0 * exp(-t * 14.0) + 65.0
                    phase1 += 2.0 * PI * freq / sampleRate
                    val noise = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 26.0)
                    (sin(phase1) * 0.65 + noise * 0.55) * exp(-t * 14.0)
                }
                SoundCue.HEAVY_HIT -> {
                    val freq = 145.0 * exp(-t * 9.0) + 42.0
                    phase1 += 2.0 * PI * freq / sampleRate
                    val punch = sin(phase1) * 0.8 + sin(phase1 * 2.1) * 0.25
                    val crunch = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 16.0) * 0.5
                    (punch + crunch) * exp(-t * 8.5)
                }
                SoundCue.BLOCK_CLANG -> {
                    phase1 += 2.0 * PI * 890.0 / sampleRate
                    phase2 += 2.0 * PI * 1340.0 / sampleRate
                    (sin(phase1) * 0.5 + sin(phase2) * 0.45) * exp(-t * 16.0)
                }
                SoundCue.WHOOSH_DODGE -> {
                    val noise = rng.nextDouble() * 2.0 - 1.0
                    val env = sin(progress * PI)
                    phase1 += 2.0 * PI * (280.0 + 380.0 * progress) / sampleRate
                    (noise * 0.38 + sin(phase1) * 0.25) * env
                }
                SoundCue.PROJECTILE_LAUNCH -> {
                    val freq = 320.0 + 620.0 * (1.0 - progress)
                    phase1 += 2.0 * PI * freq / sampleRate
                    val noise = (rng.nextDouble() * 2.0 - 1.0) * 0.22
                    (sin(phase1) * 0.6 + noise) * exp(-t * 7.5)
                }
                SoundCue.PILLAR_SHATTER -> {
                    val rumbleFreq = 68.0 * exp(-t * 3.5) + 32.0
                    phase1 += 2.0 * PI * rumbleFreq / sampleRate
                    phase2 += 2.0 * PI * (720.0 - 450.0 * progress) / sampleRate
                    val debris = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 5.2)
                    (sin(phase1) * 0.62 + debris * 0.55 + sin(phase2) * 0.2 * exp(-t * 12.0)) * exp(-t * 3.2)
                }
                SoundCue.FLOOR_CRACK -> {
                    val subFreq = 58.0 + 40.0 * exp(-t * 10.0)
                    phase1 += 2.0 * PI * subFreq / sampleRate
                    val crackle = (rng.nextDouble() * 2.0 - 1.0) * exp(-t * 7.5)
                    (sin(phase1) * 0.8 + crackle * 0.45) * exp(-t * 4.5)
                }
                SoundCue.SMOKE_BURST -> {
                    val hiss = (rng.nextDouble() * 2.0 - 1.0)
                    hiss * sin(progress * PI) * 0.48
                }
                SoundCue.ULTIMATE_BUFFALO -> {
                    // Deep bellow + stampede sub-bass
                    val freq = 82.0 + 18.0 * sin(t * 24.0)
                    phase1 += 2.0 * PI * freq / sampleRate
                    val grit = (rng.nextDouble() * 2.0 - 1.0) * 0.28
                    (sin(phase1) * 0.72 + sin(phase1 * 1.5) * 0.35 + grit) * exp(-t * 2.1)
                }
                SoundCue.ULTIMATE_RHINO -> {
                    // Tectonic metallic horn roar
                    val freq = 96.0 + 110.0 * (1.0 - progress)
                    phase1 += 2.0 * PI * freq / sampleRate
                    phase2 += 2.0 * PI * (freq * 2.5) / sampleRate
                    (sin(phase1) * 0.68 + sin(phase2) * 0.32) * exp(-t * 2.3)
                }
                SoundCue.ULTIMATE_TIGER -> {
                    // Fierce Bengal snarl + rapid slash harmonics
                    val slashMod = sin(t * 58.0)
                    val freq = 260.0 + 140.0 * slashMod
                    phase1 += 2.0 * PI * freq / sampleRate
                    val roarNoise = (rng.nextDouble() * 2.0 - 1.0) * 0.36
                    (sin(phase1) * 0.55 + roarNoise) * exp(-t * 2.2)
                }
                SoundCue.ULTIMATE_DRAGON, SoundCue.BOSS_ROAR -> {
                    // Draconic inferno roar + flame jet
                    val base = if (cue == SoundCue.BOSS_ROAR) 72.0 else 115.0
                    val freq = base + 45.0 * sin(t * 17.0)
                    phase1 += 2.0 * PI * freq / sampleRate
                    val fireJet = (rng.nextDouble() * 2.0 - 1.0) * 0.48
                    (sin(phase1) * 0.62 + sin(phase1 * 2.0) * 0.25 + fireJet) * exp(-t * 1.9)
                }
                SoundCue.FIGHT_ANNOUNCE -> {
                    val freq = when {
                        progress < 0.35 -> 330.0
                        progress < 0.7 -> 440.0
                        else -> 660.0
                    }
                    phase1 += 2.0 * PI * freq / sampleRate
                    (sin(phase1) * 0.6 + sin(phase1 * 2.0) * 0.25) * (1.0 - progress * 0.4)
                }
                SoundCue.VICTORY_FANFARE -> {
                    val freq = when {
                        progress < 0.25 -> 392.0
                        progress < 0.50 -> 493.88
                        progress < 0.75 -> 587.33
                        else -> 783.99
                    }
                    phase1 += 2.0 * PI * freq / sampleRate
                    (sin(phase1) * 0.6 + sin(phase1 * 0.5) * 0.25) * (1.0 - progress * 0.25)
                }
                SoundCue.DEFEAT_THUD -> {
                    val freq = 180.0 * (1.0 - progress * 0.65)
                    phase1 += 2.0 * PI * freq / sampleRate
                    sin(phase1) * exp(-t * 3.5) * 0.75
                }
            }
            val clamped = sample.coerceIn(-0.98, 0.98)
            out[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun playPcmBuffer(samples: ShortArray) {
        val byteSize = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(byteSize.coerceAtLeast(2048))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()
        val sleepMs = (samples.size * 1000L / sampleRate) + 40L
        Thread.sleep(sleepMs)
        track.stop()
        track.release()
    }
}
