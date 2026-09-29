package com.cobre.notification.infrastructure.adapter.out.persistence;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name="subscriptions", uniqueConstraints=@UniqueConstraint(
        name="uk_subscription_client_event", columnNames={"client_id","event_type"}))
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SubscriptionEntity {

  @Id
  @GeneratedValue(strategy=GenerationType.IDENTITY)
  private Long id;

  @Column(name="client_id",nullable=false)
  private String clientId;

  @Column(name="event_type",nullable=false)
  private String eventType;

  @Column(name="webhook_url",nullable=false)
  private String webhookUrl;

  @Column(nullable=false)
  private boolean active;

}
