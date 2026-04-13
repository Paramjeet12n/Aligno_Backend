package com.realtimeboard.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> apiException(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(
            ApiError.builder()
                .message(ex.getMessage())
                .code(ex.getCode())
                .timestamp(Instant.now())
                .build());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> accessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiError.builder()
                .message("Forbidden")
                .code("FORBIDDEN")
                .timestamp(Instant.now())
                .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : err.getField() + " invalid")
            .orElse("Validation failed");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiError.builder()
                .message(message)
                .code("VALIDATION_ERROR")
                .timestamp(Instant.now())
                .build());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> constraint(ConstraintViolationException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiError.builder()
                .message("Validation failed")
                .code("VALIDATION_ERROR")
                .timestamp(Instant.now())
                .build());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> notReadable(HttpMessageNotReadableException ex) {
    log.warn("Unreadable request body: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiError.builder()
                .message("Invalid request body")
                .code("BAD_REQUEST")
                .timestamp(Instant.now())
                .build());
  }

  /**
   * Must be more specific than {@link #dataAccess}; duplicate email (etc.) is a 409, not a generic DB
   * failure.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException ex) {
    String detail = String.valueOf(ex.getMostSpecificCause().getMessage());
    String haystack = (ex.getMessage() + " " + detail).toLowerCase();
    boolean likelyEmailUnique =
        haystack.contains("email")
            || haystack.contains("users_email")
            || haystack.contains("uq_") && haystack.contains("email");
    if (likelyEmailUnique) {
      log.warn("Duplicate email on register: {}", detail);
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body(
              ApiError.builder()
                  .message("Email already registered")
                  .code("EMAIL_EXISTS")
                  .timestamp(Instant.now())
                  .build());
    }
    log.warn("Data integrity violation: {}", detail);
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiError.builder()
                .message("This request conflicts with existing data")
                .code("CONFLICT")
                .timestamp(Instant.now())
                .build());
  }

  @ExceptionHandler(DataAccessException.class)
  public ResponseEntity<ApiError> dataAccess(DataAccessException ex) {
    log.error("Data access error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiError.builder()
                .message(
                    "A database error occurred. If you use PostgreSQL, ensure Flyway migrations are applied.")
                .code("DATABASE_ERROR")
                .timestamp(Instant.now())
                .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> fallback(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiError.builder()
                .message("Internal server error")
                .code("INTERNAL_ERROR")
                .timestamp(Instant.now())
                .build());
  }
}

