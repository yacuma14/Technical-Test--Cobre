package com.cobre.notifications.infrastructure.adapter.out.persistence

import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import spock.lang.Specification

import java.time.Instant

class NotificationPersistenceAdapterSpec extends Specification {
    SpringDataNotificationRepository repository = Mock()
    NotificationPersistenceAdapter adapter = new NotificationPersistenceAdapter(repository)

    def "find filters by date and status, sorts ascending and paginates"() {
        given:
        def e1 = entity(event('EVT001', '2024-03-15T10:00:00Z', DeliveryStatus.COMPLETED))
        def e2 = entity(event('EVT002', '2024-03-15T11:00:00Z', DeliveryStatus.FAILED))
        def e3 = entity(event('EVT003', '2024-03-15T12:00:00Z', DeliveryStatus.FAILED))
        repository.findByClientId('CLIENT001') >> [e3, e1, e2]

        when:
        def result = adapter.find('CLIENT001',
                Instant.parse('2024-03-15T10:30:00Z'),
                Instant.parse('2024-03-15T13:00:00Z'),
                DeliveryStatus.FAILED, 0, 10)

        then:
        result*.eventId() == ['EVT002', 'EVT003']
    }

    def "findById returns domain event"() {
        given:
        def entity = entity(event('EVT001', '2024-03-15T10:00:00Z', DeliveryStatus.COMPLETED))
        repository.findByClientIdAndEventId('CLIENT001', 'EVT001') >> Optional.of(entity)

        expect:
        adapter.findById('CLIENT001', 'EVT001').get().eventId() == 'EVT001'
    }

    def "findById returns empty when repository has no event"() {
        given:
        repository.findByClientIdAndEventId('CLIENT001', 'EVT999') >> Optional.empty()

        expect:
        adapter.findById('CLIENT001', 'EVT999').isEmpty()
    }

    def "save creates entity when event does not exist"() {
        given:
        def event = event('EVT001', '2024-03-15T10:00:00Z', DeliveryStatus.PENDING)
        repository.findById('EVT001') >> Optional.empty()
        when:
        def result = adapter.save(event)

        then:
        1 * repository.save({ it.toDomain() == event }) >> { NotificationEntity e -> e }
        result == event
    }

    def "save updates existing entity"() {
        given:
        def original = event('EVT001', '2024-03-15T10:00:00Z', DeliveryStatus.PENDING)
        def updated = event('EVT001', '2024-03-15T10:00:00Z', DeliveryStatus.COMPLETED)
        def entity = entity(original)
        repository.findById('EVT001') >> Optional.of(entity)
        repository.save(entity) >> entity

        when:
        def result = adapter.save(updated)

        then:
        result == updated
        entity.toDomain() == updated
    }

    private static NotificationEvent event(String id, String date, DeliveryStatus status) {
        new NotificationEvent(id, 'credit_transfer', 'payload', Instant.parse(date), status,
                'CLIENT001', 'http://localhost:8080/webhook', 0, null)
    }

    private static NotificationEntity entity(NotificationEvent event) {
        new NotificationEntity(event)
    }
}
