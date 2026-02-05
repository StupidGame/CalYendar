package io.github.stupidgame.curyendar

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.stupidgame.curyendar.data.AppDatabase
import io.github.stupidgame.curyendar.data.CuryendarDao
import io.github.stupidgame.curyendar.data.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var database: AppDatabase
    private lateinit var curyendarDao: CuryendarDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        curyendarDao = database.curyendarDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndGetExpense() = runBlocking {
        val expense = Expense(year = 2024, month = 5, day = 17, name = "Test Expense", amount = 100.0)
        curyendarDao.insert(expense)
        val expenses = curyendarDao.getItems(2024, 5, 17).first()
        assertEquals(1, expenses.size)
        assertEquals(expense, expenses[0])
    }
}
