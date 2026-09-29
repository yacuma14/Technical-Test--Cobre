package com.cobre.notification.infrastructure.adapter.out.persistence;

import com.cobre.notification.application.port.out.SubscriptionRepository;
import com.cobre.notification.domain.Subscription;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionRepository {
  private final SpringDataSubscriptionRepository repository;
  public SubscriptionPersistenceAdapter(SpringDataSubscriptionRepository repository){this.repository=repository;}
  public Optional<Subscription> findActive(String clientId,String eventType){
    return repository.findByClientIdAndEventTypeAndActiveTrue(clientId,eventType)
            .map(e -> new Subscription(e.getClientId(),e.getEventType(),e.getWebhookUrl(),e.isActive()));
  }
}