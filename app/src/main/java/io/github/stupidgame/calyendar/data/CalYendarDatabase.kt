package io.github.stupidgame.calyendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import biweekly.Biweekly
import biweekly.ICalendar
import biweekly.component.VEvent
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Transaction::class, Event::class, FinancialGoal::class, ImportedEvent::class], version = 7, exportSchema = false)
@TypeConverters(VEventConverter::class)
abstract class CalYendarDatabase : RoomDatabase() {
    abstract fun calyendarDao(): CalYendarDao

    companion object {
        @Volatile
        private var INSTANCE: CalYendarDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE events ADD COLUMN seriesId TEXT")
                } catch (e: Exception) { e.printStackTrace() }
                try {
                    database.execSQL("ALTER TABLE events ADD COLUMN notifications TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { e.printStackTrace() }
                try {
                    database.execSQL("UPDATE events SET notifications = CAST(notificationMinutesBefore AS TEXT) WHERE notificationMinutesBefore != -1")
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recover from potentially botched 5 -> 6 migration
                try {
                    database.execSQL("ALTER TABLE events ADD COLUMN seriesId TEXT")
                } catch (e: Exception) { e.printStackTrace() }
                try {
                    database.execSQL("ALTER TABLE events ADD COLUMN notifications TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) { e.printStackTrace() }
                try {
                    database.execSQL("UPDATE events SET notifications = CAST(notificationMinutesBefore AS TEXT) WHERE notificationMinutesBefore != -1 AND notifications = ''")
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        fun getDatabase(context: Context): CalYendarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CalYendarDatabase::class.java,
                    "calyendar_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
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
