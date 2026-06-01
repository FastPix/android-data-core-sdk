package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class EndedEvent(
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
    @SerialName("plisfl") override var isPlayerFullScreen: String? = null,
    @SerialName("evna") override var eventName: String? = null,
    @SerialName("vectpbti") var videoContentPlaybackTime: String? = null,
    @SerialName("vetlctpbti") var viewTotalContentPlayBackTime: String? = null,
    @SerialName("verbdu") var viewRebufferDuration: String? = null,
    @SerialName("verbfq") var viewBufferFrequency: String? = null,
    @SerialName("verbpg") var viewBufferPercentage: String? = null,
) : BaseEvent

object EndedEventBuilder {
    fun build(
        configService: SDKConfiguration,
        playheadTimeOverride: Int? = null
    ): EndedEvent {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val sdkState = sdkStateService.sdkState.value
        val base = BaseEventFactory.create(configService)
        val playheadTime = playheadTimeOverride ?: base.playheadTime
        return EndedEvent(
            workSpaceId = base.workSpaceId,
            viewId = base.viewId,
            viewSequenceNumber = base.viewSequenceNumber,
            playerSequenceNumber = base.playerSequenceNumber,
            beaconDomain = base.beaconDomain,
            playheadTime = playheadTime,
            viewerTimeStamp = base.viewerTimeStamp,
            playerInstanceId = base.playerInstanceId,
            viewWatchTime = base.viewWatchTime,
            connectionType = base.connectionType,
            isPlayerFullScreen = base.isPlayerFullScreen,
            videoContentPlaybackTime = sdkState.viewTotalContentPlaybackTime.toString(),
            viewTotalContentPlayBackTime = sdkState.viewTotalContentPlaybackTime.toString(),
            viewRebufferDuration = eventDataCalculator.calculateRebufferDuration(),
            viewBufferFrequency = eventDataCalculator.calculateBufferFrequency(),
            viewBufferPercentage = eventDataCalculator.calculateBufferPercentage(),
            eventName = "ended"
        )
    }
}
