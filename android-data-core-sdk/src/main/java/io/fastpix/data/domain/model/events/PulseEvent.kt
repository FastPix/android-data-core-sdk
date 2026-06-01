package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.scalingTracker
import io.fastpix.data.utils.Utils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class PulseEvent(
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
    @SerialName("verbfq") var viewBufferFrequency: String? = null,
    @SerialName("verbpg") var viewBufferPercentage: String? = null,
    @SerialName("plwt") var playerWidth: String? = null,
    @SerialName("plht") var playerHeight: String? = null,
    @SerialName("rqvdwt") var videoWidth: String? = null,
    @SerialName("rqvdht") var videoHeight: String? = null,
    @SerialName("verbdu") var viewRebufferDuration: String? = null,
    @SerialName("vdsodu") var videoDuration: String? = null,
    @SerialName("vetlctpbti") var viewTotalContentPlayBackTime: String? = null,
    @SerialName("vemauppg") var viewMaxUpScalePercentage: Double? = null,
    @SerialName("vemadopg") var viewMaxDownScalePercentage: Double? = null,
    @SerialName("vetlug") var viewTotalUpScaling: Double? = null,
    @SerialName("vetldg") var viewTotalDownScaling: Double? = null,
    @SerialName("vdsour") var videoSourceUrl: String? = null,
    @SerialName("vdsohn") var videoHostName: String? = null,
    @SerialName("vdcn") var videoCDN: String? = null,
) : BaseEvent

object PulseEventBuilder {
    fun build(configService: SDKConfiguration): PulseEvent {
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val playerListener = configService.playerListener
        val currentPlayheadTime = playerListener.playHeadTime() ?: 0
        val playerHeight = playerListener.playerHeight() ?: 0
        val playerWidth = playerListener.playerWidth() ?: 0
        val videoHeight = playerListener.videoSourceHeight() ?: 0
        val videoWidth = playerListener.videoSourceWidth() ?: 0
        scalingTracker.collectDataForScaling(
            currentPlayheadTime = currentPlayheadTime.toLong(),
            playerWidth = playerWidth,
            playerHeight = playerHeight,
            videoSourceWidth = videoWidth,
            videoSourceHeight = videoHeight
        )
        val base = BaseEventFactory.create(configService)
        return PulseEvent(
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
            viewBufferFrequency = eventDataCalculator.calculateBufferFrequency(),
            viewRebufferDuration = eventDataCalculator.calculateRebufferDuration(),
            viewBufferPercentage = eventDataCalculator.calculateBufferPercentage(),
            playerWidth = playerListener.playerWidth()?.toString(),
            playerHeight = playerListener.playerHeight()?.toString(),
            videoDuration = playerListener.sourceDuration()?.toString(),
            videoWidth = playerListener.videoSourceWidth()?.toString(),
            videoHeight = playerListener.videoSourceHeight()?.toString(),
            viewTotalContentPlayBackTime = scalingTracker.getTotalPlaybackTime().toString(),
            viewMaxUpScalePercentage = scalingTracker.getCurrentMaxUpscale(),
            viewMaxDownScalePercentage = scalingTracker.getCurrentMaxDownscale(),
            viewTotalUpScaling = scalingTracker.getTotalUpscalingTimeWeighted(),
            viewTotalDownScaling = scalingTracker.getTotalDownscalingTimeWeighted(),
            videoSourceUrl = configService.videoData?.videoSourceUrl
                ?: configService.playerListener.sourceUrl(),
            videoHostName = Utils.getDomain(
                configService.videoData?.videoSourceUrl
                    ?: configService.playerListener.sourceUrl()
            ),
            videoCDN = configService.videoData?.videoCDN,
            eventName = "pulse"
        )
    }
}
