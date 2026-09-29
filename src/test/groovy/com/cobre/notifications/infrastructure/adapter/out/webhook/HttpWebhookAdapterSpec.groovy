package com.cobre.notifications.infrastructure.adapter.out.webhook


import com.cobre.notifications.domain.NotificationEvent
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import java.time.Instant

class HttpWebhookAdapterSpec extends Specification {
    RestTemplate restTemplate = Mock()
    HttpWebhookAdapter adapter = new HttpWebhookAdapter(restTemplate)

    def "deliver sends POST with Cobre headers and returns success"() {
        given:
        def event = event('EVT001')
        def response = new ResponseEntity<String>('{"ok":true}', HttpStatus.OK)

        when:
        def result = adapter.deliver(event)

        then:
        1 * restTemplate.postForEntity('http://localhost:8080/webhook', { request ->
            request.headers.getFirst('X-Cobre-Event-Id') == 'EVT001' &&
            request.headers.getFirst('X-Cobre-Client-Id') == 'CLIENT001' &&
            request.headers.contentType.toString() == 'application/json' &&
            request.body == event
        }, String.class) >> response
        result.success()
        result.httpStatus() == 200
        result.errorMessage() == null
    }

    def "deliver returns failed result for non 2xx response"() {
        given:
        def event = event('EVT001')
        def response = new ResponseEntity<String>('not found', HttpStatus.NOT_FOUND)

        when:
        def result = adapter.deliver(event)

        then:
        1 * restTemplate.postForEntity(_, _, String.class) >> response
        !result.success()
        result.httpStatus() == 404
        result.errorMessage() == 'not found'
    }

    def "deliver converts transport exceptions to failed result"() {
        given:
        def event = event('EVT001')

        when:
        def result = adapter.deliver(event)

        then:
        1 * restTemplate.postForEntity(_, _, String.class) >> { throw new RuntimeException('Connection refused') }
        !result.success()
        result.httpStatus() == 0
        result.errorMessage() == 'Connection refused'
    }

    private static NotificationEvent event(String id) {
        new NotificationEvent(id, 'credit_card_payment', 'payload', Instant.parse('2024-03-15T10:00:00Z'),
                com.cobre.notification.domain.DeliveryStatus.PENDING, 'CLIENT001', 'http://localhost:8080/webhook', 0, null)
    }
}
