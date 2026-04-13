package com.realtimeboard.websocket.dto;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PresenceSnapshot {
  long boardId;
  Instant timestamp;
  List<PresenceUser> users;

  @Value
  @Builder
  public static class PresenceUser {
    long userId;
    String name;
  }
}

