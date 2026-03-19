package io.github.stupidgame.calyendar.data

import java.time.LocalDate
import java.time.ZoneId

fun Event.toLocalDate(): LocalDate = LocalDate.of(year, month + 1, day)

fun FinancialGoal.toLocalDate(): LocalDate = LocalDate.of(year, month + 1, day)

fun ImportedEvent.toStartLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate? =
    event.dateStart?.value?.toInstant()?.atZone(zoneId)?.toLocalDate()

fun List<ImportedEvent>.filterByStartLocalDate(
    date: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): List<ImportedEvent> = filter { it.toStartLocalDate(zoneId) == date }

fun List<ImportedEvent>.groupByStartLocalDate(
    zoneId: ZoneId = ZoneId.systemDefault()
): Map<LocalDate, List<ImportedEvent>> =
    mapNotNull { importedEvent ->
            importedEvent.toStartLocalDate(zoneId)?.let { date -> date to importedEvent }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
