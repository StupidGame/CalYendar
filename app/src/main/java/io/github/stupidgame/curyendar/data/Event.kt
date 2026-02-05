package io.github.stupidgame.curyendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val year: Int,
    val month: Int,
    val day: Int,
    val title: String,
    val startTime: Long, // Store as epoch millis
    val endTime: Long, // Store as epoch millis
    val notificationMinutesBefore: Long // -1 for no notification
)
