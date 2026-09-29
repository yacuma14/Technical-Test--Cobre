package com.cobre.notification.infrastructure.config;

import com.cobre.notification.domain.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

  private final ObjectMapper objectMapper;

  public KafkaConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  ProducerFactory<String, NotificationEvent> producerFactory(org.springframework.core.env.Environment env) {
    java.util.Map<String, Object> p = new java.util.HashMap<>();
    p.put("bootstrap.servers", env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"));
    p.put("key.serializer", StringSerializer.class);

    JsonSerializer<NotificationEvent> valueSerializer = new JsonSerializer<>(objectMapper);
    return new DefaultKafkaProducerFactory<>(p, new StringSerializer(), valueSerializer);
  }

  @Bean
  KafkaTemplate<String, NotificationEvent> kafkaTemplate(ProducerFactory<String, NotificationEvent> pf) {
    return new KafkaTemplate<>(pf);
  }

  @Bean
  ConsumerFactory<String, NotificationEvent> consumerFactory(org.springframework.core.env.Environment env) {
    java.util.Map<String, Object> p = new java.util.HashMap<>();
    p.put("bootstrap.servers", env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"));
    p.put("group.id", "notification-delivery");
    p.put("key.deserializer", StringDeserializer.class);
    p.put("value.deserializer", JsonDeserializer.class);

    p.put(JsonDeserializer.TRUSTED_PACKAGES, "com.cobre.notification.domain");

    JsonDeserializer<NotificationEvent> valueDeserializer = new JsonDeserializer<>(NotificationEvent.class, objectMapper, false);

    return new DefaultKafkaConsumerFactory<>(p, new StringDeserializer(), valueDeserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> kafkaListenerContainerFactory(
          ConsumerFactory<String, NotificationEvent> consumerFactory) {

    ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
  }

  @Bean
  NewTopic notificationEventsTopic() {
    return TopicBuilder.name("notification-events").partitions(6).replicas(1).build();
  }
}
