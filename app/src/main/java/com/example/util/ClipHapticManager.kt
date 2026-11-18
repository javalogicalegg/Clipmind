package com.example.util

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

enum class HapticProfile(val title: String, val subtitle: String) {
    FLAGSHIP_CRISP(
        title = "Crisp Tactile",
        subtitle = "Sharp mechanical responses calibrated for precision linear motors"
    ),
    SUBTLE_TICKS(
        title = "Subtle Micro-Ticks",
        subtitle = "Soft, low-amplitude detents for quiet environments"
    ),
    OFF(
        title = "Off",
        subtitle = "Disable all haptic feedback"
    )
}

class ClipHapticManager(context: Context) {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private var lastTickUptimeMs: Long = 0L
    private var lastClickUptimeMs: Long = 0L

    fun hasHardwareVibrator(): Boolean = runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)

    fun performClipSnap(profile: HapticProfile, view: View? = null) {
        if (profile == HapticProfile.OFF) return
        try {
            val now = SystemClock.uptimeMillis()
            if (now - lastClickUptimeMs < 22L) return
            lastClickUptimeMs = now

            val vib = vibrator
            if (vib == null || !vib.hasVibrator()) {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                return
            }

            val scale = if (profile == HapticProfile.FLAGSHIP_CRISP) 0.88f else 0.38f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, scale)
                    .compose()
                vibrateWithHardwareAttributes(vib, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val predefined = if (profile == HapticProfile.FLAGSHIP_CRISP) {
                    VibrationEffect.EFFECT_CLICK
                } else {
                    VibrationEffect.EFFECT_TICK
                }
                vibrateWithHardwareAttributes(vib, VibrationEffect.createPredefined(predefined))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (profile == HapticProfile.FLAGSHIP_CRISP) 180 else 80
                vib.vibrate(VibrationEffect.createOneShot(8L, amp))
            }
        } catch (_: Throwable) {
        }
    }

    fun performDetentTick(profile: HapticProfile, view: View? = null) {
        if (profile == HapticProfile.OFF) return
        try {
            val now = SystemClock.uptimeMillis()
            if (now - lastTickUptimeMs < 26L) return
            lastTickUptimeMs = now

            val vib = vibrator
            if (vib == null || !vib.hasVibrator()) {
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                return
            }

            val scale = if (profile == HapticProfile.FLAGSHIP_CRISP) 0.65f else 0.30f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK)
            ) {
                val primitive = if (profile == HapticProfile.FLAGSHIP_CRISP) {
                    VibrationEffect.Composition.PRIMITIVE_TICK
                } else {
                    VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                }
                if (vib.areAllPrimitivesSupported(primitive)) {
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(primitive, scale)
                        .compose()
                    vibrateWithHardwareAttributes(vib, effect)
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale)
                    .compose()
                vibrateWithHardwareAttributes(vib, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrateWithHardwareAttributes(
                    vib,
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(5L, 90))
            }
        } catch (_: Throwable) {
        }
    }

    fun performHeavyPurchaseActuation(profile: HapticProfile, view: View? = null) {
        if (profile == HapticProfile.OFF) return
        try {
            val vib = vibrator
            if (vib == null || !vib.hasVibrator()) {
                view?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                return
            }

            if (profile == HapticProfile.SUBTLE_TICKS) {
                performClipSnap(profile, view)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                vib.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_THUD,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.72f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 18)
                    .compose()
                vibrateWithHardwareAttributes(vib, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.65f)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 22)
                    .compose()
                vibrateWithHardwareAttributes(vib, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrateWithHardwareAttributes(
                    vib,
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(14L, 220))
            }
        } catch (_: Throwable) {
        }
    }

    fun performMilestoneBreakthrough(profile: HapticProfile, view: View? = null) {
        if (profile == HapticProfile.OFF) return
        try {
            val vib = vibrator
            if (vib == null || !vib.hasVibrator()) {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                return
            }

            val scaleMultiplier = if (profile == HapticProfile.FLAGSHIP_CRISP) 1.0f else 0.45f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(
                    VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                    VibrationEffect.Composition.PRIMITIVE_CLICK
                )
            ) {
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.75f * scaleMultiplier)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f * scaleMultiplier, 28)
                    .compose()
                vibrateWithHardwareAttributes(vib, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrateWithHardwareAttributes(
                    vib,
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 10, 30, 14), intArrayOf(0, 160, 0, 255), -1))
            }
        } catch (_: Throwable) {
        }
    }

    fun performRejectPulse(profile: HapticProfile, view: View? = null) {
        if (profile == HapticProfile.OFF) return
        try {
            val vib = vibrator
            if (vib == null || !vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
                }
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vib.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
            ) {
                val scale = if (profile == HapticProfile.FLAGSHIP_CRISP) 0.75f else 0.35f
                val effect = VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, scale, 35)
                    .compose()
                vibrateWithHardwareAttributes(vib, effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrateWithHardwareAttributes(
                    vib,
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                )
            }
        } catch (_: Throwable) {
        }
    }

    private fun vibrateWithHardwareAttributes(vib: Vibrator, effect: VibrationEffect) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
                    .build()
                vib.vibrate(effect, attrs)
            } else {
                vib.vibrate(effect)
            }
        } catch (_: Throwable) {
        }
    }
}
