package com.cobre.notifications.domain

import spock.lang.Specification

import java.time.Instant

class DomainSpec extends Specification {
    def "notification event preserves all fields"() {
        given:
        def date = Instant.parse('2024-03-15T10:00:00Z')

        when:
        def event = new NotificationEvent('EVT001', 'credit_card_payment', 'payload', date,
                DeliveryStatus.PENDING, 'CLIENT001', 'http://localhost:8080/webhook', 1, null)

        then:
        event.eventId() == 'EVT001'
        event.eventType() == 'credit_card_payment'
        event.content() == 'payload'
        event.deliveryDate() == date
        event.deliveryStatus() == DeliveryStatus.PENDING
        event.clientId() == 'CLIENT001'
        event.webhookUrl() == 'http://localhost:8080/webhook'
        event.attemptCount() == 1
        event.lastError() == null
    }

    def "subscription preserves all fields"() {
        when:
        def subscription = new Subscription('CLIENT001', 'credit_card_payment',
                'http://localhost:8080/webhook', true)

        then:
        subscription.clientId() == 'CLIENT001'
        subscription.eventType() == 'credit_card_payment'
        subscription.webhookUrl() == 'http://localhost:8080/webhook'
        subscription.active()
    }
}
