package io.fastpix.data.domain.calculator

import io.fastpix.data.domain.state.SDKStateService
import io.fastpix.data.domain.wallclock.ViewWatchCounter
import io.fastpix.data.utils.Logger
import io.fastpix.data.di.DependencyContainer

class EventDataCalculator {

    companion object {
        private const val TAG = "EventDataCalculator"
    }

    private val sdkStateService: SDKStateService by lazy {
        DependencyContainer.getSDKStateService()
    }

    // Buffer tracking
    private var bufferingStartTime: Long? = null
    private var bufferCount: Int = 0
    private var totalBufferDuration: Long = 0L

    // Seek tracking
    private var seekingStartTime: Long? = null
    private var seekCount: Int = 0
    private var totalSeekDuration: Long = 0L
    private var maxSeekDuration: Long = 0L

    // View metrics tracking
    private var longPlayerPlayHeadTime: Int? = null
    private var mathMaxValue: Double = 0.0
    private var mathMaxValue1: Double = 0.0
    private var incrementOfChangedTime: Long = 0L
    private var incrementOfMaxAndChangedValue: Double = 0.0
    private var incrementOfMaxMindChangedValue1: Double = 0.0

    /**
     * Called when buffering event occurs
     */
    fun onBufferingEvent() {
        bufferingStartTime = System.currentTimeMillis()
        bufferCount++
        Logger.log(TAG, "Buffering started. Count: $bufferCount")
    }

    /**
     * Called when buffered event occurs
     */
    fun onBufferedEvent() {
        bufferingStartTime?.let { startTime ->
            val duration = System.currentTimeMillis() - startTime
            totalBufferDuration += duration
            bufferingStartTime = null
            Logger.log(
                TAG,
                "Buffering ended. Duration: ${duration}ms, Total: ${totalBufferDuration}ms"
            )
        }
    }

    /**
     * Called when seeking event occurs
     */
    fun onSeekingEvent() {
        seekingStartTime = System.currentTimeMillis()
        seekCount++
        Logger.log(TAG, "Seeking started. Count: $seekCount")
    }

    /**
     * Called when seeked event occurs
     */
    fun onSeekedEvent() {
        seekingStartTime?.let { startTime ->

            val duration = System.currentTimeMillis() - startTime
            Logger.log(
                TAG,
                "Seeking ended. Duration: Start Time $startTime ${System.currentTimeMillis()}"
            )
            totalSeekDuration += duration
            if (duration > maxSeekDuration) {
                maxSeekDuration = duration
            }
            seekingStartTime = null
            Logger.log(
                TAG,
                "Seeking ended. Duration: ${duration}ms, Total: ${totalSeekDuration}ms, Max: ${maxSeekDuration}ms"
            )
        }
    }

    /**
     * Calculate buffer duration (time between buffering and buffered events)
     * Returns current buffering duration if still buffering, or 0 if not buffering
     */
    fun calculateBufferDuration(): String {
        return bufferingStartTime?.let { startTime ->
            (System.currentTimeMillis() - startTime).toString()
        } ?: "0"
    }

    /**
     * Calculate total buffer duration for the session
     */
    fun calculateRebufferDuration(): String {
        return totalBufferDuration.toString()
    }

    /**
     * Calculate buffer count for the session
     */
    fun calculateRebufferCount(): String {
        return bufferCount.toString()
    }

    /**
     * Calculate seek duration (time between seeking and seeked events)
     * Returns current seeking duration if still seeking, or 0 if not seeking
     */
    fun calculateSeekDuration(): String {
        return seekingStartTime?.let { startTime ->
            (System.currentTimeMillis() - startTime).toString()
        } ?: "0"
    }

    /**
     * Calculate total seek duration for the session
     */
    fun calculateTotalSeekDuration(): Long {
        return totalSeekDuration
    }

    /**
     * Calculate maximum seek duration for the session
     */
    fun calculateMaxSeekDuration(): String {
        return maxSeekDuration.toString()
    }

    /**
     * Calculate seek count for the session
     */
    fun calculateSeekCount(): String {
        return seekCount.toString()
    }

    /**
     * Calculate rebuffer frequency using formula: bufferCount / ViewWatchCounter.value
     * Returns the frequency of buffering events per watch time unit
     */
    fun getRebufferFrequency(): String {
        val watchTime = ViewWatchCounter.value
        return if (watchTime > 0) {
            val frequency = bufferCount.toDouble() / watchTime.toInt()
            frequency.toString()
        } else {
            "0"
        }
    }

    /**
     * Calculate rebuffer percentage using formula: bufferDuration / ViewWatchCounter.value
     * Returns the percentage of time spent buffering relative to total watch time
     */
    fun getRebufferPercentage(): String {
        val watchTime = ViewWatchCounter.value
        return if (watchTime > 0) {
            val percentage = (totalBufferDuration.toDouble() / watchTime)
            percentage.toString()
        } else {
            "0"
        }
    }


    /**
     * Set player playhead time for view metrics calculation
     */
    fun setPlayerPlayHeadTime(playHeadTime: Int?) {
        longPlayerPlayHeadTime = playHeadTime
    }

    /**
     * Calculate view metrics based on time change - equivalent to Java CallToChangeTime function
     * This function calculates upscaling/downscaling percentages and dispatches view metric events
     */
    fun updateScalingMetrics(localUpdateTime: Int?) {
        val playerListenerState = sdkStateService.sdkState.value.playerListener
        longPlayerPlayHeadTime?.let { playerPlayHeadTime ->
            val changedTime = (localUpdateTime ?: 0) - playerPlayHeadTime

            val playerWidth = playerListenerState?.playerWidth()
            val playerHeight = playerListenerState?.playerHeight()
            val videoWidth = playerListenerState?.videoSourceWidth()
            val videoHeight = playerListenerState?.videoSourceHeight()
            if (changedTime >= 0L && playerWidth != null && playerHeight != null
                && videoWidth != null && videoHeight != null) {
                // Calculate scaling ratios - equivalent to Java function logic
                val widthOfSourceWidth = playerWidth / videoWidth
                val heightOfSourceHeight = playerHeight / videoHeight
                val minDifferenceFeatherweight = minOf(widthOfSourceWidth, heightOfSourceHeight)
                val maxCalculationOfValue = maxOf(0.0, minDifferenceFeatherweight - 1.0)
                val calumniationOfValue1 = maxOf(0.0, 1.0 - minDifferenceFeatherweight)

                // Update maximum values
                mathMaxValue = maxOf(mathMaxValue, maxCalculationOfValue)
                mathMaxValue1 = maxOf(mathMaxValue1, calumniationOfValue1)

                // Update incremental values
                incrementOfChangedTime += changedTime
                incrementOfMaxAndChangedValue += maxCalculationOfValue * changedTime
                incrementOfMaxMindChangedValue1 += calumniationOfValue1 * changedTime

                Logger.log(
                    TAG,
                    "View metrics calculated - changedTime: $changedTime, maxValue: $mathMaxValue, maxValue1: $mathMaxValue1"
                )

                sdkStateService.updateScalingMetrics(
                    viewMaxUpScalePercentage = mathMaxValue,
                    viewMaxDownScalePercentage = mathMaxValue1,
                    viewTotalUpScaling = incrementOfMaxAndChangedValue,
                    viewTotalDownScaling = incrementOfMaxMindChangedValue1,
                    viewTotalContentPlaybackTime = incrementOfChangedTime
                )
            }

            // Reset playhead time regardless of whether calculation was performed
            longPlayerPlayHeadTime = null
        }
    }

    /**
     * Calculate maximum upscale percentage
     */
    fun calculateMaxUpScalePercentage(): String {
        return mathMaxValue.toString()
    }

    /**
     * Calculate maximum downscale percentage
     */
    fun calculateMaxDownScalePercentage(): String {
        return mathMaxValue1.toString()
    }

    /**
     * Calculate total upscaling value
     */
    fun calculateTotalUpScaling(): String {
        return incrementOfMaxAndChangedValue.toString()
    }

    /**
     * Calculate total downscaling value
     */
    fun calculateTotalDownScaling(): String {
        return incrementOfMaxMindChangedValue1.toString()
    }

    /**
     * Calculate total content playback time
     */
    fun calculateViewTotalContentPlaybackTime(): String {
        return incrementOfChangedTime.toString()
    }

    /**
     * Calculate video content playback time (alias for total content playback time)
     */
    fun calculateVideoContentPlaybackTime(): String {
        return calculateViewTotalContentPlaybackTime()
    }

    /**
     * Calculate buffer frequency
     */
    fun calculateBufferFrequency(): String {
        return getRebufferFrequency()
    }

    /**
     * Calculate buffer percentage
     */
    fun calculateBufferPercentage(): String {
        return getRebufferPercentage()
    }

    /**
     * Check if player is in full screen mode
     */
    fun isPlayerFullScreen(): Boolean {
        // This would need to be implemented based on your player's fullscreen state
        // For now, returning false as a placeholder
        return false
    }

    /**
     * Reset all counters and durations
     */
    fun reset() {
        bufferingStartTime = null
        bufferCount = 0
        totalBufferDuration = 0L

        seekingStartTime = null
        seekCount = 0
        totalSeekDuration = 0L
        maxSeekDuration = 0L

        // Reset view metrics
        longPlayerPlayHeadTime = null
        mathMaxValue = 0.0
        mathMaxValue1 = 0.0
        incrementOfChangedTime = 0L
        incrementOfMaxAndChangedValue = 0.0
        incrementOfMaxMindChangedValue1 = 0.0

        Logger.log(TAG, "All counters and durations reset")
    }
}