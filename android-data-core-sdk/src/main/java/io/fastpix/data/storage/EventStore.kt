package io.fastpix.data.storage

import androidx.room.withTransaction
import io.fastpix.data.domain.model.events.BaseEvent
import io.fastpix.data.pref.EventPersistenceManager
import io.fastpix.data.utils.Logger
import java.util.UUID

class EventStore(
    private val database: AnalyticsDatabase,
    private val legacyPersistenceManager: EventPersistenceManager
) {
    companion object {
        private const val TAG = "EventStore"
    }

    private val sessionDao = database.sessionDao()
    private val eventDao = database.eventDao()

    suspend fun onSdkInitialized() {
        database.withTransaction {
            sessionDao.updateStatus(SessionStatus.ACTIVE, SessionStatus.COMPLETED)
        }
        migrateLegacyEventsIfNeeded()
    }

    suspend fun onNewEvent(event: BaseEvent): Boolean {
        val viewId = event.viewId?.takeIf { it.isNotBlank() } ?: run {
            Logger.logWarning(TAG, "BUFFER_SKIP: missing viewId event=${event.eventName}")
            return false
        }
        val payload = EventJsonCodec.serialize(event) ?: run {
            Logger.logWarning(TAG, "BUFFER_SKIP: serialize failed event=${event.eventName}")
            return false
        }
        val now = System.currentTimeMillis()
        var createdNewSession = false

        database.withTransaction {
            val activeSession = sessionDao.getSessionByStatusAndViewId(SessionStatus.ACTIVE, viewId)
            val sessionId = if (activeSession == null) {
                sessionDao.updateStatus(SessionStatus.ACTIVE, SessionStatus.COMPLETED)
                val newSessionId = UUID.randomUUID().toString()
                createdNewSession = true
                sessionDao.upsert(
                    SessionEntity(
                        sessionId = newSessionId,
                        viewId = viewId,
                        status = SessionStatus.ACTIVE,
                        createdAt = now
                    )
                )
                newSessionId
            } else {
                activeSession.sessionId
            }

            eventDao.insert(
                EventEntity(
                    sessionId = sessionId,
                    eventData = payload,
                    createdAt = now
                )
            )
            Logger.log(
                TAG,
                "BUFFERED_EVENT: event=${event.eventName} dbSessionId=$sessionId createdNewSession=$createdNewSession"
            )
        }
        return true
    }

    suspend fun markActiveSessionsCompleted() {
        database.withTransaction {
            sessionDao.updateStatus(SessionStatus.ACTIVE, SessionStatus.COMPLETED)
        }
        Logger.log(TAG, "SESSIONS_MARKED_COMPLETED")
    }

    suspend fun getCompletedSessions(): List<SessionEntity> {
        return sessionDao.getSessionsByStatus(SessionStatus.COMPLETED).also {
            Logger.log(TAG, "COMPLETED_SESSIONS_FETCHED: count=${it.size}")
        }
    }

    suspend fun getAllSessions(): List<SessionEntity> {
        return sessionDao.getAllSessions()
    }

    suspend fun loadSessionEvents(sessionId: String, maxBatchSize: Int): List<Pair<Long, BaseEvent>> {
        val entities = eventDao.getEventsBySessionId(sessionId, maxBatchSize)
        return entities.mapNotNull { entity ->
            EventJsonCodec.deserialize(entity.eventData)?.let { event ->
                entity.id to event
            }
        }
    }

    suspend fun deleteUploadedEvents(eventIds: List<Long>) {
        if (eventIds.isNotEmpty()) {
            eventDao.deleteByIds(eventIds)
            Logger.log(TAG, "UPLOADED_EVENTS_DELETED: count=${eventIds.size}")
        }
    }

    suspend fun deleteSessionIfEmpty(sessionId: String) {
        val remaining = eventDao.countBySessionId(sessionId)
        if (remaining == 0) {
            sessionDao.deleteBySessionId(sessionId)
            Logger.log(TAG, "SESSION_DELETED_EMPTY: sessionId=$sessionId")
        } else {
            Logger.log(TAG, "SESSION_RETAINED: sessionId=$sessionId remainingEvents=$remaining")
        }
    }

    private suspend fun migrateLegacyEventsIfNeeded() {
        if (!legacyPersistenceManager.hasPendingEvents()) return

        val legacyEvents = legacyPersistenceManager.loadPendingEvents()
        if (legacyEvents.isEmpty()) {
            legacyPersistenceManager.clearPendingEvents()
            return
        }

        val now = System.currentTimeMillis()

        database.withTransaction {
            legacyEvents
                .groupBy { it.viewId?.takeIf { viewId -> viewId.isNotBlank() } ?: "legacy" }
                .forEach { (viewId, events) ->
                    val migrationSessionId = UUID.randomUUID().toString()
                    sessionDao.upsert(
                        SessionEntity(
                            sessionId = migrationSessionId,
                            viewId = viewId,
                            status = SessionStatus.COMPLETED,
                            createdAt = now
                        )
                    )
                    events.forEach { event ->
                        val payload = EventJsonCodec.serialize(event) ?: return@forEach
                        eventDao.insert(
                            EventEntity(
                                sessionId = migrationSessionId,
                                eventData = payload,
                                createdAt = now
                            )
                        )
                    }
                }
        }
        legacyPersistenceManager.clearPendingEvents()
        Logger.log(TAG, "Migrated ${legacyEvents.size} legacy events into Room")
    }
}
