package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.scalingTracker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class BufferingEvent(
    @SerialName("wsid") override var workSpaceId: String? = null,
    @SerialName("veid") override var viewId: String? = null,
    @SerialName("vesqnu") override var viewSequenceNumber: String? = null,
    @SerialName("plsqnu") override var playerSequenceNumber: Int? = null,
    @SerialName("bedn") override var beaconDomain: String? = null,
    @SerialName("plphti") override var playheadTime: Int? = null,
    @SerialName("vitp") override var viewerTimeStamp: Long? = null,
    @SerialName("plinid") override var playerInstanceId: String? = null,
    @SerialName("vewati") override var viewWatchTime: String? = null,
    @SerialName("vicity") override var connectionType: String? = null,
    @SerialName("evna") override var eventName: String? = null,
    @SerialName("plisfl") override var isPlayerFullScreen: String? = null,
    @SerialName("verbdu") var viewRebufferDuration: String? = null,
    @SerialName("verbco") var viewRebufferCount: String? = null,
    @SerialName("verbfq") var viewBufferFrequency: String? = null,
    @SerialName("verbpg") var viewBufferPercentage: String? = null,
    @SerialName("vetlctpbti") var viewTotalContentPlayBackTime: String? = null,
    @SerialName("vemauppg") var viewMaxUpScalePercentage: Double? = null,
    @SerialName("vemadopg") var viewMaxDownScalePercentage: Double? = null,
    @SerialName("vetlug") var viewTotalUpScaling: Double? = null,
    @SerialName("vetldg") var viewTotalDownScaling: Double? = null,
) : BaseEvent


object BufferingEventBuilder {
    fun build(configService: SDKConfiguration): BufferingEvent {
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val base = BaseEventFactory.create(configService)
        eventDataCalculator.onBufferingEvent()
        val playerListener = configService.playerListener
        val currentPlayheadTime = playerListener.playHeadTime() ?: 0
        scalingTracker.calculateScalingForCurrentInterval(currentPlayheadTime.toLong())

        return BufferingEvent(
            workSpaceId = base.workSpaceId,
            viewId = base.viewId,
            viewSequenceNumber = base.viewSequenceNumber,
            playerSequenceNumber = base.playerSequenceNumber,
            beaconDomain = base.beaconDomain,
            playheadTime = base.playheadTime,
            viewerTimeStamp = base.viewerTimeStamp,
            playerInstanceId = base.playerInstanceId,
            viewWatchTime = base.viewWatchTime,
            connectionType = base.connectionType,
            isPlayerFullScreen = base.isPlayerFullScreen,
            viewRebufferDuration = eventDataCalculator.calculateRebufferDuration(),
            viewRebufferCount = eventDataCalculator.calculateRebufferCount(),
            viewBufferFrequency = eventDataCalculator.calculateBufferFrequency(),
            viewBufferPercentage = eventDataCalculator.calculateBufferPercentage(),
            viewTotalContentPlayBackTime = scalingTracker.getTotalPlaybackTime().toString(),
            viewMaxUpScalePercentage = scalingTracker.getCurrentMaxUpscale(),
            viewMaxDownScalePercentage = scalingTracker.getCurrentMaxDownscale(),
            viewTotalUpScaling = scalingTracker.getTotalUpscalingTimeWeighted(),
            viewTotalDownScaling = scalingTracker.getTotalDownscalingTimeWeighted(),
            eventName = "buffering"
        )
    }
}