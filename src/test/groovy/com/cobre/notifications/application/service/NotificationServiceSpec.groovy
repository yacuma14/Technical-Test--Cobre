package com.cobre.notifications.application.service

import com.cobre.notifications.application.port.out.EventPublisher
import com.cobre.notifications.application.port.out.NotificationEventRepository
import com.cobre.notifications.application.port.out.SubscriptionRepository
import com.cobre.notifications.application.port.out.WebhookPort
import com.cobre.notifications.domain.DeliveryStatus
import com.cobre.notifications.domain.NotificationEvent
import com.cobre.notifications.domain.Subscription
import spock.lang.Specification

import java.time.Instant

class NotificationServiceSpec extends Specification {
    NotificationEventRepository repository = Mock()
    SubscriptionRepository subscriptions = Mock()
    WebhookPort webhook = Mock()
    EventPublisher publisher = Mock()
    NotificationService service = new NotificationService(repository, subscriptions, webhook, publisher)

    def "find delegates filters and caps page size at 100"() {
        given:
        def from = Instant.parse('2024-03-15T00:00:00Z')
        def to = Instant.parse('2024-03-16T00:00:00Z')
        def expected = [event('EVT001', DeliveryStatus.COMPLETED)]

        when:
        def result = service.find('CLIENT001', from, to, DeliveryStatus.COMPLETED, 2, 500)

        then:
        1 * repository.find('CLIENT001', from, to, DeliveryStatus.COMPLETED, 2, 100) >> expected
        result == expected
    }

    def "get returns event for the requested client"() {
        given:
        def expected = event('EVT001', DeliveryStatus.COMPLETED)

        when:
        def result = service.get('CLIENT001', 'EVT001')

        then:
        1 * repository.findById('CLIENT001', 'EVT001') >> Optional.of(expected)
        result == expected
    }

    def "get throws when event does not exist"() {
        when:
        service.get('CLIENT001', 'EVT999')

        then:
        1 * repository.findById('CLIENT001', 'EVT999') >> Optional.empty()
        def ex = thrown(NotificationService.NotificationNotFoundException)
        ex.message == 'Notification event not found: EVT999'
    }

    def "replay only failed notification, resets attempt count and publishes"() {
        given:
        def failed = event('EVT003', DeliveryStatus.FAILED, 4, 'HTTP 500')
        def pending = event('EVT003', DeliveryStatus.PENDING)

        when:
        def result = service.replay('CLIENT002', 'EVT003')

        then:
        1 * repository.findById('CLIENT002', 'EVT003') >> Optional.of(failed)
        1 * repository.save({
            it.eventId() == 'EVT003' &&
            it.deliveryStatus() == DeliveryStatus.PENDING &&
            it.attemptCount() == 0 &&
            it.lastError() == null
        }) >> pending
        1 * publisher.publish(pending)
        result == pending
    }

    def "replay rejects notification that is not definitively failed"() {
        given:
        def completed = event('EVT001', DeliveryStatus.COMPLETED)

        when:
        service.replay('CLIENT001', 'EVT001')

        then:
        1 * repository.findById('CLIENT001', 'EVT001') >> Optional.of(completed)
        thrown(IllegalStateException)
        0 * repository.save(_)
        0 * publisher.publish(_)
    }

    def "receive creates pending notification only when an active subscription exists"() {
        given:
        def source = event('EVT001', DeliveryStatus.COMPLETED)
        def pending = event('EVT001', DeliveryStatus.PENDING)
        def subscription = new Subscription('CLIENT001', 'credit_card_payment', 'http://localhost:8080/webhook', true)

        when:
        def result = service.receive(source)

        then:
        1 * subscriptions.findActive('CLIENT001', 'credit_card_payment') >> Optional.of(subscription)
        1 * repository.save({
            it.eventId() == 'EVT001' &&
            it.clientId() == 'CLIENT001' &&
            it.deliveryStatus() == DeliveryStatus.PENDING &&
            it.webhookUrl() == 'http://localhost:8080/webhook'
        }) >> pending
        1 * publisher.publish(pending)
        result == pending
    }

    def "receive ignores event when client and event type have no active subscription"() {
        given:
        def source = event('EVT999', DeliveryStatus.COMPLETED)

        when:
        def result = service.receive(source)

        then:
        1 * subscriptions.findActive('CLIENT001', 'credit_card_payment') >> Optional.empty()
        0 * repository.save(_)
        0 * publisher.publish(_)
        result == source
    }

    def "deliver persists completed notification after successful webhook"() {
        given:
        def pending = event('EVT001', DeliveryStatus.PENDING, 2, null)

        when:
        service.deliver(pending)

        then:
        1 * webhook.deliver(pending) >> new WebhookPort.DeliveryResult(true, 200, null)
        1 * repository.save({
            it.eventId() == 'EVT001' &&
            it.deliveryStatus() == DeliveryStatus.COMPLETED &&
            it.attemptCount() == 3 &&
            it.lastError() == null
        })
    }

    def "deliver throws domain exception when webhook returns failure"() {
        given:
        def pending = event('EVT001', DeliveryStatus.PENDING)

        when:
        service.deliver(pending)

        then:
        1 * webhook.deliver(pending) >> new WebhookPort.DeliveryResult(false, 503, 'Service unavailable')
        def ex = thrown(NotificationService.WebhookDeliveryException)
        ex.message == 'Webhook delivery failed. HTTP 503: Service unavailable'
        0 * repository.save(_)
    }

    def "deliveryFallback persists definitive failure"() {
        given:
        def pending = event('EVT001', DeliveryStatus.PENDING, 3, null)
        def cause = new RuntimeException('timeout')

        when:
        service.deliveryFallback(pending, cause)

        then:
        1 * repository.save({
            it.eventId() == 'EVT001' &&
            it.deliveryStatus() == DeliveryStatus.FAILED &&
            it.attemptCount() == 4 &&
            it.lastError() == 'timeout'
        })
    }

    private static NotificationEvent event(String id, DeliveryStatus status, int attempts = 0, String error = null) {
        new NotificationEvent(
                id,
                id == 'EVT003' ? 'credit_transfer' : 'credit_card_payment',
                'test-content',
                Instant.parse('2024-03-15T10:00:00Z'),
                status,
                id == 'EVT003' ? 'CLIENT002' : 'CLIENT001',
                'http://localhost:8080/webhook',
                attempts,
                error
        )
    }
}
