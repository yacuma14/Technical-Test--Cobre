# C4 Architecture Diagrams - Notification Events System

## Level 1: System Context Diagram

Shows how the Notification Platform interacts with external systems and actors.

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                       │
│  ┌────────────────────────┐  ┌────────────────────────┐             │
│  │  Event Publishing App  │  │  Query / Admin Client  │             │
│  │  (Event Source)        │  │  (Web/CLI)             │             │
│  └────────────┬───────────┘  └────────────┬───────────┘             │
│               │                           │                         │
│               │ POST /events              │ GET /notification_*     │
│               │                           │ POST /.../replay        │
│               │                           │                         │
│               └──────────────┬────────────┘                         │
│                              │ REST/HTTPS                          │
│  ┌──────────────────────────▼─────────────────────────────────────┐│
│  │                                                                  ││
│  │      Notification Events Platform                               ││
│  │      (Multi-tenant event delivery + replay)                     ││
│  │                                                                  ││
│  │  - Async webhook delivery via Kafka                             ││
│  │  - Resilience patterns (retry + circuit breaker)                ││
│  │  - Query API with filters & pagination                          ││
│  │  - Manual replay for failed events                              ││
│  │                                                                  ││
│  └──────────────────┬──────────────────────┬───────────────────────┘│
│                     │                      │                        │
│  "reads from"       │ "writes to"          │ "sends to"            │
│                     │                      │                        │
│  ┌──────────────────▼──┐    ┌──────────────▼──┐   ┌─────────────┐  │
│  │  PostgreSQL DB      │    │  Kafka Broker   │   │   Customer  │  │
│  │  (Event State)      │    │  (Event Stream) │   │   Webhooks  │  │
│  │                     │    │                 │   │   (HTTP)    │  │
│  └─────────────────────┘    └─────────────────┘   └─────────────┘  │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘

[Actors]
- Event Publishing App: Creates events, provides event metadata
- Query/Admin Client: Retrieves event status, replays failed events
- PostgreSQL: Stores notification state (ACID, durability)
- Kafka: Decouples event ingestion from delivery
- Customer Webhooks: Consumes delivered events
```

---

## Level 2: Container Diagram

Shows major containers (services, databases) and their interactions.

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          Notification System                              │
│                                                                            │
│  ┌────────────────────────────────────────────────────────────────────┐  │
│  │ Spring Boot Application (Java 18, Spring Boot 2.7.18)             │  │
│  │                                                                    │  │
│  │  ┌──────────────────────────────────────────────────────────┐    │  │
│  │  │ REST API Layer                                           │    │  │
│  │  │ ┌────────────────────────────────────────────────────┐  │    │  │
│  │  │ │ NotificationController / PlatformEventController │  │    │  │
│  │  │ │ - GET /notification_events (query with filters)  │  │    │  │
│  │  │ │ - GET /notification_events/{id} (single event)   │  │    │  │
│  │  │ │ - POST /notification_events/{id}/replay          │  │    │  │
│  │  │ │ - POST /events (receive external events)         │  │    │  │
│  │  │ │ - ApiExceptionHandler (error normalization)      │  │    │  │
│  │  │ └────────────────────────────────────────────────────┘  │    │  │
│  │  └──────────────────┬───────────────────────────────────────┘    │  │
│  │                     │                                            │  │
│  │  ┌──────────────────▼───────────────────────────────────────┐   │  │
│  │  │ Application Layer (Hexagonal)                            │   │  │
│  │  │ ┌────────────────────────────────────────────────────┐  │   │  │
│  │  │ │ NotificationService (Core Business Logic)         │  │   │  │
│  │  │ │ - receive(event) → validate & store               │  │   │  │
│  │  │ │ - find(...) → query with filters                  │  │   │  │
│  │  │ │ - get(id) → single event lookup                   │  │   │  │
│  │  │ │ - replay(id) → reset failed event to PENDING      │  │   │  │
│  │  │ │ - deliver(event) → call webhook with resilience   │  │   │  │
│  │  │ └────────────────────────────────────────────────────┘  │   │  │
│  │  │                                                          │   │  │
│  │  │ Ports (Interfaces):                                     │   │  │
│  │  │ ├─ NotificationQueryUseCase (driver)                  │   │  │
│  │  │ ├─ PlatformEventUseCase (driver)                      │   │  │
│  │  │ ├─ NotificationEventRepository (driven)               │   │  │
│  │  │ ├─ SubscriptionRepository (driven)                    │   │  │
│  │  │ ├─ EventPublisher (driven → Kafka)                    │   │  │
│  │  │ └─ WebhookPort (driven → HTTP)                        │   │  │
│  │  └──────────────────┬──────────────────────┬──────────┬───┘   │  │
│  │                     │                      │          │        │  │
│  │  ┌──────────────────▼─┐  ┌────────────────▼──┐  ┌───▼────┐  │  │
│  │  │ Persistence Adapter│  │ Kafka Adapter     │  │ Webhook│  │  │
│  │  │ (Spring Data JPA)  │  │                   │  │ Adapter│  │  │
│  │  │ ┌────────────────┐ │  │ ┌──────────────┐  │  │ ┌────┐ │  │  │
│  │  │ │ NotificationJPA│ │  │ │EventPublisher│  │  │ │HTTP│ │  │  │
│  │  │ │SubscriptionJPA│ │  │ │KafkaConsumer │  │  │ │CB  │ │  │  │
│  │  │ └────────────────┘ │  │ └──────────────┘  │  │ │RT  │ │  │  │
│  │  └────────────────────┘  └───────────┬──────┘  │ └────┘ │  │  │
│  │                                      │         └───┬────┘  │  │
│  │                                      │             │       │  │
│  │  Infrastructure Config:              │             │       │  │
│  │  ├─ WebClientConfig (HTTP client)    │             │       │  │
│  │  ├─ KafkaConfig (topics, partitions) │             │       │  │
│  │  └─ Resilience4jConfig (retry/CB)    │             │       │  │
│  │                                      │             │       │  │
│  └──────────────────────────────────────┼─────────────┼───────┘  │
│                                         │             │          │
└────────────────────────────────┬────────┼─────────────┼──────────┘
                                 │        │             │
                    ┌────────────▼──┐  ┌─▼──────────┐  │
                    │ PostgreSQL 15 │  │ Kafka 3.x  │  │
                    │ (State Store) │  │ (Messaging)│  │
                    │               │  │            │  │
                    │ Tables:       │  │ Topics:    │  │
                    │ - notification_   │ - notif-  │  │
                    │   events          │   ication │  │
                    │ - subscriptions    │ -delivery │  │
                    │                │  │ -events   │  │
                    └─────────────────┘  └────┬─────┘  │
                                              │        │
                                    ┌─────────▼────────▼──┐
                                    │  Customer Webhooks │
                                    │  (External HTTPS)  │
                                    │                    │
                                    │  Target URLs:      │
                                    │ - Receive events   │
                                    │ - Process async    │
                                    │                    │
                                    └────────────────────┘
```

---

## Level 3: Component Diagram (Detailed)

Shows internal structure of the Spring Boot application.

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                            │
│                                                                       │
│  [IN - Driver Ports]                                                │
│  ┌────────────────────────────┐  ┌──────────────────────────────┐  │
│  │ NotificationQueryUseCase   │  │ PlatformEventUseCase         │  │
│  │ (Interface)                │  │ (Interface)                  │  │
│  └────────────┬───────────────┘  └──────────────┬───────────────┘  │
│               │                                  │                  │
│               │ implemented by                   │ implemented by   │
│               │                                  │                  │
│  ┌────────────▼──────────────────────────────────▼──────────────┐  │
│  │                   NotificationService                        │  │
│  │                  (Core Domain Logic)                         │  │
│  │                                                              │  │
│  │  Public Methods:                                             │  │
│  │  ├─ List<NotificationEvent> find(clientId, filters...)      │  │
│  │  ├─ NotificationEvent get(clientId, eventId)                │  │
│  │  ├─ NotificationEvent receive(event)                        │  │
│  │  ├─ NotificationEvent replay(clientId, eventId)             │  │
│  │  │  │                                                        │  │
│  │  │  └─ [Business Rule] Only FAILED events can replay        │  │
│  │  │                                                           │  │
│  │  ├─ @Retry void deliver(event)                              │  │
│  │  │  │                                                        │  │
│  │  │  ├─ [Resilience] Exponential backoff (3 retries)         │  │
│  │  │  │                                                        │  │
│  │  │  └─ @CircuitBreaker void deliver(event)                  │  │
│  │  │     │                                                     │  │
│  │  │     └─ [Resilience] Open after 5 consecutive failures    │  │
│  │  │                                                           │  │
│  │  └─ void deliveryFallback(event, cause)                     │  │
│  │     └─ [Fallback] Mark as FAILED, enable replay             │  │
│  │                                                              │  │
│  └────────────┬──────────────────────────────────┬──────────┬───┘  │
│               │                                  │          │      │
│               │                                  │          │      │
│  [OUT - Driven Ports]                           │          │      │
│               │                                  │          │      │
│  ┌────────────▼──────────────────────────────┐  │          │      │
│  │  NotificationEventRepository               │  │          │      │
│  │  (Interface)                               │  │          │      │
│  │                                            │  │          │      │
│  │  + find(clientId, filters): List           │  │          │      │
│  │  + findById(clientId, eventId): Optional   │  │          │      │
│  │  + save(event): NotificationEvent          │  │          │      │
│  │                                            │  │          │      │
│  │  [Adapter Implementation]                  │  │          │      │
│  │  NotificationPersistenceAdapter            │  │          │      │
│  │  └─ SpringDataNotificationRepository (JPA)│  │          │      │
│  │     └─ PostgreSQL                          │  │          │      │
│  └────────────────────────────────────────────┘  │          │      │
│                                                  │          │      │
│  ┌───────────────────────────────────────────────┼──────┐   │      │
│  │  SubscriptionRepository                       │      │   │      │
│  │  (Interface)                                  │      │   │      │
│  │                                               │      │   │      │
│  │  + findActive(clientId, eventType): Optional  │      │   │      │
│  │                                               │      │   │      │
│  │  [Adapter Implementation]                     │      │   │      │
│  │  SubscriptionPersistenceAdapter               │      │   │      │
│  │  └─ SpringDataSubscriptionRepository (JPA)    │      │   │      │
│  │     └─ PostgreSQL                             │      │   │      │
│  └───────────────────────────────────────────────┼──────┘   │      │
│                                                  │          │      │
│  ┌───────────────────────────────────────────────┼──────┐   │      │
│  │  EventPublisher (Kafka)                       │      │   │      │
│  │  (Interface)                                  │      │   │      │
│  │                                               │      │   │      │
│  │  + publish(event): void                       │      │   │      │
│  │                                               │      │   │      │
│  │  [Adapter Implementation]                     │      │   │      │
│  │  KafkaEventPublisher                          │      │   │      │
│  │  └─ Spring Kafka (send to topic)              │      │   │      │
│  │     └─ Kafka Broker                           │      │   │      │
│  └───────────────────────────────────────────────┼──────┘   │      │
│                                                  │          │      │
│  ┌───────────────────────────────────────────────▼──────┐   │      │
│  │  WebhookPort                                        │   │      │
│  │  (Interface)                                        │   │      │
│  │                                                     │   │      │
│  │  + DeliveryResult deliver(event)                   │   │      │
│  │    - success: boolean                              │   │      │
│  │    - httpStatus: int                               │   │      │
│  │    - errorMessage: String                          │   │      │
│  │                                                     │   │      │
│  │  [Adapter Implementation]                          │   │      │
│  │  HttpWebhookAdapter                                │   │      │
│  │  ├─ WebClientConfig (async HTTP client)            │   │      │
│  │  ├─ Timeout: 5s                                    │   │      │
│  │  ├─ Retry backoff (before @Retry wraps it)         │   │      │
│  │  └─ Customer Webhook URL (HTTP POST)               │   │      │
│  └───────────────────────────────────────────────┬─────┘   │      │
│                                                  │          │      │
│  [Controllers - REST Adapters]                  │          │      │
│  ┌───────────────────────────────────────────────┼──────┐   │      │
│  │  NotificationController                       │      │   │      │
│  │  └─ @RequestMapping("/notification_events")  │      │   │      │
│  │     ├─ GET / (find with filters)              │      │   │      │
│  │     ├─ GET /{id} (get single)                 │      │   │      │
│  │     └─ POST /{id}/replay (manual retry)       │      │   │      │
│  │     All endpoints validate "X-Client-Id"     │      │   │      │
│  └───────────────────────────────────────────────┼──────┘   │      │
│                                                  │          │      │
│  ┌───────────────────────────────────────────────┼──────┐   │      │
│  │  PlatformEventController                      │      │   │      │
│  │  └─ @RequestMapping("/events")                │      │   │      │
│  │     └─ POST / (receive external events)       │      │   │      │
│  │        Delegates to service.receive()         │      │   │      │
│  └───────────────────────────────────────────────┼──────┘   │      │
│                                                  │          │      │
│  [Kafka Consumer - Event Listener]              │          │      │
│  ┌───────────────────────────────────────────────▼──────┐   │      │
│  │  KafkaConsumer                                       │   │      │
│  │  └─ @KafkaListener(topic="notification-delivery")   │   │      │
│  │     └─ onDeliveryEvent(event)                       │   │      │
│  │        └─ service.deliver(event)                    │   │      │
│  │           (Triggers @Retry + @CircuitBreaker)       │   │      │
│  └─────────────────────────────────────────────────────┘   │      │
│                                                            │      │
└────────────────────────────────────────────────────────────┘      │
                                                                    │
                                                                    │
┌─────────────────────────────────────────────────────────────────┘
│
│  [Exceptions]
│  - NotificationNotFoundException (404)
│  - WebhookDeliveryException (wrapped by @CircuitBreaker)
│  - PermanentWebhookDeliveryException (4xx errors)
│  - ApiExceptionHandler (normalizes to REST responses)
│
└─────────────────────────────────────────────────────────────────
```

---

## Level 4: Class Diagram (Domain Model)

```
Domain Entities & Value Objects
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌────────────────────────────┐
│   NotificationEvent        │
├────────────────────────────┤
│ - eventId: String          │
│ - eventType: String        │
│ - content: String          │
│ - clientId: String         │
│ - webhookUrl: String       │
│ - deliveryDate: Instant    │
│ - deliveryStatus: Enum     │
│ - attemptCount: int        │
│ - lastError: String        │
├────────────────────────────┤
│ + builder()                │
│ + getters (Lombok)         │
└────────────────────────────┘
         │ uses
         ▼
┌────────────────────────────┐
│   DeliveryStatus           │
├────────────────────────────┤
│ PENDING                    │
│ PROCESSING                 │
│ COMPLETED                  │
│ FAILED                     │
└────────────────────────────┘

┌────────────────────────────┐
│   Subscription             │
├────────────────────────────┤
│ - clientId: String         │
│ - eventType: String        │
│ - webhookUrl: String       │
│ - isActive: boolean        │
├────────────────────────────┤
│ + builder()                │
│ + getters                  │
└────────────────────────────┘
```

---

## Deployment Topology

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network                          │
│                                                             │
│  ┌───────────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │ Spring Boot App   │  │  PostgreSQL  │  │   Kafka     │ │
│  │ (Notification SVC)│  │   (State)    │  │  (Messaging)│ │
│  │                   │  │              │  │             │ │
│  │ :8080/health      │  │ :5432        │  │ :9092       │ │
│  │ :8080/actuator    │  │              │  │             │ │
│  │ :8080/rest API    │  │ notification │  │ Topics:     │ │
│  │                   │  │ _events DB   │  │ - notif...  │ │
│  └─────────────┬─────┘  └──────────────┘  └─────────────┘ │
│                │                                            │
│                └─────────────────┬──────────────────────────┘
│                          Docker Bridge Network
│
└─────────────────────────────────────────────────────────────┘

External
├─ Event Publishers (REST Clients)
│  └─ POST http://localhost:8080/events
│
├─ Query Clients (REST Clients)
│  └─ GET http://localhost:8080/notification_events
│
└─ Customer Webhooks (HTTPS Targets)
   └─ Receive POST payloads from Kafka consumer
```

---

## Data Flow Diagrams

### Scenario 1: Event Ingestion → Async Delivery

```
[1] POST /events
    ├─ Header: X-Client-Id: tenant-A
    ├─ Body: { eventType: "order.created", content: {...} }
    └─> REST Controller

[2] NotificationController.receive(event)
    └─> NotificationService.receive(event)

[3] Service validates
    ├─ subscriptions.findActive(tenant-A, order.created)
    │  └─> Returns WebhookUrl for tenant-A
    └─> Creates NotificationEvent(status=PENDING)

[4] repository.save(notification)
    └─> PostgreSQL insert

[5] publisher.publish(notification)
    └─> Kafka topic: notification-delivery-events
        (partition key = clientId for ordering)

[6] REST response: 202 Accepted
    ├─ Body: { eventId, status: PENDING, ... }
    └─> Request completes (async now)

[Later, in background]

[7] Kafka Consumer listens on topic
    └─> onDeliveryEvent(notification)

[8] NotificationService.deliver(notification)
    @Retry (max 3, exponential backoff)
    @CircuitBreaker (open after 5 failures)
    ├─ webhook.deliver(notification)
    │  └─> POST notification.webhookUrl
    │     ├─ Timeout: 5s
    │     └─ Headers: { X-Webhook-Signature, X-Trace-Id }
    │
    ├─ Success (2xx)
    │  └─ repository.save(COMPLETED)
    │
    └─ Failure (5xx, timeout)
       └─ Retry (with backoff)
          └─ If exhausted: deliveryFallback()
             └─ repository.save(FAILED, errorMessage)
```

### Scenario 2: Manual Replay of Failed Event

```
[1] GET /notification_events?delivery_status=FAILED
    └─> List all FAILED events for tenant

[2] User sees event with id=evt-123, status=FAILED

[3] POST /notification_events/evt-123/replay
    ├─ Header: X-Client-Id: tenant-A
    └─> REST Controller

[4] NotificationController.replay(tenant-A, evt-123)
    └─> NotificationService.replay(tenant-A, evt-123)

[5] Service validates
    ├─ repository.findById(tenant-A, evt-123)
    ├─ Check: current.status == FAILED
    │  └─ If not: throw IllegalStateException
    └─> OK, proceed

[6] Create new NotificationEvent
    ├─ eventId: same as original
    ├─ status: PENDING (reset)
    ├─ attemptCount: 0 (reset)
    └─ webhookUrl: same as original

[7] repository.save(pending)
    └─> PostgreSQL insert (new record)

[8] publisher.publish(pending)
    └─> Kafka topic: notification-delivery-events

[9] REST response: 202 Accepted
    ├─ Body: { eventId, status: PENDING, ... }
    └─> Original event still FAILED in DB
        New event ready for delivery

[Later, in background]
    └─> Kafka consumer picks up new event
        └─> Retry delivery flow (see Scenario 1, steps 7-8)
```
