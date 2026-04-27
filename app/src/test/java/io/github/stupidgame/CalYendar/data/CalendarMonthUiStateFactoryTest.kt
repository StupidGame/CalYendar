package io.github.stupidgame.calyendar.data

import biweekly.component.VEvent
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMonthUiStateFactoryTest {

    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `builds day states with grouped holidays and running balances`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 2),
                transactionsUpToToday =
                    listOf(
                        transaction(LocalDate.of(2026, 2, 28), TransactionType.INCOME, 50),
                        transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 100),
                        transaction(LocalDate.of(2026, 3, 2), TransactionType.EXPENSE, 20)
                    ),
                transactionsBeforeMonth =
                    listOf(transaction(LocalDate.of(2026, 2, 28), TransactionType.INCOME, 50)),
                monthTransactions =
                    listOf(
                        transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 100),
                        transaction(LocalDate.of(2026, 3, 2), TransactionType.EXPENSE, 20),
                        transaction(LocalDate.of(2026, 3, 3), TransactionType.EXPENSE, 10)
                    ),
                monthEvents = listOf(event(LocalDate.of(2026, 3, 2), isHoliday = true)),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 2, 20), amount = 30),
                        goal(LocalDate.of(2026, 3, 4), amount = 60)
                    ),
                importedEvents = listOf(importedEvent(LocalDate.of(2026, 3, 1), isHoliday = true))
            )

        val state = CalendarMonthUiStateFactory.create(input)

        assertEquals(130L, state.todayBalance)
        assertEquals(70L, state.todayAvailableBalance)
        assertEquals(120L, state.currentBalance)
        assertEquals(90L, state.goalComparisonBalance)
        assertEquals(150L, state.dayStates.getValue(1).balance)
        assertEquals(130L, state.dayStates.getValue(2).balance)
        assertEquals(120L, state.dayStates.getValue(3).balance)
        assertTrue(state.dayStates.getValue(1).isHoliday)
        assertTrue(state.dayStates.getValue(2).isHoliday)
        assertEquals("goal-4", state.dayStates.getValue(2).goal?.name)
        assertEquals(70L, state.dayStates.getValue(2).predictionDiff)
        assertNull(state.dayStates.getValue(1).predictionDiff)
        assertEquals(1, state.activeMonthGoals.size)
    }

    @Test
    fun `builds current month spendable balance by reserving the next goal`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 18),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 100)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 100)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 10), amount = 30),
                        goal(LocalDate.of(2026, 4, 5), amount = 40)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)

        assertEquals(100L, state.todayBalance)
        assertEquals(60L, state.todayAvailableBalance)
        assertEquals(100L, state.currentBalance)
        assertEquals(60L, state.availableMoneyAfterMonthGoals)
        assertTrue(state.activeMonthGoals.isEmpty())
    }

    @Test
    fun `subtracts completed goals in current month before comparing active month goals`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 18),
                transactionsUpToToday =
                    listOf(
                        transaction(LocalDate.of(2026, 2, 28), TransactionType.INCOME, 50),
                        transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 100)
                    ),
                transactionsBeforeMonth =
                    listOf(transaction(LocalDate.of(2026, 2, 28), TransactionType.INCOME, 50)),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 100)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 2, 20), amount = 30),
                        goal(LocalDate.of(2026, 3, 10), amount = 20),
                        goal(LocalDate.of(2026, 3, 25), amount = 60)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)

        assertEquals(150L, state.todayBalance)
        assertEquals(90L, state.todayAvailableBalance)
        assertEquals(150L, state.currentBalance)
        assertEquals(100L, state.goalComparisonBalance)
        assertEquals(1, state.activeMonthGoals.size)
    }

    @Test
    fun `uses the goal immediately after completed month goals for spendable balance`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 28),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 200)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 200)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 10), amount = 30),
                        goal(LocalDate.of(2026, 3, 20), amount = 50),
                        goal(LocalDate.of(2026, 4, 5), amount = 80)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)

        assertEquals(200L, state.currentBalance)
        assertEquals(120L, state.todayAvailableBalance)
        assertEquals(120L, state.availableMoneyAfterMonthGoals)
        assertTrue(state.activeMonthGoals.isEmpty())
    }

    @Test
    fun `cell surplus after completed month goals reserves only the next goal`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 28),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 200)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 200)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 10), amount = 30),
                        goal(LocalDate.of(2026, 3, 20), amount = 50),
                        goal(LocalDate.of(2026, 4, 5), amount = 80)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)
        val todayCell = state.dayStates.getValue(28)

        assertEquals(200L, todayCell.balance)
        assertEquals(120L, todayCell.predictionDiff)
        assertEquals("goal-5", todayCell.goal?.name)
        assertEquals(80L, todayCell.goal?.amount)
    }

    private fun transaction(
        date: LocalDate,
        type: TransactionType,
        amount: Long
    ): Transaction =
        Transaction(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            type = type,
            name = "$type-$amount",
            amount = amount
        )

    private fun event(date: LocalDate, isHoliday: Boolean): Event =
        Event(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            title = "event-${date.dayOfMonth}",
            startTime = date.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
            endTime = date.atTime(11, 0).atZone(zoneId).toInstant().toEpochMilli(),
            notificationMinutesBefore = -1L,
            isHoliday = isHoliday
        )

    private fun goal(date: LocalDate, amount: Long): FinancialGoal =
        FinancialGoal(
            year = date.year,
            month = date.monthValue - 1,
            day = date.dayOfMonth,
            name = "goal-${date.dayOfMonth}",
            amount = amount
        )

    private fun importedEvent(date: LocalDate, isHoliday: Boolean): ImportedEvent {
        val vEvent =
            VEvent().apply {
                setSummary("imported-${date.dayOfMonth}")
                setDateStart(Date.from(date.atTime(12, 0).atZone(zoneId).toInstant()))
            }

        return ImportedEvent(event = vEvent, isHoliday = isHoliday)
    }
}
