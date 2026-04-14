package io.github.stupidgame.calyendar.data

import biweekly.component.VEvent

fun ImportedEvent.identityKey(): String = event.identityKey(isHoliday)

fun importedEventsToReplace(
    existingEvents: Iterable<ImportedEvent>,
    incomingEvents: Iterable<ImportedEvent>
): List<ImportedEvent> {
    val incomingKeys = incomingEvents.mapTo(hashSetOf()) { it.identityKey() }
    return existingEvents.filter { !it.isHoliday && it.identityKey() in incomingKeys }
}

private fun VEvent.identityKey(isHoliday: Boolean): String {
    val uidValue = uid?.value?.trim().orEmpty()
    val startValue = dateStart?.value?.time?.toString().orEmpty()

    if (uidValue.isNotEmpty()) {
        return "uid|$isHoliday|$uidValue|$startValue"
    }

    val summaryValue = summary?.value?.trim().orEmpty()
    val endValue = dateEnd?.value?.time?.toString().orEmpty()

    return "fallback|$isHoliday|$summaryValue|$startValue|$endValue"
}
