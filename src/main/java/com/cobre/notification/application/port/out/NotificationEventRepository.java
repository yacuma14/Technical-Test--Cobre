package com.cobre.notification.application.port.out;

import com.cobre.notification.domain.DeliveryStatus;
import com.cobre.notification.domain.NotificationEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationEventRepository {

  List<NotificationEvent> find(String clientId, Instant from, Instant to,
                               DeliveryStatus status, int page, int size);

  Optional<NotificationEvent> findById(String clientId, String eventId);

  NotificationEvent save(NotificationEvent event);
}
