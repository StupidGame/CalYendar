package io.github.stupidgame.calyendar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvBackupCodecTest {

    @Test
    fun `rejects impossible dates before replacing saved data`() {
        val valid = CsvBackupCodec.encode(
            CsvBackupData(
                settings = AppSettings(),
                events = emptyList(),
                transactions = listOf(Transaction(year = 2026, month = 1, day = 28, type = TransactionType.EXPENSE, name = "test", amount = 1)),
                goals = emptyList()
            )
        )
        val invalid = valid.replace(",2026,2,28,", ",2026,2,30,")

        assertThrows(IllegalArgumentException::class.java) {
            CsvBackupCodec.decode(invalid)
        }
    }

    @Test
    fun `round trips backup data with commas quotes and newlines`() {
        val original =
            CsvBackupData(
                settings =
                    AppSettings(
                        webCalUrl = "webcal://example.com/feed?name=team,calendar",
                        notificationOneDayBefore = true,
                        notificationOneHourBefore = true
                    ),
                events =
                    listOf(
                        Event(
                            id = 42L,
                            year = 2026,
                            month = 2,
                            day = 19,
                            title = "Team, Sync \"Night\"",
                            startTime = 1_742_345_600_000L,
                            endTime = 1_742_349_200_000L,
                            notificationMinutesBefore = -1L,
                            isHoliday = false,
                            seriesId = "series-1",
                            notifications = "60,1440"
                        )
                    ),
                transactions =
                    listOf(
                        Transaction(
                            id = 7,
                            year = 2026,
                            month = 2,
                            day = 18,
                            type = TransactionType.EXPENSE,
                            name = "Groceries",
                            amount = 12_345L,
                            details = "milk, bread\nand fruit"
                        )
                    ),
                goals =
                    listOf(
                        FinancialGoal(
                            id = 3,
                            year = 2026,
                            month = 2,
                            day = 25,
                            name = "Weekend trip",
                            amount = 50_000L
                        )
                    )
            )

        val csv = CsvBackupCodec.encode(original)
        val restored = CsvBackupCodec.decode(csv)

        assertEquals(original, restored)
    }
}
