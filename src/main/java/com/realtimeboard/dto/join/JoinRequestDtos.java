package com.realtimeboard.dto.join;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

public final class JoinRequestDtos {
  private JoinRequestDtos() {}

  @Value
  @Builder
  public static class JoinRequestResponse {
    Long id;
    Long userId;
    String userName;
    String userEmail;
    Instant requestedAt;
  }
}

