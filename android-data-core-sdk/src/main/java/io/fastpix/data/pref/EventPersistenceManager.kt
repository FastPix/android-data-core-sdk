package io.fastpix.data.pref

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import io.fastpix.data.domain.model.events.BaseEvent
import io.fastpix.data.storage.EventJsonCodec
import io.fastpix.data.utils.JsonSerializer
import io.fastpix.data.utils.Logger
import kotlinx.serialization.json.jsonArray

/**
 * Manages persistent storage of unsent events
 * Similar to Mux Data SDK's approach of saving events for replay
 * Uses Kotlinx Serialization for type-safe JSON serialization/deserialization
 */
class EventPersistenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fastpix_event_storage", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "EventPersistenceManager"
        private const val KEY_PENDING_EVENTS = "pending_events"
        private const val KEY_EVENT_TIMESTAMP = "event_timestamp"
        private const val MAX_STORED_EVENTS = 500 // Prevent unbounded growth
        private const val MAX_EVENT_AGE_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    /**
     * Deserialize a single event from JSON string based on eventName field
     */
    private fun deserializeEvent(jsonString: String): BaseEvent? = EventJsonCodec.deserialize(jsonString)

    /**
     * Save events to persistent storage
     * Called when app is about to terminate and events haven't been sent
     *
     * @param synchronous If true, uses commit() for synchronous write (critical for app termination).
     *                    If false, uses apply() for async write (normal operation).
     */
    fun savePendingEvents(events: List<BaseEvent>, synchronous: Boolean = false) {
        if (events.isEmpty()) return

        try {
            val existingEvents = loadPendingEvents().toMutableList()

            // Add new events
            existingEvents.addAll(events)

            // Limit total events to prevent unbounded growth
            val eventsToSave = if (existingEvents.size > MAX_STORED_EVENTS) {
                existingEvents.takeLast(MAX_STORED_EVENTS)
            } else {
                existingEvents
            }

            // Convert to JSON and save using Kotlinx Serialization
            val jsonArray = eventsToSave.mapNotNull { EventJsonCodec.serialize(it) }
            val jsonString = "[${jsonArray.joinToString(",")}]"

            if (synchronous) {
                // Use commit() for synchronous write - blocks until write completes
                // Critical for app termination scenarios
                prefs.edit().apply {
                    putString(KEY_PENDING_EVENTS, jsonString)
                    putLong(KEY_EVENT_TIMESTAMP, System.currentTimeMillis())
                    commit() // Synchronous - ensures data is written before process can be killed
                }
            } else {
                // Use apply() for async write - normal operation
                prefs.edit {
                    putString(KEY_PENDING_EVENTS, jsonString)
                    putLong(KEY_EVENT_TIMESTAMP, System.currentTimeMillis())
                    apply()
                }
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to save pending events", e)
        }
    }

    /**
     * Load pending events from persistent storage
     * Called on app startup to replay unsent events
     */
    fun loadPendingEvents(): List<BaseEvent> {
        return try {
            val jsonString = prefs.getString(KEY_PENDING_EVENTS, null)
            if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                // Filter out old events (older than MAX_EVENT_AGE_MS)
                val timestamp = prefs.getLong(KEY_EVENT_TIMESTAMP, 0)
                val currentTime = System.currentTimeMillis()

                if (currentTime - timestamp > MAX_EVENT_AGE_MS) {
                    Logger.log(TAG, "Events are too old, clearing storage")
                    clearPendingEvents()
                    emptyList()
                } else {
                    // Parse JSON array and deserialize each event based on eventName
                    val jsonArray = JsonSerializer.json.parseToJsonElement(jsonString).jsonArray
                    val events = jsonArray.mapNotNull { element ->
                        deserializeEvent(element.toString())
                    }
                    events
                }
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to load pending events", e)
            emptyList()
        }
    }

    /**
     * Clear all pending events from storage
     * Called after successfully sending events
     */
    fun clearPendingEvents() {
        prefs.edit {
            clear()
        }
        Logger.log(TAG, "Cleared pending events from storage")
    }

    /**
     * Delete events by their viewerTimeStamp values
     * Only deletes events that match the provided viewerTimeStamp values
     * Called after successfully sending events to server
     *
     * @param viewerTimeStamps Set of viewerTimeStamp values to delete
     */
    fun deleteEventsByViewerTimeStamps(viewerTimeStamps: Set<Long?>) {
        if (viewerTimeStamps.isEmpty()) {
            Logger.log(TAG, "No viewerTimeStamps provided, nothing to delete")
            return
        }

        try {
            val allEvents = loadPendingEvents()
            if (allEvents.isEmpty()) {
                Logger.log(TAG, "No events to delete")
                return
            }

            // Filter out events that match the provided viewerTimeStamp values
            val remainingEvents = allEvents.filter { event ->
                val eventViewerTimeStamp = event.viewerTimeStamp
                !viewerTimeStamps.contains(eventViewerTimeStamp)
            }

            val deletedCount = allEvents.size - remainingEvents.size
            Logger.log(
                TAG,
                "Deleting $deletedCount events by viewerTimeStamp, keeping ${remainingEvents.size} events"
            )

            if (remainingEvents.isEmpty()) {
                // No events left, clear storage
                clearPendingEvents()
            } else {
                // Save remaining events back to storage
                val jsonArray = remainingEvents.mapNotNull { EventJsonCodec.serialize(it) }
                val jsonString = "[${jsonArray.joinToString(",")}]"

                prefs.edit {
                    putString(KEY_PENDING_EVENTS, jsonString)
                    putLong(KEY_EVENT_TIMESTAMP, System.currentTimeMillis())
                    commit()
                }
                Logger.log(
                    TAG,
                    "Deleted $deletedCount events by viewerTimeStamp, ${remainingEvents.size} events remaining"
                )
            }
        } catch (e: Exception) {
            Logger.logError(
                TAG,
                "Failed to delete events by viewerTimeStamp",
                e
            )
        }
    }

    /**
     * Check if there are pending events
     */
    fun hasPendingEvents(): Boolean {
        return prefs.getString(KEY_PENDING_EVENTS, null) != null
    }
}

