package io.fastpix.data.domain.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SDKStateServiceTest {

    private val stateService = SDKStateService()

    @Test
    fun `initial state has viewBeginCalled as false`() {
        assertFalse(stateService.sdkState.value.isViewBeginCalled)
    }

    @Test
    fun `initial state has a non-null viewId`() {
        assertNotNull(stateService.sdkState.value.viewId)
    }

    @Test
    fun `viewBeginCalled sets flag to true`() {
        stateService.viewBeginCalled()
        assertTrue(stateService.sdkState.value.isViewBeginCalled)
    }

    @Test
    fun `clearSdkState resets viewBeginCalled to false`() {
        stateService.viewBeginCalled()
        assertTrue(stateService.sdkState.value.isViewBeginCalled)

        stateService.clearSdkState()
        assertFalse(stateService.sdkState.value.isViewBeginCalled)
    }

    @Test
    fun `clearSdkState generates new viewId`() {
        val originalViewId = stateService.sdkState.value.viewId

        stateService.clearSdkState()
        val newViewId = stateService.sdkState.value.viewId

        assertNotNull(originalViewId)
        assertNotNull(newViewId)
        assertNotEquals("clearSdkState should generate a new viewId", originalViewId, newViewId)
    }

    @Test
    fun `clearSdkState generates new playerId`() {
        val originalPlayerId = stateService.sdkState.value.playerId

        stateService.clearSdkState()
        val newPlayerId = stateService.sdkState.value.playerId

        assertNotNull(originalPlayerId)
        assertNotNull(newPlayerId)
        assertNotEquals("clearSdkState should generate a new playerId", originalPlayerId, newPlayerId)
    }

    @Test
    fun `clearSdkState resets sequence numbers`() {
        stateService.viewSequenceNumber()
        stateService.viewSequenceNumber()
        stateService.playerSequenceNumber()
        assertEquals(2, stateService.sdkState.value.viewSequenceNumber)
        assertEquals(1, stateService.sdkState.value.playerSequenceNumber)

        stateService.clearSdkState()
        assertEquals(0, stateService.sdkState.value.viewSequenceNumber)
        assertEquals(0, stateService.sdkState.value.playerSequenceNumber)
    }

    @Test
    fun `viewBeginCalled then clearSdkState then viewBeginCalled tracks correctly`() {
        // First view
        stateService.viewBeginCalled()
        assertTrue(stateService.sdkState.value.isViewBeginCalled)

        // Session recovery creates new view
        stateService.clearSdkState()
        assertFalse(stateService.sdkState.value.isViewBeginCalled)

        // viewBegin dispatched for new view
        stateService.viewBeginCalled()
        assertTrue(stateService.sdkState.value.isViewBeginCalled)
    }

    @Test
    fun `multiple viewBeginCalled does not change state`() {
        stateService.viewBeginCalled()
        assertTrue(stateService.sdkState.value.isViewBeginCalled)

        stateService.viewBeginCalled()
        assertTrue(stateService.sdkState.value.isViewBeginCalled)
    }
}
