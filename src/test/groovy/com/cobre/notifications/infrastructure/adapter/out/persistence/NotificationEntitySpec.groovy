package com.cobre.notifications.infrastructure.adapter.out.persistence

import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import spock.lang.Specification

import java.time.Instant

class NotificationEntitySpec extends Specification {
    def "constructor maps domain event and toDomain returns equivalent event"() {
        given:
        def event = new NotificationEvent('EVT001', 'credit_card_payment', 'payload',
                Instant.parse('2024-03-15T10:00:00Z'), DeliveryStatus.PENDING,
                'CLIENT001', 'http://localhost:8080/webhook', 2, 'timeout')

        when:
        def entity = new NotificationEntity(event)

        then:
        entity.toDomain() == event
    }

    def "update replaces all mutable fields"() {
        given:
        def original = new NotificationEvent('EVT001', 'old', 'old-payload',
                Instant.parse('2024-03-15T10:00:00Z'), DeliveryStatus.PENDING,
                'CLIENT001', 'http://old', 1, null)
        def updated = new NotificationEvent('EVT001', 'new', 'new-payload',
                Instant.parse('2024-03-15T11:00:00Z'), DeliveryStatus.FAILED,
                'CLIENT002', 'http://new', 4, 'HTTP 500')
        def entity = new NotificationEntity(original)

        when:
        entity.update(updated)

        then:
        entity.toDomain() == updated
    }
}
