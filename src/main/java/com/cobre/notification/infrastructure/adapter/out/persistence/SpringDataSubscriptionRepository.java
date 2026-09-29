package com.cobre.notification.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SpringDataSubscriptionRepository extends JpaRepository<SubscriptionEntity,Long> {
  Optional<SubscriptionEntity> findByClientIdAndEventTypeAndActiveTrue(String clientId, String eventType);
}
