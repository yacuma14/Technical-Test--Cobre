---

## System Overview

A **multi-tenant event notification platform** that handles asynchronous delivery of events to customer webhooks with resilience patterns, replay capability, and distributed tracing.

### Core Problem Statement

Reliably deliver events to customer webhooks with:
- Tenant isolation and security
- Guaranteed retry on transient failures
- Observable state via query API
- Recovery path for permanently failed events (replay)

---

## C4 Model

### Level 1: System Context

```
                    ┌─────────────────┐
                    │   Customer      │
                    │   Application   │
                    └────────┬────────┘
                             │
                ┌────────────┼────────────┐
                │                         │
        ┌───────▼────────┐      ┌────────▼──────┐
        │  Event Source  │      │   Query API   │
        │                │      │   (Replay)    │
        └────────────────┘      └───────────────┘
                │                      │
                └──────────┬───────────┘
                           │ REST/HTTPS
                    ┌──────▼──────────┐
                    │  Notification   │
                    │    Platform     │
                    └─────────────────┘
                           │
                ┌──────────┴──────────┐
                │                     │
        ┌───────▼────────┐   ┌────────▼────┐
        │  PostgreSQL    │   │   Kafka     │
        │  (state)       │   │ (delivery)  │
        └────────────────┘   └────────┬────┘
                                       │
                            ┌──────────▼──────────┐
                            │ Customer Webhook   │
                            │ (HTTPS Target)     │
                            └────────────────────┘
```

### Level 2: Container Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Notification Platform (Java/Spring Boot)     │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                   REST API Layer                             │   │
│  │  ┌──────────────────────────────────────────────────────┐   │   │
│  │  │  NotificationController / PlatformEventController   │   │   │
│  │  │  - GET /notification_events (query & filter)        │   │   │
│  │  │  - GET /notification_events/{id}                    │   │   │
│  │  │  - POST /notification_events/{id}/replay            │   │   │
│  │  │  - POST /events (receive external events)           │   │   │
│  │  └──────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────┬─────────────────────────────────┘   │
│                               │                                      │
│  ┌────────────────────────────▼─────────────────────────────────┐   │
│  │              Application Layer (Hexagonal)                   │   │
│  │  ┌──────────────────────────────────────────────────────┐   │   │
│  │  │  NotificationService (Use Cases)                    │   │   │
│  │  │  - receive(event) → validate & persist              │   │   │
│  │  │  - find(...) → query with filters                   │   │   │
│  │  │  - get(id) → single event lookup                    │   │   │
│  │  │  - replay(id) → reset failed event to PENDING       │   │   │
│  │  │  - deliver(event) → call webhook with retries       │   │   │
│  │  └──────────────────────────────────────────────────────┘   │   │
│  │                                                               │   │
│  │  Ports (Interfaces):                                         │   │
│  │  ├─ NotificationQueryUseCase (in/driver)                    │   │
│  │  ├─ PlatformEventUseCase (in/driver)                        │   │
│  │  ├─ NotificationEventRepository (out/driven)                │   │
│  │  ├─ SubscriptionRepository (out/driven)                     │   │
│  │  ├─ EventPublisher (out/driven → Kafka)                     │   │
│  │  └─ WebhookPort (out/driven → HTTP)                         │   │
│  └───┬──────────────────────┬──────────────────────────────┬────┘   │
│      │                      │                              │         │
│  ┌───▼──────────────────┐   │   ┌──────────────────────────▼───┐   │
│  │ Persistence Adapter  │   │   │  Kafka Adapter               │   │
│  │ (PostgreSQL/JPA)     │   │   │                              │   │
│  │ ┌──────────────────┐ │   │   │ ┌────────────────────────┐   │   │
│  │ │ NotificationJPA  │ │   │   │ │ EventPublisher         │   │   │
│  │ │ SubscriptionJPA  │ │   │   │ │ - publish(event)       │   │   │
│  │ └──────────────────┘ │   │   │ │ KafkaConsumer          │   │   │
│  └─────────────────────┘   │   │ │ - consume & deliver    │   │   │
│                            │   │ └────────────────────────┘   │   │
│                            │   └──────────────┬────────────────┘   │
│                            │                  │                    │
│  ┌─────────────────────────▼──────────────────▼───────────────┐   │
│  │  Webhook Adapter (HTTP)                                    │   │
│  │  ┌────────────────────────────────────────────────────┐    │   │
│  │  │ HttpWebhookAdapter                                │    │   │
│  │  │ @Retry(exponential backoff)                       │    │   │
│  │  │ @CircuitBreaker(open after N failures)           │    │   │
│  │  │ - execute HTTPS POST to webhook URL              │    │   │
│  │  │ - return DeliveryResult(success, httpStatus)      │    │   │
│  │  └────────────────────────────────────────────────────┘    │   │
│  └────────────────────┬─────────────────────────────────────────┘   │
│                       │ HTTPS (resilient)                           │
└───────────────────────┼──────────────────────────────────────────────┘
                        │
              ┌─────────▼──────────┐
              │ Customer Webhook   │
              │ (External HTTPS)   │
              └────────────────────┘
```

### Level 3: Component Diagram (Business Logic)

```
NotificationService (Core Domain)
│
├─ receive(event: NotificationEvent)
│  ├─ subscriptions.findActive(clientId, eventType)
│  │  └─ Load subscription → webhook URL
│  ├─ Create pending event with subscription URL
│  ├─ repository.save(pending)
│  ├─ publisher.publish(event) → Kafka topic
│  └─ Return saved event
│
├─ find(...): List[NotificationEvent]
│  ├─ repository.find(clientId, dateRange, status, page, size)
│  └─ Apply tenant filter via clientId
│
├─ get(clientId, eventId): NotificationEvent
│  ├─ repository.findById(clientId, eventId)
│  └─ Enforce tenant boundary
│
├─ replay(clientId, eventId): NotificationEvent
│  ├─ Validate event status == FAILED
│  ├─ Create new PENDING event (reset attempt counter)
│  ├─ repository.save(pending)
│  ├─ publisher.publish(pending) → Kafka
│  └─ Return new pending event
│
└─ deliver(event: NotificationEvent)
   @Retry(3 attempts, exponential backoff)
   @CircuitBreaker(failureThreshold=5, waitDuration=30s)
   ├─ webhook.deliver(event)
   │  └─ POST event.webhookUrl with payload
   ├─ Update status → COMPLETED
   ├─ repository.save(completed)
   └─ [Fallback] On failure:
      ├─ Catch WebhookDeliveryException
      ├─ Update status → FAILED
      ├─ Store error message
      └─ repository.save(failed)
```

---

## Data Model

### NotificationEvent Entity

```sql
CREATE TABLE notification_events (
  -- PK & Tenant
  id UUID PRIMARY KEY,
  client_id VARCHAR(255) NOT NULL,
  
  -- Event Identity
  event_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  
  -- Delivery Target
  webhook_url TEXT NOT NULL,
  
  -- State
  delivery_status VARCHAR(50) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
  
  -- Payload
  content TEXT,
  
  -- Tracking
  delivery_date TIMESTAMP NOT NULL,
  attempt_count INT DEFAULT 0,
  last_error TEXT,
  
  -- Audit
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  
  -- Indexes
  UNIQUE(client_id, event_id),
  INDEX(client_id, delivery_status),
  INDEX(client_id, delivery_date)
);
```

### Subscription Entity (optional, future extension)

```sql
CREATE TABLE subscriptions (
  id UUID PRIMARY KEY,
  client_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  webhook_url TEXT NOT NULL,
  is_active BOOLEAN DEFAULT TRUE,
  
  UNIQUE(client_id, event_type),
  INDEX(client_id)
);
```

---

## Delivery Flow

### 1. Receive Event (Sync Request)

```
POST /events { clientId, eventType, content }
    ↓
NotificationService.receive()
    ├─ subscriptions.findActive(clientId, eventType)
    ├─ Create NotificationEvent(status=PENDING)
    ├─ repository.save()
    ├─ publisher.publish(event) → Kafka topic
    └─ Return 202 Accepted
```

### 2. Async Delivery (Consumer)

```
Kafka Consumer (KafkaConsumer.java)
    ↓
KafkaEventListener.onDeliveryEvent(NotificationEvent)
    ├─ service.deliver(event)
    │  │
    │  ├─ @Retry(maxAttempts=3, backoff exponential)
    │  ├─ @CircuitBreaker(failureThreshold=5, waitDuration=30s)
    │  │
    │  ├─ webhook.deliver(event)
    │  │  └─ POST event.webhookUrl
    │  │     ├─ Timeout: 5s
    │  │     ├─ Retry on: 5xx, timeout
    │  │     └─ No retry on: 4xx (permanent error)
    │  │
    │  ├─ Success → COMPLETED
    │  └─ Failure → FAILED (after retries exhausted)
    │
    └─ repository.save(result)
```

### 3. Query API (Self-Service)

```
GET /notification_events
  ?delivery_status=FAILED
  &from=2024-01-01T00:00:00Z
  
Header: X-Client-Id
    ↓
NotificationService.find(clientId, filters)
    ├─ repository.find(clientId, dateRange, status, pagination)
    └─ Return filtered list (tenant-scoped)
```

### 4. Manual Replay (Recovery)

```
POST /notification_events/{id}/replay
Header: X-Client-Id
    ↓
NotificationService.replay(clientId, eventId)
    ├─ Validate: event.status == FAILED
    ├─ Create new event (status=PENDING, attemptCount=0)
    ├─ repository.save()
    ├─ publisher.publish() → Kafka
    └─ Return 202 Accepted
    
[Later]
Kafka Consumer picks up event again
    └─ retry delivery flow (see step 2)
```

---

## Architectural Patterns

### 1. Hexagonal Architecture (Ports & Adapters)

**Why:**
- Business logic decoupled from frameworks
- Easy to test (mock ports)
- Technology swaps without rewriting domain

**Implementation:**
```
domain/
├─ NotificationEvent (value object)
├─ DeliveryStatus (enum)
└─ Subscription (value object)

application/port/in/
├─ NotificationQueryUseCase (driver port)
└─ PlatformEventUseCase (driver port)

application/port/out/
├─ NotificationEventRepository (driven port)
├─ SubscriptionRepository (driven port)
├─ EventPublisher (driven port)
└─ WebhookPort (driven port)

application/service/
└─ NotificationService (use case impl)

infrastructure/adapter/in/rest/
├─ NotificationController (REST adapter)
└─ PlatformEventController (REST adapter)

infrastructure/adapter/out/persistence/
├─ NotificationPersistenceAdapter (JPA)
└─ SubscriptionPersistenceAdapter (JPA)

infrastructure/adapter/out/kafka/
├─ KafkaEventPublisher (driven)
└─ KafkaConsumer (driver listener)

infrastructure/adapter/out/webhook/
└─ HttpWebhookAdapter (HTTP client)
```

### 2. Multi-Tenancy

**Isolation Level:** Logical (row-level via `client_id`)

**Implementation:**
- Every API request requires `X-Client-Id` header
- `NotificationService` validates tenant ownership
- Database queries filtered by `client_id`
- No cross-tenant data leakage possible

```java
// NotificationService.get(clientId, eventId)
NotificationEvent current = repository.findById(clientId, eventId)
    .orElseThrow(() -> new NotificationNotFoundException(eventId));
```

### 3. Event-Driven Async Delivery

**Why:**
- Decouples event ingestion (sync) from delivery (async)
- Enables retry/replay without user blocking
- Absorbs webhook latency/failures

**Implementation:**
- Kafka topic: `notification-delivery-events`
- Consumer group: `notification-delivery-group`
- Partition key: `clientId` (ordered per tenant)

### 4. Resilience Patterns

#### Retry (Exponential Backoff)
```java
@Retry(name = "webhookDelivery")
public void deliver(NotificationEvent event)
```
- Transient failures retry automatically
- Exponential backoff prevents thundering herd
- Max attempts: 3 (configurable)

#### Circuit Breaker (Fail-Fast)
```java
@CircuitBreaker(name = "webhookDelivery", fallbackMethod = "deliveryFallback")
public void deliver(NotificationEvent event)
```
- Opens after 5 consecutive failures
- Waits 30s before attempting half-open
- Prevents cascading failures to webhook

#### Fallback (Graceful Degradation)
```java
public void deliveryFallback(NotificationEvent event, Throwable cause) {
    // Mark as FAILED, store error, enable manual replay
    repository.save(new NotificationEvent(..., DeliveryStatus.FAILED, cause.getMessage()));
}
```

### 5. Observability

**Distributed Tracing:** Spring Cloud Sleuth (W3C)
- Every request generates `trace_id` and `span_id`
- Propagated to Kafka and webhook calls
- Useful for debugging multi-tenant issues

**Metrics:** Micrometer/Actuator
- Endpoint: `GET /actuator/metrics`
- Tracks: delivery success rate, retry counts, circuit breaker state

**Health Checks:** Spring Actuator
- Endpoint: `GET /actuator/health`
- Checks: Database, Kafka broker connectivity

---
## Deployment

### Docker Compose
```yaml
services:
  app:
    image: notification-events:latest
    ports: [8080]
    environment:
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/notification
  kafka:
    image: confluentinc/cp-kafka:latest
  db:
    image: postgres:15
```



