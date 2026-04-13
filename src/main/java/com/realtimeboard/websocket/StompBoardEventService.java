package com.realtimeboard.websocket;

import com.realtimeboard.websocket.dto.BoardEvent;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class StompBoardEventService implements BoardEventService {

  private final SimpMessagingTemplate messagingTemplate;

  public StompBoardEventService(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  @Override
  public void publish(Long boardId, BoardEvent event) {
    Objects.requireNonNull(boardId, "boardId");
    Objects.requireNonNull(event, "event");
    String destination = "/topic/board/" + boardId;

    if (TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              messagingTemplate.convertAndSend(destination, event);
            }
          });
      return;
    }

    messagingTemplate.convertAndSend(destination, event);
  }
}

