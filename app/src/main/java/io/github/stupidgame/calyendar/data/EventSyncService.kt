package io.github.stupidgame.calyendar.data

import io.github.stupidgame.calyendar.utils.EventNotificationManager
import java.time.LocalDate

class EventSyncService(
    private val repository: CalYendarRepository,
    private val notificationManager: EventNotificationManager
) {
    suspend fun upsertEvent(event: Event) {
        upsertEvents(listOf(event))
    }

    suspend fun upsertRepeatedEvent(
        event: Event,
        repeatType: EventRepeatType,
        repeatUntil: LocalDate?,
        repeatDays: Set<Int>
    ) {
        upsertEvents(
            RecurringEventGenerator.generate(
                baseEvent = event,
                repeatType = repeatType,
                repeatUntil = repeatUntil,
                repeatDays = repeatDays
            )
        )
    }

    suspend fun deleteEvent(event: Event) {
        notificationManager.cancelEventNotification(event)
        repository.deleteEvent(event)
    }

    suspend fun replaceScheduledEvents(
        existingEvents: List<Event>,
        updateData: suspend () -> List<Event>
    ): List<Event> {
        cancelNotifications(existingEvents)

        return try {
            updateData().also(::scheduleNotifications)
        } catch (exception: Exception) {
            scheduleNotifications(existingEvents)
            throw exception
        }
    }

    suspend fun rescheduleAllEventNotifications() {
        val events = repository.getAllEventsSnapshot()
        cancelNotifications(events)
        scheduleNotifications(events)
    }

    private suspend fun upsertEvents(events: List<Event>) {
        events.forEach { instance ->
            if (instance.id != 0L) {
                repository.getEventById(instance.id)?.let(notificationManager::cancelEventNotification)
            }
            val id = repository.upsertEvent(instance)
            notificationManager.scheduleEventNotification(instance.copy(id = id))
        }
    }

    private fun scheduleNotifications(events: Iterable<Event>) {
        events.forEach(notificationManager::scheduleEventNotification)
    }

    private fun cancelNotifications(events: Iterable<Event>) {
        events.forEach(notificationManager::cancelEventNotification)
    }
}
