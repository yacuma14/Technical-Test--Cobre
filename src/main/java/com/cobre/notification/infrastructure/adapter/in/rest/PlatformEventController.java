package com.cobre.notification.infrastructure.adapter.in.rest;

import com.cobre.notification.application.port.in.PlatformEventUseCase;
import com.cobre.notification.domain.NotificationEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/events")
public class PlatformEventController {
  private final PlatformEventUseCase service;
  public PlatformEventController(PlatformEventUseCase service){this.service=service;}

  @PostMapping
  public ResponseEntity<NotificationEvent> receive(@RequestBody NotificationEvent event){
    return ResponseEntity.accepted().body(service.receive(event));
  }
}
