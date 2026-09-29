package com.cobre.notifications.infrastructure.adapter.in.rest


import com.cobre.notifications.application.port.in.NotificationQueryUseCase
import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import spock.lang.Specification

import java.time.Instant

class NotificationControllerSpec extends Specification {
    NotificationQueryUseCase service = Mock()
    NotificationController controller = new NotificationController(service)

    def "find passes request filters and normalizes page and size"() {
        given:
        def event = event('EVT001')

        when:
        def result = controller.find('CLIENT001', null, null, DeliveryStatus.COMPLETED, -3, 500)

        then:
        1 * service.find('CLIENT001', null, null, DeliveryStatus.COMPLETED, 0, 100) >> [event]
        result == [event]
    }

    def "get delegates client isolation and event id"() {
        given:
        def event = event('EVT001')

        when:
        def result = controller.get('CLIENT001', 'EVT001')

        then:
        1 * service.get('CLIENT001', 'EVT001') >> event
        result == event
    }

    def "replay returns HTTP 202"() {
        given:
        def event = event('EVT003')

        when:
        def response = controller.replay('CLIENT002', 'EVT003')

        then:
        1 * service.replay('CLIENT002', 'EVT003') >> event
        response.statusCode.value() == 202
        response.body == event
    }

    private static NotificationEvent event(String id) {
        new NotificationEvent(id, 'credit_transfer', 'payload', Instant.parse('2024-03-15T10:00:00Z'),
                com.cobre.notification.domain.DeliveryStatus.PENDING, 'CLIENT001', 'http://localhost:8080/webhook', 0, null)
    }
}
