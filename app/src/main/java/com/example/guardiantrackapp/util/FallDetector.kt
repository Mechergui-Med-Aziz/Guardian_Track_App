package com.example.guardiantrackapp.util

import kotlin.math.sqrt

/**
 * Fall detection algorithm implementing two-phase detection:
 * Phase 1 (Free-fall): magnitude < 3 m/s² for > 100ms
 * Phase 2 (Impact): magnitude > threshold within 200ms window after free-fall
 *
 * magnitude = sqrt(ax² + ay² + az²)
 */
class FallDetector(
    private var impactThreshold: Float = 15.0f
) {
    companion object {
        private const val FREE_FALL_THRESHOLD = 3.0f // m/s²
        private const val FREE_FALL_DURATION_MS = 100L // minimum duration of free-fall
        private const val IMPACT_WINDOW_MS = 200L // window after free-fall to detect impact
    }

    private var isInFreeFall = false
    private var freeFallStartTime = 0L
    private var freeFallConfirmedTime = 0L // Time when free-fall phase was confirmed (> 100ms)
    private var waitingForImpact = false

    interface FallListener {
        fun onFallDetected(magnitude: Float)
    }

    var listener: FallListener? = null

    fun updateThreshold(threshold: Float) {
        impactThreshold = threshold
    }

    /**
     * Process a new accelerometer reading.
     * Call this from the sensor event listener.
     *
     * @param ax acceleration on X axis (m/s²)
     * @param ay acceleration on Y axis (m/s²)
     * @param az acceleration on Z axis (m/s²)
     * @param timestampMs current time in milliseconds
     */
    fun processSensorData(ax: Float, ay: Float, az: Float, timestampMs: Long) {
        val magnitude = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()

        when {
            // Phase 1: Detect free-fall
            magnitude < FREE_FALL_THRESHOLD -> {
                if (!isInFreeFall) {
                    isInFreeFall = true
                    freeFallStartTime = timestampMs
                } else if (!waitingForImpact) {
                    val freeFallDuration = timestampMs - freeFallStartTime
                    if (freeFallDuration > FREE_FALL_DURATION_MS) {
                        // Free-fall confirmed, now wait for impact
                        waitingForImpact = true
                        freeFallConfirmedTime = timestampMs
                    }
                }
            }

            // Phase 2: Detect impact after confirmed free-fall
            waitingForImpact && magnitude > impactThreshold -> {
                val timeSinceFreeFall = timestampMs - freeFallConfirmedTime
                if (timeSinceFreeFall <= IMPACT_WINDOW_MS) {
                    // FALL DETECTED!
                    listener?.onFallDetected(magnitude)
                }
                // Reset state regardless
                resetState()
            }

            // Timeout: if waiting for impact but outside window, reset
            waitingForImpact -> {
                val timeSinceFreeFall = timestampMs - freeFallConfirmedTime
                if (timeSinceFreeFall > IMPACT_WINDOW_MS) {
                    resetState()
                }
            }

            // Normal reading, not in free-fall
            else -> {
                resetState()
            }
        }
    }

    private fun resetState() {
        isInFreeFall = false
        waitingForImpact = false
        freeFallStartTime = 0L
        freeFallConfirmedTime = 0L
    }
}
