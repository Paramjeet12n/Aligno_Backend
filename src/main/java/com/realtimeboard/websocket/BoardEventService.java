package com.realtimeboard.websocket;

import com.realtimeboard.websocket.dto.BoardEvent;

public interface BoardEventService {
  void publish(Long boardId, BoardEvent event);
}

