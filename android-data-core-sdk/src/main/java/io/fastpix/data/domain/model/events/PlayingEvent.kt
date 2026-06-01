package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.utils.Utils
import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.scalingTracker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PlayingEvent(
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
    @SerialName("vetitofifr") var videoStartTime: String? = null,
    @SerialName("verbdu") var viewRebufferDuration: String? = null,
    @SerialName("verbfq") var viewBufferFrequency: String? = null,
    @SerialName("verbpg") var viewBufferPercentage: String? = null,
    @SerialName("plwt") var playerWidth: String? = null,
    @SerialName("plht") var playerHeight: String? = null,
    @SerialName("vdsodu") var videoDuration: String? = null,
    @SerialName("vdsowt") var videoSourceWidth: String? = null,
    @SerialName("vdsoht") var videoSourceHeight: String? = null,
    @SerialName("vdsour") var videoSourceUrl: String? = null,
    @SerialName("vdsohn") var videoHostName: String? = null,
) : BaseEvent

object PlayingEventBuilder {

    fun build(configService: SDKConfiguration): PlayingEvent {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val sdkState = sdkStateService.sdkState.value

        // Use configService.playerListener directly to avoid state synchronization issues
        // during rapid init/clear cycles
        val playerListener = configService.playerListener
        val base = BaseEventFactory.create(configService)
        val playerHeight = playerListener.playerHeight() ?: 0
        val playerWidth = playerListener.playerWidth() ?: 0
        val videoHeight = playerListener.videoSourceHeight() ?: 0
        val videoWidth = playerListener.videoSourceWidth() ?: 0
        if (playerListener.isFullScreen() == true) {
            sdkStateService.updateFullScreenUsed()
        }
        var calculateViewTimeToFirstFrame = 0L
        if (!sdkState.viewTimeToFirstFrameSent) {
            calculateViewTimeToFirstFrame = Utils.currentTimeStamp() - sdkState.viewBeginTime
            sdkStateService.updateViewTimeToFirstFrame()
        }
        scalingTracker.collectDataForScaling(
            currentPlayheadTime = (playerListener.playHeadTime() ?: 0).toLong(),
            playerWidth = playerWidth,
            playerHeight = playerHeight,
            videoSourceWidth = videoWidth,
            videoSourceHeight = videoHeight
        )
        return PlayingEvent(
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
            videoStartTime = calculateViewTimeToFirstFrame.toString(),
            viewRebufferDuration = eventDataCalculator.calculateRebufferDuration(),
            viewBufferFrequency = eventDataCalculator.calculateBufferFrequency(),
            viewBufferPercentage = eventDataCalculator.calculateBufferPercentage(),
            playerWidth = playerWidth.toString(),
            playerHeight = playerHeight.toString(),
            videoSourceWidth = videoWidth.toString(),
            videoSourceHeight = videoHeight.toString(),
            videoDuration = playerListener.sourceDuration()?.toString(),
            videoSourceUrl = configService.videoData?.videoSourceUrl
                ?: configService.playerListener.sourceUrl(),
            videoHostName = Utils.getDomain(
                configService.videoData?.videoSourceUrl ?: configService.playerListener.sourceUrl()
            ),
            eventName = "playing"
        )
    }

}