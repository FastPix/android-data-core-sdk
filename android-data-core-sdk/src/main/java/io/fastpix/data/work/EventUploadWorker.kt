package io.fastpix.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.fastpix.data.di.DependencyContainer
import io.fastpix.data.domain.model.EventRequest
import io.fastpix.data.domain.model.Metadata
import io.fastpix.data.sdkBuild.SDKBuildConfig
import io.fastpix.data.storage.SessionStatus
import io.fastpix.data.utils.Logger
import kotlinx.coroutines.CancellationException

class EventUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "EventUploadWorker"
        private const val MAX_BATCH_SIZE = 50
    }

    override suspend fun doWork(): Result {
        return try {
            Logger.log(
                TAG,
                "WORK_START: workId=$id attempt=$runAttemptCount"
            )
            ensureContainerReady()
            val eventStore = DependencyContainer.getEventStore()
            val apiService = DependencyContainer.getEventApiService()

            val allSessions = eventStore.getAllSessions()
            if (allSessions.isEmpty()) {
                Logger.log(TAG, "No sessions to upload")
                return Result.success()
            }
            Logger.log(TAG, "UPLOAD_SCAN: sessions=${allSessions.size}")

            for (session in allSessions) {
                Logger.log(
                    TAG,
                    "SESSION_UPLOAD_START: sessionId=${session.sessionId} viewId=${session.viewId} status=${session.status}"
                )
                while (true) {
                    val events = eventStore.loadSessionEvents(session.sessionId, MAX_BATCH_SIZE)
                    if (events.isEmpty()) {
                        if (session.status == SessionStatus.COMPLETED) {
                            eventStore.deleteSessionIfEmpty(session.sessionId)
                        }
                        Logger.log(
                            TAG,
                            "SESSION_UPLOAD_DONE: sessionId=${session.sessionId} noMoreEvents=true"
                        )
                        break
                    }

                    val request = EventRequest(
                        metadata = Metadata(transmission_timestamp = System.currentTimeMillis()),
                        events = events.map { it.second }
                    )
                    Logger.log(
                        TAG,
                        "NETWORK_REQUEST_START: sessionId=${session.sessionId} batchSize=${events.size}"
                    )

                    val response = apiService.sendEvents(request)
                    Logger.log(
                        TAG,
                        "NETWORK_REQUEST_COMPLETE: sessionId=${session.sessionId} code=${response.code()} success=${response.isSuccessful}"
                    )
                    if (!response.isSuccessful) {
                        Logger.logWarning(
                            TAG,
                            "Upload failed for session=${session.sessionId}, viewId=${session.viewId}, code=${response.code()}"
                        )
                        return Result.retry()
                    }

                    eventStore.deleteUploadedEvents(events.map { it.first })
                    if (session.status == SessionStatus.COMPLETED) {
                        eventStore.deleteSessionIfEmpty(session.sessionId)
                    }
                }
            }

            Logger.log(TAG, "WORK_COMPLETE: workId=$id result=success")
            Result.success()
        } catch (ce: CancellationException) {
            Logger.logWarning(TAG, "WORK_CANCELLED: workId=$id reason=${ce.message}")
            throw ce
        } catch (e: Exception) {
            Logger.logError(TAG, "Upload worker failed: ${e.message}", e)
            Result.retry()
        }
    }

    private fun ensureContainerReady() {
        try {
            DependencyContainer.getContext()
        } catch (_: IllegalStateException) {
            DependencyContainer.initialize(applicationContext)
        }

        if (SDKBuildConfig.SDK_URL.isBlank()) {
            val savedSdkUrl = DependencyContainer.getViewerPref()?.getSdkUrl()
            if (!savedSdkUrl.isNullOrBlank()) {
                SDKBuildConfig.SDK_URL = savedSdkUrl
            }
        }
    }

}
