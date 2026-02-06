package io.github.stupidgame.calyendar

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.stupidgame.calyendar.data.calyendarDatabase
import io.github.stupidgame.calyendar.data.calyendarDao
import io.github.stupidgame.calyendar.data.Transaction
import io.github.stupidgame.calyendar.data.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var database: calyendarDatabase
    private lateinit var calyendarDao: calyendarDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, calyendarDatabase::class.java).build()
        calyendarDao = database.calyendarDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndGetTransaction() = runBlocking {
        val transaction = Transaction(year = 2024, month = 5, day = 17, type = TransactionType.EXPENSE, name = "Test Expense", amount = 100L)
        calyendarDao.upsertTransaction(transaction)
        val transactions = calyendarDao.getTransactionsForDate(2024, 5, 17).first()
        assertEquals(1, transactions.size)
        assertEquals(transaction.copy(id=1), transactions[0])
    }
}
