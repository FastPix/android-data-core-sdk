package io.fastpix.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.fastpix.data.utils.Logger
import java.util.concurrent.TimeUnit

object EventUploadScheduler {
    private const val TAG = "EventUploadScheduler"
    private const val UNIQUE_UPLOAD_WORK_NAME = "fastpix_event_upload"
    private const val SCHEDULER_PREFS = "fastpix_event_upload_scheduler"
    private const val KEY_ENQUEUED_ONLY_FIRST_SEEN_MS = "enqueued_only_first_seen_ms"
    private const val STALE_ENQUEUED_TIMEOUT_MS = 2 * 60 * 1000L

    fun schedule(context: Context) {
        val defaultPolicy = ExistingWorkPolicy.APPEND
        var effectivePolicy = defaultPolicy
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<EventUploadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(context.applicationContext)
        val prefs = context.applicationContext.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)
        try {
            val existing = workManager.getWorkInfosForUniqueWork(UNIQUE_UPLOAD_WORK_NAME).get()
            val running = existing.count { it.state == androidx.work.WorkInfo.State.RUNNING }
            val enqueued = existing.count { it.state == androidx.work.WorkInfo.State.ENQUEUED }
            val stateSummary = existing.joinToString(separator = " | ") {
                "id=${it.id},state=${it.state},attempt=${it.runAttemptCount}"
            }
            Logger.log(
                TAG,
                "WORK_ENQUEUE_REQUEST: newWorkId=${request.id} policy=$defaultPolicy existingRunning=$running existingEnqueued=$enqueued states=[$stateSummary]"
            )
            if (running + enqueued > 1) {
                Logger.logWarning(
                    TAG,
                    "WORK_OVERLAP_DETECTED: existingRunning=$running existingEnqueued=$enqueued"
                )
            }

            if (running == 0 && enqueued > 0) {
                val now = System.currentTimeMillis()
                val firstSeen = prefs.getLong(KEY_ENQUEUED_ONLY_FIRST_SEEN_MS, 0L)
                if (firstSeen == 0L) {
                    prefs.edit().putLong(KEY_ENQUEUED_ONLY_FIRST_SEEN_MS, now).apply()
                    Logger.log(
                        TAG,
                        "WORK_PENDING_TRACKING_STARTED: enqueuedOnly=true firstSeenMs=$now timeoutMs=$STALE_ENQUEUED_TIMEOUT_MS"
                    )
                } else {
                    val ageMs = now - firstSeen
                    if (ageMs >= STALE_ENQUEUED_TIMEOUT_MS) {
                        effectivePolicy = ExistingWorkPolicy.APPEND
                        prefs.edit().remove(KEY_ENQUEUED_ONLY_FIRST_SEEN_MS).apply()
                        Logger.logWarning(
                            TAG,
                            "WORK_STALE_ENQUEUED_DETECTED: ageMs=$ageMs timeoutMs=$STALE_ENQUEUED_TIMEOUT_MS action=REPLACE"
                        )
                    }
                }
            } else {
                prefs.edit().remove(KEY_ENQUEUED_ONLY_FIRST_SEEN_MS).apply()
            }
        } catch (e: Exception) {
            Logger.logWarning(TAG, "WORK_INSPECTION_FAILED: ${e.message}")
        }

        workManager.enqueueUniqueWork(
            UNIQUE_UPLOAD_WORK_NAME,
            effectivePolicy,
            request
        )
    }
}
