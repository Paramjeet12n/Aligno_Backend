package com.realtimeboard.websocket.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BoardEvent {
  String type;
  Long boardId;
  Long actorUserId;
  Instant timestamp;
  Object payload;
}

