package com.cobre.notifications.infrastructure.config

import com.cobre.notifications.application.port.out.NotificationEventRepository
import com.cobre.notifications.domain.DeliveryStatus
import spock.lang.Specification

class DataInitializerSpec extends Specification {
    def "seed loads all fixture events into repository"() {
        given:
        def repository = Mock(NotificationEventRepository)
        def runner = new DataInitializer().seed(repository)

        when:
        runner.run()

        then:
        10 * repository.save({ it.eventId().startsWith('EVT') && it.clientId() != null })
    }
}
