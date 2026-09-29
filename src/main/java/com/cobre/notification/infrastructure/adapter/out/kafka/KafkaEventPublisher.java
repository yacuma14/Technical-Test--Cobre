package com.cobre.notification.infrastructure.adapter.out.kafka;

import com.cobre.notification.application.port.out.EventPublisher;
import com.cobre.notification.domain.NotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {
  public static final String TOPIC = "notification-events";
  private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

  public KafkaEventPublisher(KafkaTemplate<String, NotificationEvent> kafkaTemplate){
    this.kafkaTemplate=kafkaTemplate;
  }

  public void publish(NotificationEvent event){
    kafkaTemplate.send(TOPIC,event.getClientId(),event);
  }
}
