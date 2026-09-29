package com.cobre.notifications.infrastructure.adapter.out.kafka

import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

import java.time.Instant

class KafkaEventPublisherSpec extends Specification {
    KafkaTemplate<String, NotificationEvent> kafkaTemplate = Mock()
    KafkaEventPublisher publisher = new KafkaEventPublisher(kafkaTemplate)

    def "publish sends event to notification topic using client id as key"() {
        given:
        def event = new NotificationEvent('EVT001', 'credit_card_payment', 'payload',
                Instant.parse('2024-03-15T10:00:00Z'), DeliveryStatus.PENDING,
                'CLIENT001', 'http://localhost:8080/webhook', 0, null)

        when:
        publisher.publish(event)

        then:
        1 * kafkaTemplate.send(KafkaEventPublisher.TOPIC, 'CLIENT001', event)
    }
}
