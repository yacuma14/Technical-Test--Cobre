package com.cobre.notification.infrastructure.adapter.out.persistence;

import com.cobre.notification.domain.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationEntity,String> {

  List<NotificationEntity> findByClientIdAndDeliveryDateGreaterThanEqualAndDeliveryDateLessThanEqual(
          String clientId, Instant from, Instant to);

  List<NotificationEntity> findByClientIdAndDeliveryStatus(String clientId, DeliveryStatus status);

  List<NotificationEntity> findByClientId(String clientId);

  Optional<NotificationEntity> findByClientIdAndEventId(String clientId, String eventId);
}