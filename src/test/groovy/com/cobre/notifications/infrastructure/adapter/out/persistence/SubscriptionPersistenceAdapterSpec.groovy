package com.cobre.notifications.infrastructure.adapter.out.persistence

import spock.lang.Specification

class SubscriptionPersistenceAdapterSpec extends Specification {
    SpringDataSubscriptionRepository repository = Mock()
    SubscriptionPersistenceAdapter adapter = new SubscriptionPersistenceAdapter(repository)

    def "findActive maps active subscription to domain"() {
        given:
        def entity = Stub(SubscriptionEntity) {
            getClientId() >> 'CLIENT001'
            getEventType() >> 'credit_card_payment'
            getWebhookUrl() >> 'http://localhost:8080/webhook'
            isActive() >> true
        }
        repository.findByClientIdAndEventTypeAndActiveTrue('CLIENT001', 'credit_card_payment') >> Optional.of(entity)

        when:
        def result = adapter.findActive('CLIENT001', 'credit_card_payment')

        then:
        result.present
        result.get().clientId() == 'CLIENT001'
        result.get().eventType() == 'credit_card_payment'
        result.get().webhookUrl() == 'http://localhost:8080/webhook'
        result.get().active()
    }

    def "findActive returns empty when no active subscription exists"() {
        given:
        repository.findByClientIdAndEventTypeAndActiveTrue('CLIENT001', 'credit_card_payment') >> Optional.empty()

        expect:
        adapter.findActive('CLIENT001', 'credit_card_payment').isEmpty()
    }
}
