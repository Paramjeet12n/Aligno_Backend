package com.realtimeboard.service;

import com.realtimeboard.model.entity.Card;
import com.realtimeboard.model.entity.CardActionType;
import com.realtimeboard.model.entity.CardActivity;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.CardActivityRepository;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CardActivityService {

  private final CardActivityRepository cardActivityRepository;

  public CardActivityService(CardActivityRepository cardActivityRepository) {
    this.cardActivityRepository = cardActivityRepository;
  }

  /**
   * Record card activity asynchronously. Runs in a separate thread and transaction
   * so it does not block the main card-move operation or delay the WebSocket event.
   */
  @Async("activityExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(Card card, CardActionType actionType, User performedBy, String details) {
    try {
      CardActivity a = new CardActivity();
      a.setCard(card);
      a.setActionType(actionType);
      a.setPerformedBy(performedBy);
      a.setTimestamp(Instant.now());
      a.setDetails(details);
      cardActivityRepository.save(a);
    } catch (Exception e) {
      log.warn("Failed to record card activity for card {}: {}", card.getId(), e.getMessage());
    }
  }
}

