package io.fastpix.data.domain.state

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.fastpix.data.utils.Logger
import java.util.UUID

object SessionService {

    private const val TAG = "SessionService"

    private var sessionId: String? = null
    private var traceId: String? = null
    private var sessionStartTime: Long? = null
    @Volatile
    private var lastEventTime: Long? = null
    @Volatile
    private var isSessionValid: Boolean = false
    private var sessionExpiryTime: Long? = null
    private val _sessionState = MutableLiveData<Boolean>()
    val sessionState: LiveData<Boolean> get() = _sessionState

    private const val SESSION_TIMEOUT_MS = 60 * 60 * 1000L // 1 hour

    /**
     * Start a new session (only after expiry or reset).
     */
    @Synchronized
    fun initializeSession() {
        val currentTime = System.currentTimeMillis()
        sessionId = UUID.randomUUID().toString()
        traceId = UUID.randomUUID().toString().substring(0, 8)
        sessionStartTime = currentTime
        sessionExpiryTime = currentTime + SESSION_TIMEOUT_MS
        lastEventTime = currentTime
        isSessionValid = true
        _sessionState.postValue(true)
        Logger.log(
            TAG,
            "SESSION_CREATED: new analytics session started traceId=${traceId ?: "none"}"
        )
    }

    /**
     * Check if session is still valid.
     * If expired, invalidate session and return false.
     */
    @Synchronized
    fun validateSession(): Boolean {
        val last = lastEventTime
        if (!isSessionValid || last == null) {
            Logger.logWarning(TAG, "SESSION_INVALID: session missing or not initialized")
            return false
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - last >= SESSION_TIMEOUT_MS) {
            Logger.logWarning(
                TAG,
                "SESSION_EXPIRED: inactivity exceeded timeoutMs=$SESSION_TIMEOUT_MS"
            )
            invalidateSession()
            return false
        }
        lastEventTime = currentTime
        return true
    }

    /**
     * Expire the current session.
     */
    private fun invalidateSession() {
        isSessionValid = false
        Logger.logWarning(TAG, "SESSION_INVALIDATED")
        sessionId = null
        traceId = null
        _sessionState.postValue(false)
    }

    /**
     * Manually reset all session data.
     */
    @Synchronized
    fun reset() {
        Logger.log(TAG, "SESSION_RESET")
        isSessionValid = false
        sessionId = null
        traceId = null
        sessionStartTime = null
        lastEventTime = null
        _sessionState.postValue(false)
    }

    fun getSessionId(): String? = sessionId
    fun getTraceId(): String? = traceId
    fun getSessionStartTime(): Long? = sessionStartTime
    fun getLastEventTime(): Long? = lastEventTime
    fun getSessionExpireTime(): Long? = sessionExpiryTime
    fun isSessionValid(): Boolean = validateSession()
}
