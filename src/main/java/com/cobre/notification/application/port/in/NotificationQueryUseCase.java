package com.cobre.notification.application.port.in;

import com.cobre.notification.domain.DeliveryStatus;
import com.cobre.notification.domain.NotificationEvent;
import java.time.Instant;
import java.util.List;

public interface NotificationQueryUseCase {
  List<NotificationEvent> find(String clientId, Instant from, Instant to,
                               DeliveryStatus status, int page, int size);

  NotificationEvent get(String clientId, String eventId);

  NotificationEvent replay(String clientId, String eventId);
}
