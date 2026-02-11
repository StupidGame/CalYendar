package io.github.stupidgame.CalYendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent

@Database(entities = [Transaction::class, Event::class, FinancialGoal::class, ImportedEvent::class], version = 4, exportSchema = false)
@TypeConverters(VEventConverter::class)
abstract class CalYendarDatabase : RoomDatabase() {
    abstract fun calyendarDao(): CalYendarDao

    companion object {
        @Volatile
        private var INSTANCE: CalYendarDatabase? = null

        fun getDatabase(context: Context): CalYendarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalYendarDatabase::class.java,
                    "calyendar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object VEventConverter {
    @TypeConverter
    @JvmStatic
    fun fromVEvent(event: VEvent?): String? {
        return event?.let {
            val ical = ICalendar()
            ical.addEvent(it)
            Biweekly.write(ical).go()
        }
    }

    @TypeConverter
    @JvmStatic
    fun toVEvent(eventString: String?): VEvent? {
        return eventString?.let { Biweekly.parse(it).first().events.firstOrNull() }
    }
}
