package com.cobre.notification.infrastructure.adapter.in.rest;

import com.cobre.notification.application.service.NotificationService.NotificationNotFoundException;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;


@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(NotificationNotFoundException.class)
  ResponseEntity<ErrorResponse> notFound(NotificationNotFoundException ex){
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .error("NOT_FOUND")
            .message(ex.getMessage())
            .build());
  }
  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ErrorResponse> conflict(IllegalStateException ex){
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .error("INVALID_STATE")
            .message(ex.getMessage())
            .build());
  }


  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> validation(
          MethodArgumentNotValidException ex,
          HttpServletRequest request) {

    List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse.builder()
            .timestamp(LocalDateTime.now())
            .error("BAD_REQUEST")
            .message("Validation error.")
            .path(request.getRequestURI())
            .details(errors)
            .build());

  }

}
