package com.cobre.notifications.infrastructure.adapter.in.rest

import com.cobre.notifications.application.port.in.PlatformEventUseCase
import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import spock.lang.Specification

import java.time.Instant

class PlatformEventControllerSpec extends Specification {
    PlatformEventUseCase service = Mock()
    PlatformEventController controller = new PlatformEventController(service)

    def "receive delegates event and returns HTTP 202"() {
        given:
        def event = new NotificationEvent('EVT001', 'credit_card_payment', 'payload',
                Instant.parse('2024-03-15T10:00:00Z'), DeliveryStatus.COMPLETED,
                'CLIENT001', null, 0, null)
        def pending = new NotificationEvent('EVT001', 'credit_card_payment', 'payload',
                event.deliveryDate(), DeliveryStatus.PENDING,
                'CLIENT001', 'http://localhost:8080/webhook', 0, null)

        when:
        def response = controller.receive(event)

        then:
        1 * service.receive(event) >> pending
        response.statusCode.value() == 202
        response.body == pending
    }
}
