package com.cobre.notification.infrastructure.adapter.out.webhook;

import com.cobre.notification.application.exception.PermanentWebhookDeliveryException;
import com.cobre.notification.application.port.out.WebhookPort;
import com.cobre.notification.domain.NotificationEvent;
import com.cobre.notification.application.exception.WebhookDeliveryException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class HttpWebhookAdapter implements WebhookPort {

  private final RestTemplate restTemplate;

  @Override
  public DeliveryResult deliver(NotificationEvent event) {

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("X-Cobre-Event-Id", event.getEventId());
      headers.set("X-Cobre-Client-Id", event.getClientId());

      HttpEntity<String> request =
              new HttpEntity<>(event.getContent(), headers);

      ResponseEntity<String> response = restTemplate.exchange(
              event.getWebhookUrl(),
              HttpMethod.POST,
              request,
              String.class
      );

      if (!response.getStatusCode().is2xxSuccessful()) {
          handleHttpError(event, response.getStatusCode());
      }
      boolean ok = response.getStatusCode().is2xxSuccessful();
      return new DeliveryResult(ok, response.getStatusCodeValue(),
              ok ? null : response.getBody());
    } catch (WebhookDeliveryException ex) {
      throw ex;

    } catch (Exception ex) {
      throw new WebhookDeliveryException(
              "Error delivering event " + event.getEventId()
                      + " to webhook " + event.getWebhookUrl()
                      + ": " + ex.getMessage(),
              ex
      );
    }
  }

  private void handleHttpError(
          NotificationEvent event,
          HttpStatus status) {

    String message = String.format(
            "Webhook returned HTTP %s for event %s",
            status.value(),
            event.getEventId()
    );
    if (status.is4xxClientError()) {
      throw new PermanentWebhookDeliveryException(message);
    }
    throw new WebhookDeliveryException(message);
  }
}