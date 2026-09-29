package com.cobre.notification.infrastructure.adapter.out.persistence;

import com.cobre.notification.application.port.out.NotificationEventRepository;
import com.cobre.notification.domain.DeliveryStatus;
import com.cobre.notification.domain.NotificationEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class NotificationPersistenceAdapter implements NotificationEventRepository {

  private final SpringDataNotificationRepository repository;

  public NotificationPersistenceAdapter(SpringDataNotificationRepository repository) {
    this.repository=repository;
  }

  @Override
  public List<NotificationEvent> find(String clientId, Instant from, Instant to,
                                      DeliveryStatus status, int page, int size) {
    List<NotificationEntity> all = repository.findByClientId(clientId);

    return all.stream()
            .filter(e -> from == null || !e.toDomain().getDeliveryDate().isBefore(from))
            .filter(e -> to == null || !e.toDomain().getDeliveryDate().isAfter(to))
            .filter(e -> status == null || e.toDomain().getDeliveryStatus() == status)
            .sorted(Comparator.comparing(e -> e.toDomain().getDeliveryDate()))
            .skip((long)page * size)
            .limit(size)
            .map(NotificationEntity::toDomain)
            .toList();
  }

  @Override
  public Optional<NotificationEvent> findById(String clientId, String eventId) {
    return repository.findByClientIdAndEventId(clientId,eventId).map(NotificationEntity::toDomain);
  }

  @Override
  public NotificationEvent save(NotificationEvent event) {
    NotificationEntity entity = repository.findById(event.getEventId())
            .orElse(new NotificationEntity(event));
    entity.update(event);
    return repository.save(entity).toDomain();
  }
}
