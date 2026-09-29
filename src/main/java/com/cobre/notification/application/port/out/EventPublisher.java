package com.cobre.notification.application.port.out;

import com.cobre.notification.domain.NotificationEvent;

public interface EventPublisher {
  void publish(NotificationEvent event);
}
