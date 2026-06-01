package io.fastpix.data.domain.model.events

import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.state.SessionService
import io.fastpix.data.scalingTracker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class PauseEvent(
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
    @SerialName("vemauppg") var viewMaxUpScalePercentage: Double? = null,
    @SerialName("vemadopg") var viewMaxDownScalePercentage: Double? = null,
    @SerialName("vetlug") var viewTotalUpScaling: Double? = null,
    @SerialName("vetldg") var viewTotalDownScaling: Double? = null,
    @SerialName("vetlctpbti") var viewTotalContentPlayBackTime: String? = null,
    @SerialName("plispu") var playerIsPaused: String? = null,
    @SerialName("snepti") var sessionExpiredTime: String? = null,
) : BaseEvent

object PauseEventBuilder {

    fun build(
        configService: SDKConfiguration,
        playheadTimeOverride: Int? = null
    ): PauseEvent {
        val base = BaseEventFactory.create(configService)
        val playheadTime = playheadTimeOverride ?: base.playheadTime
        val playerListener = configService.playerListener
        val currentPlayheadTime = playheadTimeOverride ?: playerListener.playHeadTime() ?: 0
        scalingTracker.calculateScalingForCurrentInterval(currentPlayheadTime.toLong())
        return PauseEvent(
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
            viewTotalContentPlayBackTime = scalingTracker.getTotalPlaybackTime().toString(),
            playerIsPaused = playerListener.isPause()?.toString(),
            sessionExpiredTime = SessionService.getSessionExpireTime()?.toString(),
            viewMaxUpScalePercentage = scalingTracker.getCurrentMaxUpscale(),
            viewMaxDownScalePercentage = scalingTracker.getCurrentMaxDownscale(),
            viewTotalUpScaling = scalingTracker.getTotalUpscalingTimeWeighted(),
            viewTotalDownScaling = scalingTracker.getTotalDownscalingTimeWeighted(),
            eventName = "pause"
        )
    }
}
