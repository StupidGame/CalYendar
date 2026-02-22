package io.github.stupidgame.calyendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val day: Int,
    val title: String,
    val startTime: Long, // Store as epoch millis
    val endTime: Long, // Store as epoch millis,
    val notificationMinutesBefore: Long, // -1 for no notification (Legacy, kept for migration if needed, or we can just leave it and add others. Wait, it's better to keep it for backwards compatibility if we don't drop the column, but actually we will migrate to notifications)
    val isHoliday: Boolean = false,
    val seriesId: String? = null,
    val notifications: String = "" // Comma-separated list of minutes. e.g., "30,1440"
)
