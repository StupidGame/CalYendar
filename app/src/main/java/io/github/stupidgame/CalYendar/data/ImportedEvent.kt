package io.github.stupidgame.CalYendar.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import biweekly.component.VEvent

@Entity(tableName = "imported_events")
data class ImportedEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val event: VEvent
)
