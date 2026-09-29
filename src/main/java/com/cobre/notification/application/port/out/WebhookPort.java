package com.cobre.notification.application.port.out;

import com.cobre.notification.domain.NotificationEvent;

public interface WebhookPort {
  DeliveryResult deliver(NotificationEvent event);

  record DeliveryResult(boolean success, int httpStatus, String errorMessage) {}
}
