package com.cobre.notification.application.service;

import com.cobre.notification.application.port.in.NotificationQueryUseCase;
import com.cobre.notification.application.port.in.PlatformEventUseCase;
import com.cobre.notification.application.port.out.EventPublisher;
import com.cobre.notification.application.port.out.NotificationEventRepository;
import com.cobre.notification.application.port.out.SubscriptionRepository;
import com.cobre.notification.application.port.out.WebhookPort;
import com.cobre.notification.domain.DeliveryStatus;
import com.cobre.notification.domain.NotificationEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService implements NotificationQueryUseCase, PlatformEventUseCase {

  private final NotificationEventRepository repository;
  private final SubscriptionRepository subscriptions;
  private final WebhookPort webhook;
  private final EventPublisher publisher;

  public NotificationService(NotificationEventRepository repository,
                             SubscriptionRepository subscriptions,
                             WebhookPort webhook,
                             EventPublisher publisher) {
    this.repository = repository;
    this.subscriptions = subscriptions;
    this.webhook = webhook;
    this.publisher = publisher;
  }

  @Override
  @Transactional(readOnly = true)
  public List<NotificationEvent> find(String clientId, Instant from, Instant to,
                                      DeliveryStatus status, int page, int size) {
    return repository.find(clientId, from, to, status, page, Math.min(size, 100));
  }

  @Override
  @Transactional(readOnly = true)
  public NotificationEvent get(String clientId, String eventId) {
    return repository.findById(clientId, eventId)
            .orElseThrow(() -> new NotificationNotFoundException(eventId));
  }

  @Override
  @Transactional
  public NotificationEvent replay(String clientId, String eventId) {
    NotificationEvent current = get(clientId, eventId);
    if (current.getDeliveryStatus() != DeliveryStatus.FAILED) {
      throw new IllegalStateException("Only definitively failed notifications can be replayed");
    }
    NotificationEvent pending = new NotificationEvent(
            current.getEventId(), current.getEventType(), current.getContent(), current.getDeliveryDate(),
            DeliveryStatus.PENDING, current.getClientId(), current.getWebhookUrl(), 0, null);
    NotificationEvent saved = repository.save(pending);
    publisher.publish(saved);
    return saved;
  }

  @Override
  @Transactional
  public NotificationEvent receive(NotificationEvent event) {
    return subscriptions.findActive(event.getClientId(), event.getEventType())
            .map(subscription -> {
              NotificationEvent pending = new NotificationEvent(
                      event.getEventId(), event.getEventType(), event.getContent(), event.getDeliveryDate(),
                      DeliveryStatus.PENDING, event.getClientId(), subscription.getWebhookUrl(), 0, null);
              NotificationEvent saved = repository.save(pending);
              publisher.publish(saved);
              return saved;
            })
            .orElseGet(() -> event);
  }

  @Retry(name = "webhookDelivery")
  @CircuitBreaker(name = "webhookDelivery", fallbackMethod = "deliveryFallback")
  public void deliver(NotificationEvent event) {
    WebhookPort.DeliveryResult result = webhook.deliver(event);
    if (!result.success()) {
      throw new WebhookDeliveryException(result.httpStatus(), result.errorMessage());
    }
    repository.save(new NotificationEvent(
            event.getEventId(), event.getEventType(), event.getContent(), event.getDeliveryDate(),
            DeliveryStatus.COMPLETED, event.getClientId(), event.getWebhookUrl(),
            event.getAttemptCount() + 1, null));
  }

  @Transactional
  public void deliveryFallback(NotificationEvent event, Throwable cause) {
    repository.save(new NotificationEvent(
            event.getEventId(), event.getEventType(), event.getContent(), event.getDeliveryDate(),
            DeliveryStatus.FAILED, event.getClientId(), event.getWebhookUrl(),
            event.getAttemptCount() + 1, cause.getMessage()));
  }

  public static class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(String id) { super("Notification event not found: " + id); }
  }

  public static class WebhookDeliveryException extends RuntimeException {
    public WebhookDeliveryException(int status, String message) {
      super("Webhook delivery failed. HTTP " + status + ": " + message);
    }
  }
}
