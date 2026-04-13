package com.realtimeboard.service;

import com.realtimeboard.cache.BoardCacheEvictor;
import com.realtimeboard.dto.column.ColumnDtos.ColumnResponse;
import com.realtimeboard.dto.column.ColumnDtos.CreateColumnRequest;
import com.realtimeboard.dto.column.ColumnDtos.UpdateColumnRequest;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.Board;
import com.realtimeboard.model.entity.BoardColumn;
import com.realtimeboard.repository.BoardColumnRepository;
import com.realtimeboard.websocket.BoardEventService;
import com.realtimeboard.websocket.dto.BoardEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColumnService {

  private final BoardColumnRepository columnRepository;
  private final BoardAccessService boardAccessService;
  private final BoardEventService boardEventService;
  private final BoardCacheEvictor boardCacheEvictor;

  public ColumnService(
      BoardColumnRepository columnRepository,
      BoardAccessService boardAccessService,
      BoardEventService boardEventService,
      BoardCacheEvictor boardCacheEvictor) {
    this.columnRepository = columnRepository;
    this.boardAccessService = boardAccessService;
    this.boardEventService = boardEventService;
    this.boardCacheEvictor = boardCacheEvictor;
  }

  @Transactional
  public ColumnResponse create(Long userId, CreateColumnRequest req) {
    boardAccessService.requireMemberOrOwner(req.boardId(), userId);
    Board board = boardAccessService.requireBoard(req.boardId());

    List<BoardColumn> existing = columnRepository.findByBoardIdOrderByPositionAsc(board.getId());
    int position = req.position() == null ? existing.size() : Math.max(0, req.position());

    // shift
    for (BoardColumn c : existing) {
      if (c.getPosition() >= position) c.setPosition(c.getPosition() + 1);
    }
    columnRepository.saveAll(existing);

    BoardColumn c = new BoardColumn();
    c.setBoard(board);
    c.setName(req.name().trim());
    c.setPosition(position);
    c = columnRepository.save(c);

    Long boardId = board.getId();
    boardCacheEvictor.evictBoardAfterCommit(boardId);
    publishColumnSync(boardId, userId);

    return toResponse(c);
  }

  @Transactional
  public ColumnResponse update(Long userId, Long columnId, UpdateColumnRequest req) {
    BoardColumn c =
        columnRepository
            .findById(columnId)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "COLUMN_NOT_FOUND", "Column not found"));

    Long boardId = c.getBoard().getId();
    boardAccessService.requireMemberOrOwner(boardId, userId);

    if (req.name() != null && !req.name().isBlank()) {
      c.setName(req.name().trim());
    }

    if (req.position() != null) {
      int newPos = Math.max(0, req.position());
      int oldPos = c.getPosition();
      if (newPos != oldPos) {
        List<BoardColumn> cols = columnRepository.findByBoardIdOrderByPositionAsc(boardId);
        for (BoardColumn other : cols) {
          if (other.getId().equals(c.getId())) continue;
          int p = other.getPosition();
          if (newPos > oldPos) {
            // move down: shift up those between (oldPos, newPos]
            if (p > oldPos && p <= newPos) other.setPosition(p - 1);
          } else {
            // move up: shift down those between [newPos, oldPos)
            if (p >= newPos && p < oldPos) other.setPosition(p + 1);
          }
        }
        c.setPosition(newPos);
        columnRepository.saveAll(cols);
      }
    }

    c = columnRepository.save(c);

    boardCacheEvictor.evictBoardAfterCommit(boardId);
    publishColumnSync(boardId, userId);

    return toResponse(c);
  }

  private void publishColumnSync(Long boardId, Long actorUserId) {
    List<ColumnResponse> cols =
        columnRepository.findByBoardIdOrderByPositionAsc(boardId).stream()
            .map(ColumnService::toResponse)
            .toList();
    boardEventService.publish(
        boardId,
        BoardEvent.builder()
            .type("column_sync")
            .boardId(boardId)
            .actorUserId(actorUserId)
            .timestamp(Instant.now())
            .payload(cols)
            .build());
  }

  private static ColumnResponse toResponse(BoardColumn c) {
    return ColumnResponse.builder()
        .id(c.getId())
        .boardId(c.getBoard().getId())
        .name(c.getName())
        .position(c.getPosition())
        .build();
  }
}

