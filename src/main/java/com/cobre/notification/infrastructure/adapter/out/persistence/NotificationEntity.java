package com.cobre.notification.infrastructure.adapter.out.persistence;

import com.cobre.notification.domain.DeliveryStatus;
import com.cobre.notification.domain.NotificationEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notification_events",
        indexes = {
                @Index(name = "idx_notification_client_date", columnList = "client_id,delivery_date"),
                @Index(name = "idx_notification_client_status", columnList = "client_id,delivery_status")
        })
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEntity {

  @Id
  @Column(name = "event_id", nullable = false)
  private String eventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "delivery_date", nullable = false)
  private Instant deliveryDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "delivery_status", nullable = false)
  private DeliveryStatus deliveryStatus;

  @Column(name = "client_id", nullable = false)
  private String clientId;

  @Column(name = "webhook_url")
  private String webhookUrl;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

  public NotificationEntity(NotificationEvent event) {
    this.eventId = event.getEventId();
    this.eventType = event.getEventType();
    this.content = event.getContent();
    this.deliveryDate = event.getDeliveryDate();
    this.deliveryStatus = event.getDeliveryStatus();
    this.clientId = event.getClientId();
    this.webhookUrl = event.getWebhookUrl();
    this.attemptCount = event.getAttemptCount();
    this.lastError = event.getLastError();
  }

  public NotificationEvent toDomain() {
    return new NotificationEvent(eventId,eventType,content,deliveryDate,deliveryStatus,
            clientId,webhookUrl,attemptCount,lastError);
  }
  public void update(NotificationEvent e) {
    this.eventType=e.getEventType(); this.content=e.getContent(); this.deliveryDate=e.getDeliveryDate();
    this.deliveryStatus=e.getDeliveryStatus(); this.clientId=e.getClientId(); this.webhookUrl=e.getWebhookUrl();
    this.attemptCount=e.getAttemptCount(); this.lastError=e.getLastError();
  }
}
