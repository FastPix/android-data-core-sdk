package io.fastpix.data.domain.model.events

import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.domain.SDKConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RequestCompletedEvent(
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
    @SerialName("rqid") var requestId: String? = null,
    @SerialName("rqur") var requestUrl: String? = null,
    @SerialName("rqty") var requestMethod: String? = null,
    @SerialName("rqrphs") var requestResponseHeaders: String? = null,
    @SerialName("rqhn") var requestHostName: String? = null,
    @SerialName("rqca") var requestCancel: String? = null,
) : BaseEvent

object RequestCompletedEventBuilder {
    fun build(configService: SDKConfiguration): RequestCompletedEvent {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val bandWidthModel = sdkStateService.sdkState.value.playerListener?.getBandWidthData()
        val base = BaseEventFactory.create(configService)
        return RequestCompletedEvent(
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
            requestId = bandWidthModel?.requestId,
            requestUrl = bandWidthModel?.requestUrl,
            requestMethod = bandWidthModel?.requestMethod,
            requestResponseHeaders = bandWidthModel?.requestResponseHeaders.toString(),
            requestHostName = bandWidthModel?.requestHostName,
            requestCancel = bandWidthModel?.requestCancel,
            eventName = "requestCompleted"
        )
    }
}
