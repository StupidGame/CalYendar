package io.github.stupidgame.calyendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import biweekly.component.VEvent

@Entity(tableName = "imported_events")
@TypeConverters(VEventConverter::class)
data class ImportedEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val event: VEvent,
    val isHoliday: Boolean = false
)
