package com.cobre.notifications.infrastructure.config

import org.springframework.core.env.Environment
import spock.lang.Specification

class KafkaConfigSpec extends Specification {
    def "creates Kafka topic with expected name and partitions"() {
        when:
        def topic = new KafkaConfig().notificationEventsTopic()

        then:
        topic.name() == 'notification-events'
        topic.numPartitions() == 6
        topic.replicationFactor() == 1
    }

    def "creates producer factory from configured bootstrap server"() {
        given:
        def environment = Mock(Environment)
        environment.getProperty('spring.kafka.bootstrap-servers', 'localhost:9092') >> 'localhost:29092'

        expect:
        new KafkaConfig().producerFactory(environment) != null
    }

    def "creates consumer factory from configured bootstrap server"() {
        given:
        def environment = Mock(Environment)
        environment.getProperty('spring.kafka.bootstrap-servers', 'localhost:9092') >> 'localhost:29092'

        expect:
        new KafkaConfig().consumerFactory(environment) != null
    }
}
