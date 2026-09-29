package com.cobre.notification.application.exception;

public class PermanentWebhookDeliveryException extends RuntimeException {

  public PermanentWebhookDeliveryException(String message) {
    super(message);
  }
}