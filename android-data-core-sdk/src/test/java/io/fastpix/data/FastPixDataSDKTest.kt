package io.fastpix.data

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.enums.PlayerEventType
import io.fastpix.data.domain.lifecycle.SdkLifecycleState
import io.fastpix.data.domain.listeners.PlayerListener
import io.fastpix.data.domain.model.BandwidthModel
import io.fastpix.data.domain.model.ErrorModel
import io.fastpix.data.domain.model.events.PlayEvent
import io.fastpix.data.domain.model.events.PlayEventBuilder
import io.fastpix.data.domain.model.events.PlayerReadyEvent
import io.fastpix.data.domain.model.events.PlayerReadyEventBuilder
import io.fastpix.data.domain.model.events.ViewBeginEvent
import io.fastpix.data.domain.model.events.ViewBeginEventBuilder
import io.fastpix.data.domain.repository.EventDispatcher
import io.fastpix.data.domain.state.SDKStateService
import io.fastpix.data.domain.state.SessionService
import io.fastpix.data.domain.wallclock.ViewWatchCounter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.slot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class FastPixDataSDKTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockViewBeginEvent: ViewBeginEvent = mockk(relaxed = true)
    private val mockPlayerReadyEvent: PlayerReadyEvent = mockk(relaxed = true)
    private val mockPlayEvent: PlayEvent = mockk(relaxed = true)

    private val stubPlayerListener = object : PlayerListener {
        override fun playerHeight(): Int? = 720
        override fun playerWidth(): Int? = 1280
        override fun videoSourceWidth(): Int? = 1920
        override fun videoSourceHeight(): Int? = 1080
        override fun playHeadTime(): Int? = 1000
        override fun mimeType(): String? = "video/mp4"
        override fun sourceFps(): Int? = 30
        override fun sourceAdvertisedBitrate(): String? = "5000000"
        override fun sourceAdvertiseFrameRate(): Int? = 30
        override fun sourceDuration(): Int? = 60000
        override fun isPause(): Boolean? = false
        override fun isAutoPlay(): Boolean? = false
        override fun preLoad(): Boolean? = false
        override fun isBuffering(): Boolean? = false
        override fun playerCodec(): String? = "avc1"
        override fun sourceHostName(): String? = "example.com"
        override fun isLive(): Boolean? = false
        override fun sourceUrl(): String? = "https://example.com/video.mp4"
        override fun isFullScreen(): Boolean? = false
        override fun getBandWidthData(): BandwidthModel = BandwidthModel()
        override fun getPlayerError(): ErrorModel = ErrorModel()
        override fun getVideoCodec(): String? = "h264"
        override fun getSoftwareName(): String? = "TestPlayer"
        override fun getSoftwareVersion(): String? = "1.0"
    }

    private val sdk = FastPixDataSDK()
    private val sdkStateService = SDKStateService()
    private val dispatcher: EventDispatcher = mockk(relaxed = true)
    private val config = SDKConfiguration(
        workspaceId = "test-workspace",
        playerListener = stubPlayerListener,
        enableLogging = false
    )

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0

        mockkObject(ViewWatchCounter)
        every { ViewWatchCounter.start() } returns Unit
        every { ViewWatchCounter.pause() } returns Unit
        every { ViewWatchCounter.reset() } returns Unit
        every { ViewWatchCounter.value } returns 0L

        mockkObject(SessionService)
        every { SessionService.validateSession() } returns true
        every { SessionService.initializeSession() } returns Unit
        every { SessionService.getSessionId() } returns "test-session"
        every { SessionService.getTraceId() } returns "trace123"

        mockkObject(ViewBeginEventBuilder)
        every { ViewBeginEventBuilder.build(any()) } returns mockViewBeginEvent

        mockkObject(PlayerReadyEventBuilder)
        every { PlayerReadyEventBuilder.build(any()) } returns mockPlayerReadyEvent

        mockkObject(PlayEventBuilder)
        every { PlayEventBuilder.build(any()) } returns mockPlayEvent

        every { dispatcher.dispatchEvent(any()) } returns true

        injectPrivateField(sdk, "lifecycleState", AtomicReference(SdkLifecycleState.INITIALIZED))
        injectPrivateField(sdk, "configuration", config)
        injectPrivateField(sdk, "eventDispatcher", dispatcher)
        injectPrivateField(sdk, "sdkStateService", sdkStateService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // -- Duplicate viewBegin guard tests --

    @Test
    fun `viewBegin dispatches successfully on first call`() {
        sdk.dispatchEvent(PlayerEventType.viewBegin)

        verify(exactly = 1) { dispatcher.dispatchEvent(mockViewBeginEvent) }
    }

    @Test
    fun `viewBegin is blocked on second call with same viewId`() {
        sdk.dispatchEvent(PlayerEventType.viewBegin)
        sdk.dispatchEvent(PlayerEventType.viewBegin)

        verify(exactly = 1) { dispatcher.dispatchEvent(mockViewBeginEvent) }
    }

    @Test
    fun `viewBegin is blocked on third call with same viewId`() {
        sdk.dispatchEvent(PlayerEventType.viewBegin)
        sdk.dispatchEvent(PlayerEventType.viewBegin)
        sdk.dispatchEvent(PlayerEventType.viewBegin)

        verify(exactly = 1) { dispatcher.dispatchEvent(mockViewBeginEvent) }
    }

    @Test
    fun `isViewBeginCalled flag is set after first viewBegin`() {
        assertEquals(false, sdkStateService.sdkState.value.isViewBeginCalled)

        sdk.dispatchEvent(PlayerEventType.viewBegin)

        assertEquals(true, sdkStateService.sdkState.value.isViewBeginCalled)
    }

    // -- Session recovery tests --

    @Test
    fun `session recovery creates new viewId`() {
        sdk.dispatchEvent(PlayerEventType.viewBegin)
        val originalViewId = sdkStateService.sdkState.value.viewId

        every { SessionService.validateSession() } returnsMany listOf(false, true)

        sdk.dispatchEvent(PlayerEventType.play)
        val newViewId = sdkStateService.sdkState.value.viewId

        assertNotNull(originalViewId)
        assertNotNull(newViewId)
        assertNotEquals("Recovery should create a new viewId", originalViewId, newViewId)
    }

    @Test
    fun `session recovery emits playerReady and viewBegin for new view`() {
        every { SessionService.validateSession() } returnsMany listOf(false, true)

        sdk.dispatchEvent(PlayerEventType.play)

        verify(atLeast = 1) { dispatcher.dispatchEvent(mockPlayerReadyEvent) }
        verify(atLeast = 1) { dispatcher.dispatchEvent(mockViewBeginEvent) }
    }

    @Test
    fun `session recovery re-dispatches the original event`() {
        sdk.dispatchEvent(PlayerEventType.viewBegin)

        every { SessionService.validateSession() } returnsMany listOf(false, true)

        sdk.dispatchEvent(PlayerEventType.play)

        verify(atLeast = 1) { dispatcher.dispatchEvent(mockPlayEvent) }
    }

    @Test
    fun `session recovery sets isViewBeginCalled for new view`() {
        every { SessionService.validateSession() } returnsMany listOf(false, true)

        sdk.dispatchEvent(PlayerEventType.play)

        assertEquals(true, sdkStateService.sdkState.value.isViewBeginCalled)
    }

    @Test
    fun `session recovery resets sequence numbers via clearSdkState`() {
        sdkStateService.viewSequenceNumber()
        sdkStateService.viewSequenceNumber()
        assertEquals(2, sdkStateService.sdkState.value.viewSequenceNumber)

        every { SessionService.validateSession() } returnsMany listOf(false, true)
        sdk.dispatchEvent(PlayerEventType.play)

        val seqAfterRecovery = sdkStateService.sdkState.value.viewSequenceNumber
        assert(seqAfterRecovery < 2) {
            "Sequence should reset after clearSdkState, was $seqAfterRecovery"
        }
    }

    // -- Combined scenario: simulates the exact bug from the screenshots --

    @Test
    fun `full scenario - viewBegin then session recovery does not duplicate on same viewId`() {
        // 1. Normal viewBegin
        sdk.dispatchEvent(PlayerEventType.viewBegin)
        val firstViewId = sdkStateService.sdkState.value.viewId

        // 2. Some normal events
        sdk.dispatchEvent(PlayerEventType.play)

        // 3. Session expires (simulating long background)
        every { SessionService.validateSession() } returnsMany listOf(false, true)
        sdk.dispatchEvent(PlayerEventType.pause)
        val secondViewId = sdkStateService.sdkState.value.viewId

        // The recovery created a NEW view, not a duplicate on the old one
        assertNotEquals(
            "Recovery must not reuse the old viewId",
            firstViewId,
            secondViewId
        )
    }

    @Test
    fun `full scenario - multiple player inits without release only send one viewBegin`() {
        // Simulates creating multiple FastPixBaseMedia3Player instances
        // without releasing the previous one -- they all call dispatchEvent(viewBegin)
        // on the same SDK instance
        sdk.dispatchEvent(PlayerEventType.viewBegin) // Player A init
        sdk.dispatchEvent(PlayerEventType.viewBegin) // Player B init (no release of A)
        sdk.dispatchEvent(PlayerEventType.viewBegin) // Player C init (no release of B)

        verify(exactly = 1) { dispatcher.dispatchEvent(mockViewBeginEvent) }
    }

    private fun injectPrivateField(target: Any, fieldName: String, value: Any?) {
        val field = target::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
