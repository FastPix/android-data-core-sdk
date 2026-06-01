package io.fastpix.data.domain.model.events


import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.di.DependencyContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class VariantChangedEvent(
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
    @SerialName("vdsowt") var videoSourceWidth: String? = null,
    @SerialName("vdsoht") var videoSourceHeight: String? = null,
    @SerialName("vdid") var videoId: String? = null,
    @SerialName("vdsofs") var frameRate: Int? = null,
    @SerialName("vdsomity") var mimeType: String? = null,
    @SerialName("vdsobi") var bitrate: String? = null,
) : BaseEvent


object VariantChangedEventBuilder {
    fun build(configService: SDKConfiguration): VariantChangedEvent {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val sdkState = sdkStateService.sdkState.value
        val base = BaseEventFactory.create(configService)
        val playerListener = configService.playerListener

        return VariantChangedEvent(
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
            videoSourceWidth = playerListener.videoSourceWidth().toString(),
            videoSourceHeight = playerListener.videoSourceHeight().toString(),
            videoId = sdkState.videoDataDetails?.videoId,
            frameRate = playerListener.sourceAdvertiseFrameRate(),
            mimeType = playerListener.mimeType(),
            bitrate = playerListener.sourceAdvertisedBitrate(),
            eventName = "variantChanged"
            )
    }
}
