package com.cobre.notification.infrastructure.adapter.out.kafka;

import com.cobre.notification.application.service.NotificationService;
import com.cobre.notification.domain.NotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaConsumer {

  private final NotificationService service;

  public KafkaConsumer(NotificationService service){this.service=service;}

  @KafkaListener(topics=KafkaEventPublisher.TOPIC, groupId="notification-delivery")
  public void consume(NotificationEvent event) {
    service.deliver(event);
  }
}