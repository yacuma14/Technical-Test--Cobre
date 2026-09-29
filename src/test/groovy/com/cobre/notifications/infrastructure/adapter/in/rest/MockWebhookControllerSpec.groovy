package com.cobre.notifications.infrastructure.adapter.in.rest

import com.cobre.notifications.infrastructure.adapter.in.webhook.MockWebhookController
import spock.lang.Specification

class MockWebhookControllerSpec extends Specification {
    def controller = new MockWebhookController()

    def "receive accepts webhook and returns HTTP 200"() {
        when:
        def response = controller.receive('EVT001', 'CLIENT001', '{"event":"payment"}')

        then:
        response.statusCode.value() == 200
    }
}
