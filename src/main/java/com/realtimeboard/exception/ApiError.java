package com.realtimeboard.exception;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiError {
  String message;
  String code;
  Instant timestamp;
}

