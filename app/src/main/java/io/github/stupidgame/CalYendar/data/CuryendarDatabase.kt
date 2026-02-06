package io.github.stupidgame.CalYendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class, Event::class, FinancialGoal::class], version = 3, exportSchema = false)
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
