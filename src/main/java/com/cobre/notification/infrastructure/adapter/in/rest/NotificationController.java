package com.cobre.notification.infrastructure.adapter.in.rest;

import com.cobre.notification.application.port.in.NotificationQueryUseCase;
import com.cobre.notification.domain.DeliveryStatus;
import com.cobre.notification.domain.NotificationEvent;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/notification_events")
public class NotificationController {
  private final NotificationQueryUseCase service;
  public NotificationController(NotificationQueryUseCase service){this.service=service;}

  @GetMapping
  public List<NotificationEvent> find(
          @RequestHeader("X-Client-Id") String clientId,
          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,
          @RequestParam(name="delivery_status",required=false) DeliveryStatus status,
          @RequestParam(defaultValue="0") int page,
          @RequestParam(defaultValue="20") int size) {
    return service.find(clientId,from,to,status,Math.max(page,0),Math.min(size,100));
  }

  @GetMapping("/{notification_event_id}")
  public NotificationEvent get(@RequestHeader("X-Client-Id") String clientId,
                               @PathVariable("notification_event_id") String id){
    return service.get(clientId,id);
  }

  @PostMapping("/{notification_event_id}/replay")
  public ResponseEntity<NotificationEvent> replay(@RequestHeader("X-Client-Id") String clientId,
                                                  @PathVariable("notification_event_id") String id){
    return ResponseEntity.accepted().body(service.replay(clientId,id));
  }
}
