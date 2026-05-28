package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.wallclock.ViewWatchCounter
import io.fastpix.data.utils.Utils
import io.fastpix.data.di.DependencyContainer
import kotlinx.serialization.SerialName


interface BaseEvent {
    @SerialName("wsid")
    val workSpaceId: String?

    @SerialName("veid")
    val viewId: String?

    @SerialName("vesqnu")
    val viewSequenceNumber: String?

    @SerialName("plsqnu")
    val playerSequenceNumber: Int?

    @SerialName("bedn")
    val beaconDomain: String?

    @SerialName("plphti")
    val playheadTime: Int?

    @SerialName("vitp")
    val viewerTimeStamp: Long?

    @SerialName("plinid")
    val playerInstanceId: String?

    @SerialName("vewati")
    val viewWatchTime: String?

    @SerialName("vicity")
    val connectionType: String?

    @SerialName("evna")
    var eventName: String?

    @SerialName("plisfl")
    var isPlayerFullScreen: String?
}


data class BaseEventData(
    val workSpaceId: String?,
    val viewId: String?,
    val viewSequenceNumber: String?,
    val playerSequenceNumber: Int?,
    val beaconDomain: String?,
    val playheadTime: Int?,
    val viewerTimeStamp: Long?,
    val playerInstanceId: String?,
    val viewWatchTime: String?,
    val connectionType: String?,
    var eventName: String?,
    var isPlayerFullScreen: String?
)


object BaseEventFactory {
    const val EMPTY_STRING = ""

    fun create(configService: SDKConfiguration): BaseEventData {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val sdkState = sdkStateService.sdkState.value
        sdkStateService.viewSequenceNumber()
        sdkStateService.playerSequenceNumber()
        val viewId = sdkState.viewId
        val currentTimeStamp = Utils.currentTimeStamp()
        val playerObserver = configService.playerListener
        val sequenceNumber = sdkState.viewSequenceNumber
        val playerSequenceNumber = sdkState.playerSequenceNumber
        val workSpaceId = configService.workspaceId
        val playerId = sdkState.playerId
        val playHeadTime = (sdkState.playheadTimeOverride ?: playerObserver.playHeadTime()) ?: 0
        val viewWatchTime = ViewWatchCounter.value.toString()
        val connectionType = Utils.checkNetworkType() ?: sdkState.connectionType
        if (playerObserver.isFullScreen() == true) {
            sdkStateService.updateFullScreenUsed()
        }

        return BaseEventData(
            workSpaceId = workSpaceId,
            viewId = viewId,
            viewSequenceNumber = sequenceNumber.toString(),
            playerSequenceNumber = playerSequenceNumber,
            beaconDomain = configService.beaconUrl ?: "anlytix.io",
            playheadTime = playHeadTime,
            viewerTimeStamp = currentTimeStamp,
            playerInstanceId = playerId,
            viewWatchTime = viewWatchTime,
            connectionType = connectionType,
            eventName = EMPTY_STRING,
            isPlayerFullScreen = sdkState.isFullScreen.toString()
        )
    }
}
