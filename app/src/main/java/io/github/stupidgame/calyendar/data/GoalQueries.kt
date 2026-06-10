package io.github.stupidgame.calyendar.data

import java.time.LocalDate

private val goalDateComparator =
    compareBy<FinancialGoal>({ it.year }, { it.month }, { it.day }, { it.id })

internal fun List<FinancialGoal>.sortedByDateThenId(): List<FinancialGoal> =
    sortedWith(goalDateComparator)

internal fun List<FinancialGoal>.firstOnDate(date: LocalDate): FinancialGoal? =
    sortedByDateThenId().firstOrNull { goal -> goal.toLocalDate() == date }

internal fun List<FinancialGoal>.allOnDate(date: LocalDate): List<FinancialGoal> =
    sortedByDateThenId().filter { goal -> goal.toLocalDate() == date }

internal fun List<FinancialGoal>.firstAfterDate(date: LocalDate): FinancialGoal? =
    sortedByDateThenId().firstOrNull { goal -> goal.toLocalDate().isAfter(date) }

internal fun List<FinancialGoal>.totalAmountOnDate(date: LocalDate): Long =
    filter { goal -> goal.toLocalDate() == date }.sumOf(FinancialGoal::amount)
