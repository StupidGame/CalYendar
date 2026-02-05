package io.github.stupidgame.curyendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class, Event::class], version = 2, exportSchema = false)
abstract class CuryendarDatabase : RoomDatabase() {
    abstract fun curyendarDao(): CuryendarDao

    companion object {
        @Volatile
        private var INSTANCE: CuryendarDatabase? = null

        fun getDatabase(context: Context): CuryendarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CuryendarDatabase::class.java,
                    "curyendar_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
