package io.fastpix.data.utils

import android.util.Log
import io.fastpix.data.domain.state.SessionService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Logger private constructor() {

    companion object {
        private const val TAG = "FastPixAnalytics"
        private const val LEVEL_DEBUG = "DEBUG"
        private const val LEVEL_INFO = "INFO"
        private const val LEVEL_WARN = "WARN"
        private const val LEVEL_ERROR = "ERROR"
        private var isLoggingEnabled = false
        private val timeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        
        /**
         * Configure logging. Logs are emitted only for debug builds.
         */
        fun configure(enable: Boolean) {
            isLoggingEnabled = true
        }

        /**
         * Enable logging for backward compatibility.
         */
        fun enableLogging() {
            configure(true)
        }

        /**
         * Disable logging for the SDK
         */
        fun disableLogging() {
            isLoggingEnabled = false
        }
        
        /**
         * Check if logging is enabled
         */
        fun isLoggingEnabled(): Boolean = isLoggingEnabled
        
        /**
         * Log a debug message
         */
        fun log(message: String) {
            if (isLoggingEnabled) {
                Log.d(TAG, formatMessage(LEVEL_DEBUG, TAG, message, null, null, null))
            }
        }
        
        /**
         * Log a debug message with tag
         */
        fun log(tag: String, message: String) {
            if (isLoggingEnabled) {
                Log.d(tag, message)
            }
        }

        fun log(
            tag: String,
            message: String,
            sessionId: String? = null,
            videoId: String? = null,
            playerInstanceId: String? = null
        ) {
            if (isLoggingEnabled) {
                Log.d(
                    TAG,
                    formatMessage(LEVEL_DEBUG, tag, message, sessionId, videoId, playerInstanceId)
                )
            }
        }
        
        /**
         * Log an error message
         */
        fun logError(message: String, throwable: Throwable? = null) {
            if (isLoggingEnabled) {
                Log.e(TAG, formatMessage(LEVEL_ERROR, TAG, message, null, null, null), throwable)
            }
        }
        
        /**
         * Log an error message with tag
         */
        fun logError(tag: String, message: String, throwable: Throwable? = null) {
            if (isLoggingEnabled) {
                Log.e(TAG, formatMessage(LEVEL_ERROR, tag, message, null, null, null), throwable)
            }
        }

        fun logError(
            tag: String,
            message: String,
            sessionId: String? = null,
            videoId: String? = null,
            playerInstanceId: String? = null,
            throwable: Throwable? = null
        ) {
            if (isLoggingEnabled) {
                Log.e(
                    TAG,
                    formatMessage(LEVEL_ERROR, tag, message, sessionId, videoId, playerInstanceId),
                    throwable
                )
            }
        }
        
        /**
         * Log an info message
         */
        fun logInfo(message: String) {
            if (isLoggingEnabled) {
                Log.i(TAG, formatMessage(LEVEL_INFO, TAG, message, null, null, null))
            }
        }
        
        /**
         * Log an info message with tag
         */
        fun logInfo(tag: String, message: String) {
            if (isLoggingEnabled) {
                Log.i(TAG, formatMessage(LEVEL_INFO, tag, message, null, null, null))
            }
        }

        fun logInfo(
            tag: String,
            message: String,
            sessionId: String? = null,
            videoId: String? = null,
            playerInstanceId: String? = null
        ) {
            if (isLoggingEnabled) {
                Log.i(
                    TAG,
                    formatMessage(LEVEL_INFO, tag, message, sessionId, videoId, playerInstanceId)
                )
            }
        }
        
        /**
         * Log a warning message
         */
        fun logWarning(message: String) {
            if (isLoggingEnabled) {
                Log.w(TAG, formatMessage(LEVEL_WARN, TAG, message, null, null, null))
            }
        }
        
        /**
         * Log a warning message with tag
         */
        fun logWarning(tag: String, message: String) {
            if (isLoggingEnabled) {
                Log.w(TAG, formatMessage(LEVEL_WARN, tag, message, null, null, null))
            }
        }

        fun logWarning(
            tag: String,
            message: String,
            sessionId: String? = null,
            videoId: String? = null,
            playerInstanceId: String? = null
        ) {
            if (isLoggingEnabled) {
                Log.w(
                    TAG,
                    formatMessage(LEVEL_WARN, tag, message, sessionId, videoId, playerInstanceId)
                )
            }
        }

        private fun formatMessage(
            level: String,
            tag: String,
            message: String,
            sessionId: String?,
            videoId: String?,
            playerInstanceId: String?
        ): String {
            val resolvedSessionId = sessionId ?: SessionService.getSessionId()
            val traceId = SessionService.getTraceId()
            return "ts=${timeFormatter.format(Date())} level=$level tag=$tag thread=${Thread.currentThread().name} traceId=${traceId ?: "none"} sessionId=${resolvedSessionId ?: "none"} videoId=${videoId ?: "none"} playerInstanceId=${playerInstanceId ?: "none"} msg=$message"
        }

        // --- Pipeline / lifecycle debug labels (use with log(tag, "LABEL: ...")) ---
        const val EVENT_ENQUEUED = "EVENT_ENQUEUED"
        const val EVENT_BATCHED = "EVENT_BATCHED"
        const val EVENT_SENT = "EVENT_SENT"
        const val EVENT_RETRY = "EVENT_RETRY"
        const val EVENT_DROPPED = "EVENT_DROPPED"
        const val SDK_INITIALIZED = "SDK_INITIALIZED"
        const val SDK_RELEASE_STARTED = "SDK_RELEASE_STARTED"
        const val SDK_RELEASE_COMPLETED = "SDK_RELEASE_COMPLETED"
    }
}
