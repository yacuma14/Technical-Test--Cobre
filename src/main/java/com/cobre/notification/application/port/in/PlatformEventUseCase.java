package com.cobre.notification.application.port.in;

import com.cobre.notification.domain.NotificationEvent;

public interface PlatformEventUseCase {
  NotificationEvent receive(NotificationEvent event);
}
