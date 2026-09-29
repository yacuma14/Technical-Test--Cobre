# Cobre Notification Events

**Event notification delivery platform** with tenant isolation, async processing, resilience patterns, and self-service replay capabilities.

## Quick Start

```bash
# Build
mvn clean package

# Run with Docker Compose
docker-compose up

# API available at
curl http://localhost:8080/notification_events \
  -H "X-Client-Id: tenant-123"
```

## Stack

| Layer | Technology |
|-------|-----------|
| **Runtime** | Java 18, Spring Boot 2.7.18 |
| **REST** | Spring MVC, Spring Validation |
| **Architecture** | Hexagonal (Ports & Adapters) |
| **Database** | PostgreSQL + Flyway migrations |
| **Messaging** | Apache Kafka (async event delivery) |
| **Resilience** | Resilience4j (Retry, Circuit Breaker) |
| **Observability** | Spring Cloud Sleuth (W3C traces), Micrometer/Actuator |

> **Why Spring Boot 2.7?** Spring Cloud Sleuth 3.1.x aligns with Spring Cloud 2021.x / Spring Boot 2.7, preserving Java 18 compatibility and the requested tracing stack.

## Core Features

- ✅ **Multi-tenant isolation** via `X-Client-Id` header
- ✅ **Async webhook delivery** with Kafka event streaming
- ✅ **Resilience patterns** (retry + circuit breaker) for webhook failures
- ✅ **Replay capability** for failed deliveries
- ✅ **Query API** with filtering by status, date range, and pagination
- ✅ **Event tracing** with W3C distributed tracing
- ✅ **Health checks** and metrics via Actuator

## Architecture

```text
                    +-----------------------------+
                    |      Customer / API Client  |
                    +-------------+---------------+
                                  |
                              HTTPS/REST
                                  |
                    +-------------v---------------+
                    | Notification REST Adapter   |
                    | GET /notification_events    |
                    | GET /notification_events/id |
                    | POST /.../replay             |
                    +-------------+---------------+
                                  |
                         Application Ports
                                  |
                    +-------------v---------------+
                    | NotificationService         |
                    | - tenant isolation          |
                    | - replay rules               |
                    | - subscription validation    |
                    +------+----------------------+
                           |
                +----------+-----------+
                |                      |
        +-------v------+       +-------v--------+
        | PostgreSQL   |       | Kafka          |
        | state/query  |       | async delivery |
        +--------------+       +-------+--------+
                                       |
                               +-------v--------+
                               | Delivery       |
                               | Consumer       |
                               +-------+--------+
                                       |
                           Retry + Circuit Breaker
                                       |
                               +-------v--------+
                               | HTTPS Webhook  |
                               +----------------+
```

See **[ARCHITECTURE.md](./ARCHITECTURE.md)** for detailed C4 diagrams and design decisions.

## API Endpoints

### Query Notifications
```http
GET /notification_events
  ?delivery_status=FAILED
  &from=2024-01-01T00:00:00Z
  &to=2024-12-31T23:59:59Z
  &page=0
  &size=20
  
Header: X-Client-Id: tenant-123
```

**Status Codes:** `200 OK` with paginated results

### Get Single Notification
```http
GET /notification_events/{notification_event_id}

Header: X-Client-Id: tenant-123
```

**Status Codes:** `200 OK` | `404 Not Found`

### Replay Failed Notification
```http
POST /notification_events/{notification_event_id}/replay

Header: X-Client-Id: tenant-123
```

**Status Codes:** `202 Accepted` (returns new pending event) | `404 Not Found` | `400 Bad Request` (not FAILED status)

## Delivery Status Lifecycle

```
PENDING → PROCESSING → COMPLETED
       ↓
    FAILED ─→ REPLAY → PENDING (retry cycle)
```

- **PENDING:** Event created, awaiting webhook delivery
- **PROCESSING:** Kafka consumer picked up the event
- **COMPLETED:** Successfully delivered to webhook
- **FAILED:** Max retries exhausted or permanent error

## Resilience Strategy

Webhook delivery uses:
- **Retry policy:** Exponential backoff (configured via resilience4j)
- **Circuit breaker:** Opens after consecutive failures to prevent cascading
- **Fallback:** On final failure, event marked as FAILED with error message
- **Manual recovery:** Replay endpoint for explicit retry

## Database Schema

- `notification_events` → Core event state (tenant-scoped)
- `subscriptions` → Webhook URL mappings per event type
- Schema versioning via Flyway migrations

## Development

```bash
# Run tests
mvn test

# Run application
mvn spring-boot:run

# View logs with traces
docker-compose logs -f app | grep "trace_id"

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

## Documentation

- [**ARCHITECTURE.md**](./ARCHITECTURE.md) — Detailed C4 model, decisions, and trade-offs
