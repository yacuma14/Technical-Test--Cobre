package com.cobre.notification.domain;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificationEvent{
        @NotBlank(message = "El eventId es obligatorio")
        String eventId;

        @NotBlank(message = "El eventType es obligatorio")
        String eventType;

        String content;

        @NotNull(message = "La fecha es obligatoria")
        Instant deliveryDate;

        DeliveryStatus deliveryStatus;

        @NotBlank(message = "El clientId es obligatorio")
        String clientId;
        String webhookUrl;
        int attemptCount;
        String lastError;
}