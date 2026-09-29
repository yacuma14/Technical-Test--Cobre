package com.cobre.notifications.infrastructure.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class WebClientConfigSpec extends Specification {
    def "creates RestTemplate bean"() {
        when:
        RestTemplate restTemplate = new WebClientConfig().restTemplate(new RestTemplateBuilder())

        then:
        restTemplate != null
    }
}
