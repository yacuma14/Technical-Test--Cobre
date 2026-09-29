package com.cobre.notifications.infrastructure.adapter.out.kafka

import com.cobre.notifications.application.service.NotificationService
import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import spock.lang.Specification

import java.time.Instant

class KafkaConsumerSpec extends Specification {
    NotificationService service = Mock()
    KafkaConsumer consumer = new KafkaConsumer(service)

    def "consume delegates Kafka event to notification service"() {
        given:
        def event = new NotificationEvent('EVT001', 'credit_card_payment', 'payload',
                Instant.parse('2024-03-15T10:00:00Z'), DeliveryStatus.PENDING,
                'CLIENT001', 'http://localhost:8080/webhook', 0, null)

        when:
        consumer.consume(event)

        then:
        1 * service.deliver(event)
    }
}
