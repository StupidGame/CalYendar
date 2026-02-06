package io.github.stupidgame.calyendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class, Event::class, FinancialGoal::class], version = 3, exportSchema = false)
abstract class calyendarDatabase : RoomDatabase() {
    abstract fun calyendarDao(): calyendarDao

    companion object {
        @Volatile
        private var INSTANCE: calyendarDatabase? = null

        fun getDatabase(context: Context): calyendarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    calyendarDatabase::class.java,
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
