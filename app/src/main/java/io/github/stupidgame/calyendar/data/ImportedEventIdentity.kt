package io.github.stupidgame.calyendar.data

import biweekly.component.VEvent

// biweekly assigns a random UUID to a newly created VEvent, including events
// parsed from feeds that did not provide a UID. It changes on every import.
private val generatedUidPattern = Regex(
    "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",
    RegexOption.IGNORE_CASE
)

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

    if (uidValue.isNotEmpty() && !generatedUidPattern.matches(uidValue)) {
        return "uid|$isHoliday|$uidValue|$startValue"
    }

    val summaryValue = summary?.value?.trim().orEmpty()
    val endValue = dateEnd?.value?.time?.toString().orEmpty()

    return "fallback|$isHoliday|$summaryValue|$startValue|$endValue"
}
