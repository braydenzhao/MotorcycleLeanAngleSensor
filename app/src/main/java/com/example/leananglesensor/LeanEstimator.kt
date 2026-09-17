package com.example.leananglesensor

import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

data class LeanReading(
    val leanDegrees: Float,
    val maxLeftDegrees: Float,
    val maxRightDegrees: Float,
    val calibrated: Boolean,
    val calibrating: Boolean,
    val calibrationProgress: Float
)

/**
 * Converts Android rotation-vector samples into a signed lean angle.
 *
 * Mounting assumption:
 * - phone screen faces upward
 * - phone top edge points toward the front of the motorcycle
 * - positive roll is interpreted as a right lean
 */
class LeanEstimator {
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var uprightRollDegrees = 0f
    private var filteredLeanDegrees = 0f
    private var lastTimestampNanos = 0L

    private var calibrationRequested = false
    private var calibrationStartNanos = 0L
    private var calibrationSinSum = 0.0
    private var calibrationCosSum = 0.0
    private var calibrationCount = 0

    private var trackingPeaks = false
    private var calibrated = false

    var maxLeftDegrees: Float = 0f
        private set

    var maxRightDegrees: Float = 0f
        private set

    fun beginCalibration() {
        calibrationRequested = true
        calibrated = false
        calibrationStartNanos = 0L
        calibrationSinSum = 0.0
        calibrationCosSum = 0.0
        calibrationCount = 0
        filteredLeanDegrees = 0f
        lastTimestampNanos = 0L
    }

    fun setTracking(enabled: Boolean) {
        trackingPeaks = enabled
        if (enabled) resetPeaks()
    }

    fun resetPeaks() {
        maxLeftDegrees = 0f
        maxRightDegrees = 0f
    }

    fun isCalibrated(): Boolean = calibrated

    fun process(rotationVector: FloatArray, timestampNanos: Long): LeanReading? {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)
        SensorManager.getOrientation(rotationMatrix, orientation)

        val rawRollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat()

        if (calibrationRequested) {
            if (calibrationStartNanos == 0L) {
                calibrationStartNanos = timestampNanos
            }

            val radians = Math.toRadians(rawRollDegrees.toDouble())
            calibrationSinSum += sin(radians)
            calibrationCosSum += cos(radians)
            calibrationCount++

            val elapsedNanos = timestampNanos - calibrationStartNanos
            val progress = (elapsedNanos / CALIBRATION_DURATION_NANOS.toFloat())
                .coerceIn(0f, 1f)

            if (elapsedNanos >= CALIBRATION_DURATION_NANOS && calibrationCount >= 10) {
                uprightRollDegrees = Math.toDegrees(
                    atan2(calibrationSinSum, calibrationCosSum)
                ).toFloat()
                calibrationRequested = false
                calibrated = true
                filteredLeanDegrees = 0f
                lastTimestampNanos = timestampNanos

                return reading(0f, false, 1f)
            }

            return reading(0f, true, progress)
        }

        if (!calibrated) return null

        val rawLeanDegrees = normalizeDegrees(rawRollDegrees - uprightRollDegrees)
        val deltaSeconds = if (lastTimestampNanos == 0L) {
            DEFAULT_SAMPLE_PERIOD_SECONDS
        } else {
            ((timestampNanos - lastTimestampNanos) / 1_000_000_000f)
                .coerceIn(0.001f, 0.2f)
        }
        lastTimestampNanos = timestampNanos

        // Smooth vibration without adding a large amount of visible lag.
        val alpha = (1.0 - exp(
            (-deltaSeconds / SMOOTHING_TIME_CONSTANT_SECONDS).toDouble()
        )).toFloat()
        filteredLeanDegrees += alpha * (rawLeanDegrees - filteredLeanDegrees)
        filteredLeanDegrees = filteredLeanDegrees.coerceIn(-90f, 90f)

        if (trackingPeaks) {
            if (filteredLeanDegrees < 0f) {
                maxLeftDegrees = maxOf(maxLeftDegrees, -filteredLeanDegrees)
            } else {
                maxRightDegrees = maxOf(maxRightDegrees, filteredLeanDegrees)
            }
        }

        return reading(filteredLeanDegrees, false, 1f)
    }

    private fun reading(
        leanDegrees: Float,
        calibrating: Boolean,
        progress: Float
    ): LeanReading {
        return LeanReading(
            leanDegrees = leanDegrees,
            maxLeftDegrees = maxLeftDegrees,
            maxRightDegrees = maxRightDegrees,
            calibrated = calibrated,
            calibrating = calibrating,
            calibrationProgress = progress
        )
    }

    private fun normalizeDegrees(value: Float): Float {
        var result = value
        while (result > 180f) result -= 360f
        while (result < -180f) result += 360f
        return result
    }

    companion object {
        private const val CALIBRATION_DURATION_NANOS = 1_000_000_000L
        private const val DEFAULT_SAMPLE_PERIOD_SECONDS = 1f / 50f
        private const val SMOOTHING_TIME_CONSTANT_SECONDS = 0.10f
    }
}
