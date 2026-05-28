package io.fastpix.data.storage

import io.fastpix.data.domain.model.events.BaseEvent
import io.fastpix.data.domain.model.events.BufferedEvent
import io.fastpix.data.domain.model.events.BufferingEvent
import io.fastpix.data.domain.model.events.EndedEvent
import io.fastpix.data.domain.model.events.ErrorEvent
import io.fastpix.data.domain.model.events.PauseEvent
import io.fastpix.data.domain.model.events.PlayEvent
import io.fastpix.data.domain.model.events.PlayerReadyEvent
import io.fastpix.data.domain.model.events.PlayingEvent
import io.fastpix.data.domain.model.events.PulseEvent
import io.fastpix.data.domain.model.events.RequestCancelledEvent
import io.fastpix.data.domain.model.events.RequestCompletedEvent
import io.fastpix.data.domain.model.events.RequestFailedEvent
import io.fastpix.data.domain.model.events.SeekedEvent
import io.fastpix.data.domain.model.events.SeekingEvent
import io.fastpix.data.domain.model.events.VariantChangedEvent
import io.fastpix.data.domain.model.events.ViewBeginEvent
import io.fastpix.data.domain.model.events.ViewCompletedEvent
import io.fastpix.data.utils.JsonSerializer
import io.fastpix.data.utils.Logger
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object EventJsonCodec {
    private const val TAG = "EventJsonCodec"

    fun serialize(event: BaseEvent): String? {
        return try {
            when (event) {
                is PlayEvent -> JsonSerializer.json.encodeToString(PlayEvent.serializer(), event)
                is PlayingEvent -> JsonSerializer.json.encodeToString(PlayingEvent.serializer(), event)
                is PauseEvent -> JsonSerializer.json.encodeToString(PauseEvent.serializer(), event)
                is EndedEvent -> JsonSerializer.json.encodeToString(EndedEvent.serializer(), event)
                is PulseEvent -> JsonSerializer.json.encodeToString(PulseEvent.serializer(), event)
                is SeekedEvent -> JsonSerializer.json.encodeToString(SeekedEvent.serializer(), event)
                is SeekingEvent -> JsonSerializer.json.encodeToString(SeekingEvent.serializer(), event)
                is BufferedEvent -> JsonSerializer.json.encodeToString(BufferedEvent.serializer(), event)
                is BufferingEvent -> JsonSerializer.json.encodeToString(BufferingEvent.serializer(), event)
                is ErrorEvent -> JsonSerializer.json.encodeToString(ErrorEvent.serializer(), event)
                is PlayerReadyEvent -> JsonSerializer.json.encodeToString(
                    PlayerReadyEvent.serializer(),
                    event
                )
                is ViewBeginEvent -> JsonSerializer.json.encodeToString(ViewBeginEvent.serializer(), event)
                is ViewCompletedEvent -> JsonSerializer.json.encodeToString(
                    ViewCompletedEvent.serializer(),
                    event
                )
                is VariantChangedEvent -> JsonSerializer.json.encodeToString(
                    VariantChangedEvent.serializer(),
                    event
                )
                is RequestCompletedEvent -> JsonSerializer.json.encodeToString(
                    RequestCompletedEvent.serializer(),
                    event
                )
                is RequestFailedEvent -> JsonSerializer.json.encodeToString(
                    RequestFailedEvent.serializer(),
                    event
                )
                is RequestCancelledEvent -> JsonSerializer.json.encodeToString(
                    RequestCancelledEvent.serializer(),
                    event
                )
                else -> null
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to serialize event", e)
            null
        }
    }

    fun deserialize(jsonString: String): BaseEvent? {
        return try {
            val jsonObject = JsonSerializer.json.parseToJsonElement(jsonString).jsonObject
            val eventName = jsonObject["eventName"]?.jsonPrimitive?.content
                ?: jsonObject["evna"]?.jsonPrimitive?.content

            when (eventName) {
                "play" -> JsonSerializer.json.decodeFromString<PlayEvent>(jsonString)
                "playing" -> JsonSerializer.json.decodeFromString<PlayingEvent>(jsonString)
                "pause" -> JsonSerializer.json.decodeFromString<PauseEvent>(jsonString)
                "ended" -> JsonSerializer.json.decodeFromString<EndedEvent>(jsonString)
                "pulse" -> JsonSerializer.json.decodeFromString<PulseEvent>(jsonString)
                "seeked" -> JsonSerializer.json.decodeFromString<SeekedEvent>(jsonString)
                "seeking" -> JsonSerializer.json.decodeFromString<SeekingEvent>(jsonString)
                "buffered" -> JsonSerializer.json.decodeFromString<BufferedEvent>(jsonString)
                "buffering" -> JsonSerializer.json.decodeFromString<BufferingEvent>(jsonString)
                "error" -> JsonSerializer.json.decodeFromString<ErrorEvent>(jsonString)
                "playerReady" -> JsonSerializer.json.decodeFromString<PlayerReadyEvent>(jsonString)
                "viewBegin" -> JsonSerializer.json.decodeFromString<ViewBeginEvent>(jsonString)
                "viewCompleted" -> JsonSerializer.json.decodeFromString<ViewCompletedEvent>(jsonString)
                "variantChanged" -> JsonSerializer.json.decodeFromString<VariantChangedEvent>(
                    jsonString
                )
                "requestCompleted" -> JsonSerializer.json.decodeFromString<RequestCompletedEvent>(
                    jsonString
                )
                "requestFailed" -> JsonSerializer.json.decodeFromString<RequestFailedEvent>(
                    jsonString
                )
                "requestCanceled" -> JsonSerializer.json.decodeFromString<RequestCancelledEvent>(
                    jsonString
                )
                else -> {
                    Logger.logWarning(TAG, "Unknown event type: $eventName")
                    null
                }
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to deserialize event", e)
            null
        }
    }
}
