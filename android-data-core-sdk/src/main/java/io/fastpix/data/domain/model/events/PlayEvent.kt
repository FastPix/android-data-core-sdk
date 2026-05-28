package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.utils.Utils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PlayEvent(
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
    @SerialName("vdid") var videoId: String? = null,
    @SerialName("verbdu") var viewRebufferDuration: String? = null,
    @SerialName("verbfq") var viewBufferFrequency: String? = null,
    @SerialName("verbpg") var viewBufferPercentage: String? = null,
    @SerialName("vdsodu") var videoDuration: String? = null,
    @SerialName("plwt") var playerWidth: String? = null,
    @SerialName("plht") var playerHeight: String? = null,
    @SerialName("rqvdwt") var videoWidth: String? = null,
    @SerialName("rqvdht") var videoHeight: String? = null,
    @SerialName("vdsour") var videoSourceUrl: String? = null,
    @SerialName("vdsohn") var videoHostName: String? = null,
) : BaseEvent


object PlayEventBuilder {
    fun build(configService: SDKConfiguration): PlayEvent {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val base = BaseEventFactory.create(configService)
        return PlayEvent(
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
            videoId = configService.videoData?.videoId,
            viewRebufferDuration = eventDataCalculator.calculateRebufferDuration(),
            viewBufferFrequency = eventDataCalculator.calculateBufferFrequency(),
            videoDuration = sdkStateService.sdkState.value.playerListener?.sourceDuration()
                ?.toString(),
            viewBufferPercentage = eventDataCalculator.calculateBufferPercentage(),
            playerWidth = sdkStateService.sdkState.value.playerListener?.playerWidth()?.toString(),
            playerHeight = sdkStateService.sdkState.value.playerListener?.playerHeight()
                ?.toString(),
            videoWidth = sdkStateService.sdkState.value.playerListener?.videoSourceWidth()
                ?.toString(),
            videoHeight = sdkStateService.sdkState.value.playerListener?.videoSourceHeight()
                ?.toString(),
            videoSourceUrl = configService.videoData?.videoSourceUrl
                ?: configService.playerListener.sourceUrl(),
            videoHostName = Utils.getDomain(
                configService.videoData?.videoSourceUrl ?: configService.playerListener.sourceUrl()
            ),
            eventName = "play"
        )
    }
}
