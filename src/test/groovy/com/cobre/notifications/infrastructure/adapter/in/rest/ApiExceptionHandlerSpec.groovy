package com.cobre.notifications.infrastructure.adapter.in.rest

import com.cobre.notifications.application.service.NotificationService
import org.springframework.http.HttpStatus
import spock.lang.Specification

class ApiExceptionHandlerSpec extends Specification {
    def handler = new ApiExceptionHandler()

    def "notFound maps notification exception to 404"() {
        when:
        def response = handler.notFound(new NotificationService.NotificationNotFoundException('EVT999'))

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.error == 'NOT_FOUND'
        response.body.message == 'Notification event not found: EVT999'
        response.body.timestamp != null
    }

    def "conflict maps illegal state to 409"() {
        when:
        def response = handler.conflict(new IllegalStateException('Only failed events can be replayed'))

        then:
        response.statusCode == HttpStatus.CONFLICT
        response.body.error == 'INVALID_STATE'
        response.body.message == 'Only failed events can be replayed'
        response.body.timestamp != null
    }
}
