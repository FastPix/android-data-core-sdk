package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PlayerReadyEvent(
    @SerialName("wsid") override val workSpaceId: String? = null,
    @SerialName("veid") override val viewId: String? = null,
    @SerialName("vesqnu") override val viewSequenceNumber: String? = null,
    @SerialName("plsqnu") override val playerSequenceNumber: Int? = null,
    @SerialName("bedn") override val beaconDomain: String? = null,
    @SerialName("plphti") override val playheadTime: Int? = null,
    @SerialName("vitp") override val viewerTimeStamp: Long? = null,
    @SerialName("plinid") override val playerInstanceId: String? = null,
    @SerialName("vewati") override val viewWatchTime: String? = null,
    @SerialName("vicity") override val connectionType: String? = null,
    @SerialName("evna") override var eventName: String? = null,
    @SerialName("plisfl") override var isPlayerFullScreen: String? = null,
    @SerialName("plitti") val playerInitTime: String? = null,
    @SerialName("plwt") val playerWidth: Int? = null,
    @SerialName("plht") val playerHeight: Int? = null,
    @SerialName("vdsodu") val videoDuration: String? = null,
) : BaseEvent


object PlayerReadyEventBuilder {

    fun build(config: SDKConfiguration): PlayerReadyEvent {
        val base = BaseEventFactory.create(config)
        val state = DependencyContainer.getSDKStateService().sdkState.value
        val listener = config.playerListener
        val initTime = System.currentTimeMillis() - state.viewBeginTime
        return PlayerReadyEvent(
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
            playerInitTime = initTime.toString(),
            playerWidth = listener.playerWidth(),
            playerHeight = listener.playerHeight(),
            videoDuration = listener.sourceDuration()?.toString(),
            eventName = "playerReady"
        )
    }
}