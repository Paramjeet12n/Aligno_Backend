package com.realtimeboard.service;

import com.realtimeboard.dto.card.CardDtos.CardResponse;
import com.realtimeboard.dto.card.CardDtos.CreateCardRequest;
import com.realtimeboard.dto.card.CardDtos.UpdateCardRequest;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.BoardColumn;
import com.realtimeboard.model.entity.Card;
import com.realtimeboard.model.entity.CardActionType;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.cache.BoardCacheEvictor;
import com.realtimeboard.repository.BoardColumnRepository;
import com.realtimeboard.repository.CardRepository;
import com.realtimeboard.repository.UserRepository;
import com.realtimeboard.websocket.BoardEventService;
import com.realtimeboard.websocket.dto.BoardEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

  private final CardRepository cardRepository;
  private final BoardColumnRepository columnRepository;
  private final UserRepository userRepository;
  private final BoardAccessService boardAccessService;
  private final CardActivityService cardActivityService;
  private final BoardEventService boardEventService;
  private final BoardCacheEvictor boardCacheEvictor;

  public CardService(
      CardRepository cardRepository,
      BoardColumnRepository columnRepository,
      UserRepository userRepository,
      BoardAccessService boardAccessService,
      CardActivityService cardActivityService,
      BoardEventService boardEventService,
      BoardCacheEvictor boardCacheEvictor) {
    this.cardRepository = cardRepository;
    this.columnRepository = columnRepository;
    this.userRepository = userRepository;
    this.boardAccessService = boardAccessService;
    this.cardActivityService = cardActivityService;
    this.boardEventService = boardEventService;
    this.boardCacheEvictor = boardCacheEvictor;
  }

  @Transactional
  public CardResponse create(Long userId, CreateCardRequest req) {
    BoardColumn column = requireColumn(req.columnId());
    Long boardId = column.getBoard().getId();
    boardAccessService.requireMemberOrOwner(boardId, userId);
    User actor = requireUser(userId);

    List<Card> existing = cardRepository.findByColumnIdOrderByPositionAsc(column.getId());
    int position = req.position() == null ? existing.size() : Math.max(0, req.position());
    for (Card c : existing) {
      if (c.getPosition() >= position) c.setPosition(c.getPosition() + 1);
    }
    cardRepository.saveAll(existing);

    Card card = new Card();
    card.setColumn(column);
    card.setTitle(req.title().trim());
    card.setDescription(req.description());
    card.setAssignedUser(req.assignedUserId() == null ? null : requireUser(req.assignedUserId()));
    card.setCreatedBy(actor);
    card.setUpdatedBy(actor);
    card.setPosition(position);
    card.setCreatedAt(Instant.now());
    card.setUpdatedAt(card.getCreatedAt());
    card = cardRepository.save(card);

    cardActivityService.record(card, CardActionType.CARD_CREATED, actor, "created");
    boardCacheEvictor.evictBoardAfterCommit(boardId);
    boardEventService.publish(
        boardId,
        BoardEvent.builder()
            .type("card_created")
            .boardId(boardId)
            .actorUserId(actor.getId())
            .timestamp(Instant.now())
            .payload(toResponse(card))
            .build());

    return toResponse(card);
  }

  @Transactional
  public CardResponse update(Long userId, Long cardId, UpdateCardRequest req) {
    Card card =
        cardRepository
            .findById(cardId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card not found"));

    Long oldBoardId = card.getColumn().getBoard().getId();
    boardAccessService.requireMemberOrOwner(oldBoardId, userId);
    User actor = requireUser(userId);

    boolean moved = false;
    Long targetColumnId = req.getColumnId() == null ? card.getColumn().getId() : req.getColumnId();
    Integer targetPosition = req.getPosition();

    if (!targetColumnId.equals(card.getColumn().getId())) {
      moved = true;
      moveBetweenColumns(card, targetColumnId, targetPosition);
    } else if (targetPosition != null && targetPosition != card.getPosition()) {
      moved = true;
      reorderWithinColumn(card, Math.max(0, targetPosition));
    }

    if (req.getTitle() != null && !req.getTitle().isBlank()) {
      card.setTitle(req.getTitle().trim());
    }
    if (req.getDescription() != null) {
      String d = req.getDescription().trim();
      card.setDescription(d.isEmpty() ? null : d);
    }
    if (req.isAssignedUserIdPresent()) {
      if (req.getAssignedUserId() == null) {
        card.setAssignedUser(null);
      } else {
        card.setAssignedUser(requireUser(req.getAssignedUserId()));
      }
    }

    card.setUpdatedBy(actor);
    card.setUpdatedAt(Instant.now());
    card = cardRepository.save(card);

    CardActionType action = moved ? CardActionType.CARD_MOVED : CardActionType.CARD_UPDATED;
    cardActivityService.record(card, action, actor, moved ? "moved" : "updated");

    Long boardId = card.getColumn().getBoard().getId();
    boardCacheEvictor.evictBoardAfterCommit(boardId);
    boardEventService.publish(
        boardId,
        BoardEvent.builder()
            .type(moved ? "card_moved" : "card_updated")
            .boardId(boardId)
            .actorUserId(actor.getId())
            .timestamp(Instant.now())
            .payload(toResponse(card))
            .build());

    return toResponse(card);
  }

  @Transactional
  public void delete(Long userId, Long cardId) {
    Card card =
        cardRepository
            .findById(cardId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card not found"));

    Long boardId = card.getColumn().getBoard().getId();
    boardAccessService.requireMemberOrOwner(boardId, userId);
    User actor = requireUser(userId);

    Long columnId = card.getColumn().getId();
    int oldPos = card.getPosition();
    cardRepository.delete(card);

    List<Card> remaining = cardRepository.findByColumnIdOrderByPositionAsc(columnId);
    for (Card c : remaining) {
      if (c.getPosition() > oldPos) c.setPosition(c.getPosition() - 1);
    }
    cardRepository.saveAll(remaining);

    boardCacheEvictor.evictBoardAfterCommit(boardId);
    boardEventService.publish(
        boardId,
        BoardEvent.builder()
            .type("card_deleted")
            .boardId(boardId)
            .actorUserId(actor.getId())
            .timestamp(Instant.now())
            .payload(cardId)
            .build());
  }

  private void reorderWithinColumn(Card card, int newPos) {
    Long columnId = card.getColumn().getId();
    int oldPos = card.getPosition();
    List<Card> cards = cardRepository.findByColumnIdOrderByPositionAsc(columnId);
    for (Card c : cards) {
      if (c.getId().equals(card.getId())) continue;
      int p = c.getPosition();
      if (newPos > oldPos) {
        if (p > oldPos && p <= newPos) c.setPosition(p - 1);
      } else {
        if (p >= newPos && p < oldPos) c.setPosition(p + 1);
      }
    }
    card.setPosition(newPos);
    cardRepository.saveAll(cards);
  }

  private void moveBetweenColumns(Card card, Long targetColumnId, Integer targetPosition) {
    BoardColumn targetColumn = requireColumn(targetColumnId);
    Long sourceBoardId = card.getColumn().getBoard().getId();
    Long targetBoardId = targetColumn.getBoard().getId();
    if (!sourceBoardId.equals(targetBoardId)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "COLUMN_BOARD_MISMATCH",
          "Target column is not on this board");
    }
    Long oldColumnId = card.getColumn().getId();
    int oldPos = card.getPosition();

    // compact old column
    List<Card> oldCards = cardRepository.findByColumnIdOrderByPositionAsc(oldColumnId);
    for (Card c : oldCards) {
      if (c.getId().equals(card.getId())) continue;
      if (c.getPosition() > oldPos) c.setPosition(c.getPosition() - 1);
    }
    cardRepository.saveAll(oldCards);

    // insert into new column
    List<Card> newCards = cardRepository.findByColumnIdOrderByPositionAsc(targetColumnId);
    int newPos = targetPosition == null ? newCards.size() : Math.max(0, targetPosition);
    for (Card c : newCards) {
      if (c.getPosition() >= newPos) c.setPosition(c.getPosition() + 1);
    }
    cardRepository.saveAll(newCards);

    card.setColumn(targetColumn);
    card.setPosition(newPos);
  }

  private BoardColumn requireColumn(Long columnId) {
    return columnRepository
        .findById(columnId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COLUMN_NOT_FOUND", "Column not found"));
  }

  private User requireUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
  }

  private static CardResponse toResponse(Card card) {
    return CardResponse.builder()
        .id(card.getId())
        .columnId(card.getColumn().getId())
        .title(card.getTitle())
        .description(card.getDescription())
        .assignedUserId(card.getAssignedUser() == null ? null : card.getAssignedUser().getId())
        .createdById(card.getCreatedBy() == null ? null : card.getCreatedBy().getId())
        .updatedById(card.getUpdatedBy() == null ? null : card.getUpdatedBy().getId())
        .position(card.getPosition())
        .createdAt(card.getCreatedAt())
        .updatedAt(card.getUpdatedAt())
        .build();
  }
}

