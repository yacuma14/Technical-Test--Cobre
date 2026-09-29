-- V1 is immutable once applied. This migration switches the demo webhook
-- destination to the MockWebhookController running in the same Spring Boot app.
UPDATE subscriptions
SET webhook_url = 'http://localhost:8080/webhook';
