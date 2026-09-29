package com.cobre.notification.infrastructure.adapter.in.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@Slf4j
@RestController
@RequestMapping("/webhook")
public class MockWebhookController {

  private static final Random random = new Random();

  @PostMapping
  public ResponseEntity<String> receive(
          @RequestHeader("X-Cobre-Event-Id") String eventId,
          @RequestHeader("X-Cobre-Client-Id") String clientId,
          @RequestBody String payload) {

    int randomResponse = random.nextInt(100);
    HttpStatus status;
    String responseMessage;

    if (randomResponse < 70) {
      status = HttpStatus.OK;
      responseMessage = "Event processed successfully";
      log.info("✓ Webhook SUCCESS. eventId={}, clientId={}", eventId, clientId);
    } else if (randomResponse < 85) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
      responseMessage = "Internal server error - simulated failure";
      log.warn("✗ Webhook FAILED (500). eventId={}, clientId={}", eventId, clientId);
    } else if (randomResponse < 95) {
      status = HttpStatus.BAD_REQUEST;
      responseMessage = "Bad request - invalid payload";
      log.warn("✗ Webhook FAILED (400). eventId={}, clientId={}", eventId, clientId);
    } else {
      status = HttpStatus.SERVICE_UNAVAILABLE;
      responseMessage = "Service temporarily unavailable";
      log.warn("✗ Webhook FAILED (503). eventId={}, clientId={}", eventId, clientId);
    }

    log.debug("Webhook payload: {}", payload);
    return ResponseEntity.status(status).body(responseMessage);
  }
}