package com.realtimeboard.service;

import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.Board;
import com.realtimeboard.repository.BoardMemberRepository;
import com.realtimeboard.repository.BoardRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardAccessService {

  private final BoardRepository boardRepository;
  private final BoardMemberRepository boardMemberRepository;

  public BoardAccessService(BoardRepository boardRepository, BoardMemberRepository boardMemberRepository) {
    this.boardRepository = boardRepository;
    this.boardMemberRepository = boardMemberRepository;
  }

  @Transactional(readOnly = true)
  public Board requireBoard(Long boardId) {
    return boardRepository
        .findById(boardId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOARD_NOT_FOUND", "Board not found"));
  }

  /** Caller must have already verified membership (e.g. requireMemberOrOwner). */
  @Transactional(readOnly = true)
  public String viewerRole(Board board, Long userId) {
    if (board.getOwner().getId().equals(userId)) {
      return "OWNER";
    }
    return boardMemberRepository
        .findByBoardIdAndUserId(board.getId(), userId)
        .map(m -> m.getRole().name())
        .orElse("MEMBER");
  }

  /**
   * Only the board owner can create invite links so the team access is controlled by the creator.
   */
  @Transactional(readOnly = true)
  public boolean canUserManageInvites(Long boardId, Long userId) {
    Board b = requireBoard(boardId);
    return b.getOwner().getId().equals(userId);
  }

  @Transactional(readOnly = true)
  public void requireMemberOrOwner(Long boardId, Long userId) {
    Board b = requireBoard(boardId);
    if (b.getOwner().getId().equals(userId)) return;
    if (boardMemberRepository.existsByBoardIdAndUserId(boardId, userId)) return;
    throw new ApiException(HttpStatus.FORBIDDEN, "NOT_A_MEMBER", "You are not a member of this board");
  }

  @Transactional(readOnly = true)
  public void requireCanManageInvites(Long boardId, Long userId) {
    if (!canUserManageInvites(boardId, userId)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Only the board owner can create invite links");
    }
  }

  @Transactional(readOnly = true)
  public void requireOwner(Long boardId, Long userId) {
    Board b = requireBoard(boardId);
    if (!b.getOwner().getId().equals(userId)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Only the board owner can perform this action");
    }
  }

  @Transactional(readOnly = true)
  public void requireAdminOrOwner(Long boardId, Long userId) {
    Board b = requireBoard(boardId);
    if (b.getOwner().getId().equals(userId)) return;
    
    boolean isAdmin = boardMemberRepository
        .findByBoardIdAndUserId(boardId, userId)
        .map(m -> m.getRole().name().equals("ADMIN"))
        .orElse(false);
        
    if (!isAdmin) {
      throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ADMIN", "Only board admins or the owner can perform this action");
    }
  }
}
