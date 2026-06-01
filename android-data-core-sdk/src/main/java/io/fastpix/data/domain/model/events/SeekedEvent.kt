package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.wallclock.ViewWatchCounter
import io.fastpix.data.di.DependencyContainer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class SeekedEvent(
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
    @SerialName("vesedu") var viewSeekDuration: Long? = null,
    @SerialName("vemaseti") var viewMaxSeekDuration: Long? = null,
    @SerialName("veseco") var viewSeekCount: String? = null,
) : BaseEvent

object SeekedEventBuilder {

    fun build(
        configService: SDKConfiguration,
        playheadTimeOverride: Int? = null
    ): SeekedEvent {
        val sdkStateService = DependencyContainer.getSDKStateService()
        val eventDataCalculator = DependencyContainer.getEventDataCalculator()
        val playerListener = sdkStateService.sdkState.value.playerListener
        if (playerListener?.isPause() == true) {
            ViewWatchCounter.pause()
        } else {
            ViewWatchCounter.start()
        }
        val base = BaseEventFactory.create(configService)
        eventDataCalculator.onSeekedEvent()
        val playheadTime = playheadTimeOverride ?: base.playheadTime
        val seekDuration = eventDataCalculator.calculateTotalSeekDuration()
        val seekCount = eventDataCalculator.calculateSeekCount()
        return SeekedEvent(
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
            viewSeekDuration = seekDuration,
            viewMaxSeekDuration = seekDuration,
            viewSeekCount = seekCount,
            eventName = "seeked"
        )
    }
}
