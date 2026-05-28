package io.fastpix.data

import android.content.Context
import android.util.Log
import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.enums.PlayerEventType
import io.fastpix.data.domain.lifecycle.SdkLifecycleState
import io.fastpix.data.domain.model.events.BufferedEventBuilder
import io.fastpix.data.domain.model.events.BufferingEventBuilder
import io.fastpix.data.domain.model.events.EndedEventBuilder
import io.fastpix.data.domain.model.events.ErrorEventBuilder
import io.fastpix.data.domain.model.events.PauseEventBuilder
import io.fastpix.data.domain.model.events.PlayEventBuilder
import io.fastpix.data.domain.model.events.PlayerReadyEventBuilder
import io.fastpix.data.domain.model.events.PlayingEventBuilder
import io.fastpix.data.domain.model.events.PulseEventBuilder
import io.fastpix.data.domain.model.events.RequestCancelledEventBuilder
import io.fastpix.data.domain.model.events.RequestCompletedEventBuilder
import io.fastpix.data.domain.model.events.RequestFailedEventBuilder
import io.fastpix.data.domain.model.events.SeekedEventBuilder
import io.fastpix.data.domain.model.events.SeekingEventBuilder
import io.fastpix.data.domain.model.events.VariantChangedEventBuilder
import io.fastpix.data.domain.model.events.ViewBeginEventBuilder
import io.fastpix.data.domain.model.events.ViewCompletedEventBuilder
import io.fastpix.data.domain.repository.EventDispatcher
import io.fastpix.data.domain.state.SDKStateService
import io.fastpix.data.domain.state.SessionService
import io.fastpix.data.domain.wallclock.ViewWatchCounter
import io.fastpix.data.sdkBuild.SDKBuildConfig
import io.fastpix.data.storage.EventJsonCodec
import io.fastpix.data.utils.Logger
import io.fastpix.data.utils.ScalingTracker
import java.util.concurrent.atomic.AtomicReference

/**
 * FastPix Data SDK - Main entry point. Lifecycle is managed via [SdkLifecycleState].
 * Events are only dispatched when state is [SdkLifecycleState.INITIALIZED].
 */
val scalingTracker = ScalingTracker()

class FastPixDataSDK {

    companion object {
        private const val TAG = "FastPixDataSDK"
    }

    private val lifecycleState = AtomicReference(SdkLifecycleState.NOT_INITIALIZED)
    private var configuration: SDKConfiguration? = null
    private var context: Context? = null

    private var eventDispatcher: EventDispatcher? = null
    private var sdkStateService: SDKStateService? = null
    private var sessionCreatedAtMs: Long = 0L
    private var lastVisibleAtMs: Long = 0L
    private var totalVisibleDurationMs: Long = 0L

    private fun currentState(): SdkLifecycleState = lifecycleState.get()

    /** True when the SDK accepts events (state is [SdkLifecycleState.INITIALIZED]). */
    fun isInitialized(): Boolean = currentState().isAcceptingEvents()

    /**
     * Initialize the FastPix SDK. Validates configuration, initializes dependencies and pipeline.
     * Multiple calls are safely ignored when already [SdkLifecycleState.INITIALIZED].
     */
    @Synchronized
    fun initialize(config: SDKConfiguration, context: Context) {
        Logger.configure(config.enableLogging && true)
        when (currentState()) {
            SdkLifecycleState.INITIALIZED -> return
            SdkLifecycleState.INITIALIZING -> return
            SdkLifecycleState.RELEASING, SdkLifecycleState.RELEASED -> {
                Logger.logWarning(
                    TAG,
                    "initialize() ignored: state is ${currentState()}"
                )
                return
            }

            SdkLifecycleState.NOT_INITIALIZED -> {}
        }

        if (!lifecycleState.get().canTransitionTo(SdkLifecycleState.INITIALIZING)) return
        lifecycleState.set(SdkLifecycleState.INITIALIZING)

        this.context = context.applicationContext
        this.configuration = config

        if (config.workspaceId.isBlank()) {
            Logger.logWarning(TAG, "Invalid config: workspaceId is blank")
            lifecycleState.set(SdkLifecycleState.NOT_INITIALIZED)
            return
        }

        if (config.beaconUrl?.isNotEmpty() == true) {
            SDKBuildConfig.SDK_URL = "https://${config.workspaceId}.${config.beaconUrl}"
        } else {
            SDKBuildConfig.SDK_URL = "https://${config.workspaceId}.anlytix.io"
        }

        DependencyContainer.initialize(context)
        ViewWatchCounter.reset()
        initializeDependencies()

        ViewWatchCounter.start()
        sessionCreatedAtMs = System.currentTimeMillis()
        lastVisibleAtMs = sessionCreatedAtMs
        totalVisibleDurationMs = 0L
        lifecycleState.set(SdkLifecycleState.INITIALIZED)
        Logger.log(
            TAG,
            "TRACE_KEY: traceId=${SessionService.getTraceId() ?: "none"} sessionId=${SessionService.getSessionId() ?: "none"} videoId=${config.videoData?.videoId ?: "none"}"
        )
        Logger.log(
            TAG,
            "${Logger.SDK_INITIALIZED}: debugEnabled=${config.enableLogging && true}, videoId=${config.videoData?.videoId ?: "none"}"
        )
    }

    private fun initializeDependencies() {
        if (DependencyContainer.getViewerPref()?.getViewerId() == null) {
            DependencyContainer.getViewerPref()?.viewerId(java.util.UUID.randomUUID().toString())
        }
        DependencyContainer.getViewerPref()?.saveSdkUrl(SDKBuildConfig.SDK_URL)
        SessionService.initializeSession()
        eventDispatcher = DependencyContainer.getEventDispatcher()
        sdkStateService = DependencyContainer.getSDKStateService()
        configuration?.let { sdkStateService?.updateSDKConfiguration(it) }
        eventDispatcher?.let { dispatcher ->
            kotlinx.coroutines.runBlocking {
                dispatcher.onSdkInitialized()
            }
        }
    }

    private data class EventDispatchSpec(
        val eventName: String,
        val build: (SDKConfiguration, Int?) -> io.fastpix.data.domain.model.events.BaseEvent,
        val sideEffect: () -> Unit
    )

    private val eventDispatchTable: Map<PlayerEventType, EventDispatchSpec> by lazy {
        mapOf(
            PlayerEventType.play to EventDispatchSpec(
                "play",
                { c, _ -> PlayEventBuilder.build(c) },
                { ViewWatchCounter.start(); lastVisibleAtMs = System.currentTimeMillis() }
            ),
            PlayerEventType.playerReady to EventDispatchSpec(
                "playerReady",
                { c, _ -> PlayerReadyEventBuilder.build(c) },
                { ViewWatchCounter.start() }
            ),
            PlayerEventType.seeked to EventDispatchSpec(
                "seeked",
                { c, o -> SeekedEventBuilder.build(c, o) },
                {}
            ),
            PlayerEventType.variantChanged to EventDispatchSpec(
                "variantChanged",
                { c, _ -> VariantChangedEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.playing to EventDispatchSpec(
                "playing",
                { c, _ -> PlayingEventBuilder.build(c) },
                { ViewWatchCounter.start() }
            ),
            PlayerEventType.seeking to EventDispatchSpec(
                "seeking",
                { c, o -> SeekingEventBuilder.build(c, o) },
                {}
            ),
            PlayerEventType.pause to EventDispatchSpec(
                "pause",
                { c, o -> PauseEventBuilder.build(c, o) },
                ::onPauseSideEffect
            ),
            PlayerEventType.buffering to EventDispatchSpec(
                "buffering_start",
                { c, _ -> BufferingEventBuilder.build(c) },
                { ViewWatchCounter.start() }
            ),
            PlayerEventType.buffered to EventDispatchSpec(
                "buffering_end",
                { c, _ -> BufferedEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.ended to EventDispatchSpec(
                "ended",
                { c, o -> EndedEventBuilder.build(c, o) },
                { ViewWatchCounter.pause() }
            ),
            PlayerEventType.viewCompleted to EventDispatchSpec(
                "viewCompleted",
                { c, _ -> ViewCompletedEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.error to EventDispatchSpec(
                "error",
                { c, _ -> ErrorEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.requestCanceled to EventDispatchSpec(
                "requestCanceled",
                { c, _ -> RequestCancelledEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.requestFailed to EventDispatchSpec(
                "requestFailed",
                { c, _ -> RequestFailedEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.requestCompleted to EventDispatchSpec(
                "requestCompleted",
                { c, _ -> RequestCompletedEventBuilder.build(c) },
                {}
            ),
            PlayerEventType.pulse to EventDispatchSpec(
                "pulse",
                { c, _ -> PulseEventBuilder.build(c) },
                {}
            ),
        )
    }

    private fun onPauseSideEffect() {
        ViewWatchCounter.pause()
        if (lastVisibleAtMs > 0L) {
            totalVisibleDurationMs += (System.currentTimeMillis() - lastVisibleAtMs).coerceAtLeast(0L)
        }
    }

    /**
     * Dispatch a player event. Events are only enqueued when state is [SdkLifecycleState.INITIALIZED].
     * Player adapters must use this only; they must not send to the network directly.
     */
    fun dispatchEvent(event: PlayerEventType, playheadTimeOverride: Int? = null) {
        if (!currentState().isAcceptingEvents()) {
            Logger.logWarning(TAG, "EVENT_SKIPPED: sdk not accepting events, event=$event")
            return
        }
        val config = configuration ?: run {
            Logger.logWarning(TAG, "EVENT_SKIPPED: missing configuration, event=$event")
            return
        }
        val dispatcher = eventDispatcher ?: run {
            Logger.logWarning(TAG, "EVENT_SKIPPED: missing dispatcher, event=$event")
            return
        }

        if (!SessionService.validateSession()) {
            handleSessionRecreation(event, playheadTimeOverride, config, dispatcher)
            return
        }

        val videoId = config.videoData?.videoId
        val playerInstanceId = sdkStateService?.sdkState?.value?.playerId

        if (event == PlayerEventType.viewBegin) {
            handleViewBegin(config, dispatcher, videoId, playerInstanceId)
            return
        }

        val spec = eventDispatchTable[event] ?: return
        spec.sideEffect()
        emitEvent(
            dispatcher,
            spec.build(config, playheadTimeOverride),
            spec.eventName,
            videoId,
            playerInstanceId
        )
    }

    private fun handleViewBegin(
        config: SDKConfiguration,
        dispatcher: EventDispatcher,
        videoId: String?,
        playerInstanceId: String?
    ) {
        val alreadySent = sdkStateService?.sdkState?.value?.isViewBeginCalled == true
        if (alreadySent) {
            Logger.logWarning(
                TAG,
                "VIEW_BEGIN_SKIPPED: viewBegin already dispatched for viewId=${sdkStateService?.sdkState?.value?.viewId}"
            )
            return
        }
        ViewWatchCounter.start()
        lastVisibleAtMs = System.currentTimeMillis()
        sdkStateService?.viewBeginCalled()
        Logger.log(TAG, "VIEW_BEGIN_TRIGGERED: video became visible")
        emitEvent(
            dispatcher,
            ViewBeginEventBuilder.build(config),
            "viewBegin",
            videoId,
            playerInstanceId
        )
    }

    private fun handleSessionRecreation(
        event: PlayerEventType,
        playheadTimeOverride: Int?,
        config: SDKConfiguration,
        dispatcher: EventDispatcher
    ) {
        Logger.logWarning(
            TAG,
            "SESSION_RECREATED: event=$event triggered without valid session; creating new view"
        )
        SessionService.initializeSession()
        sdkStateService?.clearSdkState()
        configuration?.let { sdkStateService?.updateSDKConfiguration(it) }
        ViewWatchCounter.reset()
        ViewWatchCounter.start()
        sessionCreatedAtMs = System.currentTimeMillis()
        lastVisibleAtMs = sessionCreatedAtMs
        totalVisibleDurationMs = 0L

        val newVideoId = config.videoData?.videoId
        val newPlayerInstanceId = sdkStateService?.sdkState?.value?.playerId

        sdkStateService?.viewBeginCalled()
        emitEvent(
            dispatcher,
            PlayerReadyEventBuilder.build(config),
            "playerReady",
            newVideoId,
            newPlayerInstanceId
        )
        emitEvent(
            dispatcher,
            ViewBeginEventBuilder.build(config),
            "viewBegin",
            newVideoId,
            newPlayerInstanceId
        )

        Logger.log(
            TAG,
            "SESSION_RECREATED: new viewId=${sdkStateService?.sdkState?.value?.viewId}, re-dispatching original event=$event"
        )
        dispatchEvent(event, playheadTimeOverride)
    }

    /**
     * Release the SDK: enqueue viewCompleted, flush pipeline (no event loss), then clean up.
     */
    fun release(playheadTimeOverride: Int? = null) {
        if (currentState().isReleased()) return
        if (!currentState().canTransitionTo(SdkLifecycleState.RELEASING)) {
            Logger.logWarning(TAG, "release() ignored: state is ${currentState()}")
            return
        }

        val config = configuration
        val releaseDuration = if (lastVisibleAtMs > 0L) {
            totalVisibleDurationMs + (System.currentTimeMillis() - lastVisibleAtMs).coerceAtLeast(0L)
        } else {
            totalVisibleDurationMs
        }
        Logger.log(
            TAG,
            "SESSION_ENDED: reason=release visibleDurationMs=$releaseDuration totalSessionMs=${System.currentTimeMillis() - sessionCreatedAtMs}"
        )

        config?.let {
            val viewCompletedEvent = ViewCompletedEventBuilder.build(it, playheadTimeOverride)
            val payload = EventJsonCodec.serialize(viewCompletedEvent)
            Logger.log(
                TAG,
                "EVENT_EMIT_BEFORE: event=viewCompleted payload=${payload ?: "serialization_failed"}"
            )
            eventDispatcher?.enqueue(viewCompletedEvent)
        }

        lifecycleState.set(SdkLifecycleState.RELEASING)
        Logger.log(TAG, "${Logger.SDK_RELEASE_STARTED}")

        eventDispatcher?.flushAndShutdown()
        DependencyContainer.prepareForRelease()
        eventDispatcher?.cleanThroughWorkManager(null)
        eventDispatcher = null
        sdkStateService = null
        configuration = null
        context = null

        lifecycleState.set(SdkLifecycleState.RELEASED)
        Logger.log(TAG, "${Logger.SDK_RELEASE_COMPLETED}")
    }

    private fun emitEvent(
        dispatcher: EventDispatcher,
        eventData: io.fastpix.data.domain.model.events.BaseEvent,
        eventName: String,
        videoId: String?,
        playerInstanceId: String?
    ) {
        Logger.log("EVENT_NAME_KEY", eventName)
        val payload = EventJsonCodec.serialize(eventData)
        Logger.log(
            TAG,
            "EVENT_EMIT_BEFORE: event=$eventName payload=${payload ?: "serialization_failed"}",
            videoId = videoId,
            playerInstanceId = playerInstanceId
        )
        val enqueued = dispatcher.dispatchEvent(eventData)
        if (enqueued) {
            Logger.log(
                TAG,
                "EVENT_EMIT_AFTER: event=$eventName enqueue=success",
                videoId = videoId,
                playerInstanceId = playerInstanceId
            )
        } else {
            Logger.logWarning(
                TAG,
                "EVENT_EMIT_FAILED: event=$eventName enqueue=false",
                videoId = videoId,
                playerInstanceId = playerInstanceId
            )
        }
    }
}
