package com.cobre.notification.domain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private String clientId;
    private String eventType;
    private String webhookUrl;
    private boolean active;

}