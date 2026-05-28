package io.fastpix.data.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analytics_events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = [EventEntity.COLUMN_SESSION_ID],
            childColumns = [EventEntity.COLUMN_SESSION_ID],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = [EventEntity.COLUMN_SESSION_ID]), Index(value = ["createdAt"])]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: String,
    val eventData: String,
    val createdAt: Long
) {
    companion object {
        const val COLUMN_SESSION_ID = "sessionId"
    }
}
