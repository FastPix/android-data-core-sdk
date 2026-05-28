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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

object EventJsonCodec {
    private const val TAG = "EventJsonCodec"

    private class Binding(
        val eventName: String,
        val klass: KClass<out BaseEvent>,
        val serializer: KSerializer<out BaseEvent>
    )

    private val bindings: List<Binding> = listOf(
        Binding("play", PlayEvent::class, PlayEvent.serializer()),
        Binding("playing", PlayingEvent::class, PlayingEvent.serializer()),
        Binding("pause", PauseEvent::class, PauseEvent.serializer()),
        Binding("ended", EndedEvent::class, EndedEvent.serializer()),
        Binding("pulse", PulseEvent::class, PulseEvent.serializer()),
        Binding("seeked", SeekedEvent::class, SeekedEvent.serializer()),
        Binding("seeking", SeekingEvent::class, SeekingEvent.serializer()),
        Binding("buffered", BufferedEvent::class, BufferedEvent.serializer()),
        Binding("buffering", BufferingEvent::class, BufferingEvent.serializer()),
        Binding("error", ErrorEvent::class, ErrorEvent.serializer()),
        Binding("playerReady", PlayerReadyEvent::class, PlayerReadyEvent.serializer()),
        Binding("viewBegin", ViewBeginEvent::class, ViewBeginEvent.serializer()),
        Binding("viewCompleted", ViewCompletedEvent::class, ViewCompletedEvent.serializer()),
        Binding("variantChanged", VariantChangedEvent::class, VariantChangedEvent.serializer()),
        Binding("requestCompleted", RequestCompletedEvent::class, RequestCompletedEvent.serializer()),
        Binding("requestFailed", RequestFailedEvent::class, RequestFailedEvent.serializer()),
        Binding("requestCanceled", RequestCancelledEvent::class, RequestCancelledEvent.serializer()),
    )

    private val byClass: Map<KClass<out BaseEvent>, KSerializer<out BaseEvent>> =
        bindings.associate { it.klass to it.serializer }

    private val byName: Map<String, KSerializer<out BaseEvent>> =
        bindings.associate { it.eventName to it.serializer }

    fun serialize(event: BaseEvent): String? {
        return try {
            @Suppress("UNCHECKED_CAST")
            val serializer = byClass[event::class] as? KSerializer<BaseEvent> ?: return null
            JsonSerializer.json.encodeToString(serializer, event)
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
            val serializer = eventName?.let { byName[it] }
            if (serializer == null) {
                Logger.logWarning(TAG, "Unknown event type: $eventName")
                null
            } else {
                JsonSerializer.json.decodeFromString(serializer, jsonString)
            }
        } catch (e: Exception) {
            Logger.logError(TAG, "Failed to deserialize event", e)
            null
        }
    }
}
