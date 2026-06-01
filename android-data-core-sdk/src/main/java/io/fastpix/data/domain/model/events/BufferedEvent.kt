package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BufferedEvent(
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
    @SerialName("verbfq") var viewBufferFrequency: String? = null,
    @SerialName("verbpg") var viewBufferPercentage: String? = null,
) : BaseEvent


object BufferedEventBuilder {
    fun build(configService: SDKConfiguration): BufferedEvent {
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val base = BaseEventFactory.create(configService)
        eventDataCalculator.onBufferedEvent()

        return BufferedEvent(
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
            viewBufferFrequency = eventDataCalculator.calculateBufferFrequency(),
            viewBufferPercentage = eventDataCalculator.calculateBufferPercentage(),
            eventName = "buffered"
        )
    }
}
