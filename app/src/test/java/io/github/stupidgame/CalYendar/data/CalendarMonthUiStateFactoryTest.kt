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

        assertEquals(100L, state.todayBalance)
        assertEquals(40L, state.todayAvailableBalance)
        assertEquals(90L, state.currentBalance)
        assertEquals(90L, state.goalComparisonBalance)
        assertEquals(120L, state.dayStates.getValue(1).balance)
        assertEquals(100L, state.dayStates.getValue(2).balance)
        assertEquals(90L, state.dayStates.getValue(3).balance)
        assertTrue(state.dayStates.getValue(1).isHoliday)
        assertTrue(state.dayStates.getValue(2).isHoliday)
        assertEquals("goal-4", state.dayStates.getValue(2).goal?.name)
        assertEquals(40L, state.dayStates.getValue(2).predictionDiff)
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

        assertEquals(70L, state.todayBalance)
        assertEquals(30L, state.todayAvailableBalance)
        assertEquals(70L, state.currentBalance)
        assertEquals(30L, state.availableMoneyAfterMonthGoals)
        assertTrue(state.activeMonthGoals.isEmpty())
    }

    @Test
    fun `subtracts paid goals before comparing active month goals`() {
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

        assertEquals(100L, state.todayBalance)
        assertEquals(40L, state.todayAvailableBalance)
        assertEquals(100L, state.currentBalance)
        assertEquals(100L, state.goalComparisonBalance)
        assertEquals(1, state.activeMonthGoals.size)
    }

    @Test
    fun `keeps balances at zero when paid goals exceed income`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 18),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 50)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 50)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 10), amount = 80),
                        goal(LocalDate.of(2026, 3, 25), amount = 60)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)
        val todayCell = state.dayStates.getValue(18)

        assertEquals(0L, state.todayBalance)
        assertEquals(-60L, state.todayAvailableBalance)
        assertEquals(0L, state.currentBalance)
        assertEquals(0L, state.goalComparisonBalance)
        assertEquals(0L, todayCell.balance)
        assertEquals(-60L, todayCell.predictionDiff)
    }

    @Test
    fun `treats a goal on today as completed for month and day calculations`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 15),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 5_000)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 5_000)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)
        val todayCell = state.dayStates.getValue(15)

        assertEquals(4_000L, state.todayBalance)
        assertEquals(1_000L, state.todayAvailableBalance)
        assertEquals(4_000L, state.currentBalance)
        assertTrue(state.activeMonthGoals.isEmpty())
        assertEquals("goal-5", todayCell.goal?.name)
        assertEquals(4_000L, todayCell.balance)
        assertEquals(1_000L, todayCell.predictionDiff)
    }

    @Test
    fun `day cell sums goals on the same upcoming date`() {
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
                        goal(LocalDate.of(2026, 3, 25), amount = 30),
                        goal(LocalDate.of(2026, 3, 25), amount = 20)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)
        val todayCell = state.dayStates.getValue(18)

        assertEquals("goal-25", todayCell.goal?.name)
        assertEquals(50L, todayCell.goalTargetAmount)
        assertEquals(50L, todayCell.predictionDiff)
        assertEquals(50L, state.todayAvailableBalance)
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

        assertEquals(120L, state.currentBalance)
        assertEquals(40L, state.todayAvailableBalance)
        assertEquals(40L, state.availableMoneyAfterMonthGoals)
        assertTrue(state.activeMonthGoals.isEmpty())
    }

    @Test
    fun `cell display subtracts paid goals and uses the next goal amount`() {
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

        assertEquals(120L, todayCell.balance)
        assertEquals(40L, todayCell.predictionDiff)
        assertEquals("goal-5", todayCell.goal?.name)
        assertEquals(80L, todayCell.goal?.amount)
        assertEquals(80L, todayCell.goalTargetAmount)
    }

    @Test
    fun `cell display subtracts earlier future goals before comparing later goals`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 3, 1),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 5_000)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 5_000)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 15), amount = 1_000),
                        goal(LocalDate.of(2026, 4, 5), amount = 3_000)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)
        val beforeFirstGoalCell = state.dayStates.getValue(14)
        val firstGoalDateCell = state.dayStates.getValue(15)
        val afterFirstGoalCell = state.dayStates.getValue(16)

        assertEquals("goal-15", beforeFirstGoalCell.goal?.name)
        assertEquals(1_000L, beforeFirstGoalCell.goalTargetAmount)
        assertEquals(4_000L, beforeFirstGoalCell.predictionDiff)
        assertEquals(4_000L, firstGoalDateCell.balance)
        assertEquals("goal-5", firstGoalDateCell.goal?.name)
        assertEquals(3_000L, firstGoalDateCell.goalTargetAmount)
        assertEquals(1_000L, firstGoalDateCell.predictionDiff)
        assertEquals("goal-5", afterFirstGoalCell.goal?.name)
        assertEquals(3_000L, afterFirstGoalCell.goalTargetAmount)
        assertEquals(1_000L, afterFirstGoalCell.predictionDiff)
    }

    @Test
    fun `subtracts a completed goal on the last day of a past month`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 2,
                today = LocalDate.of(2026, 4, 5),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 500)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 3, 1), TransactionType.INCOME, 500)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 31), amount = 200),
                        goal(LocalDate.of(2026, 4, 10), amount = 100)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)

        assertEquals(300L, state.currentBalance)
        assertEquals(200L, state.availableMoneyAfterMonthGoals)
        assertTrue(state.activeMonthGoals.isEmpty())
    }

    @Test
    fun `cell display subtracts paid goals across month boundaries`() {
        val input =
            CalendarMonthUiStateInput(
                year = 2026,
                month = 3,
                today = LocalDate.of(2026, 4, 5),
                transactionsUpToToday =
                    listOf(transaction(LocalDate.of(2026, 4, 1), TransactionType.INCOME, 250)),
                transactionsBeforeMonth = emptyList(),
                monthTransactions =
                    listOf(transaction(LocalDate.of(2026, 4, 1), TransactionType.INCOME, 250)),
                monthEvents = emptyList(),
                allGoals =
                    listOf(
                        goal(LocalDate.of(2026, 3, 28), amount = 120),
                        goal(LocalDate.of(2026, 4, 10), amount = 200)
                    ),
                importedEvents = emptyList()
            )

        val state = CalendarMonthUiStateFactory.create(input)
        val dayCell = state.dayStates.getValue(5)

        assertEquals("goal-10", dayCell.goal?.name)
        assertEquals(-70L, dayCell.predictionDiff)
        assertEquals(200L, dayCell.goalTargetAmount)
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
