package com.realtimeboard.service;

import com.realtimeboard.dto.invite.InviteDtos.JoinBoardResponse;
import com.realtimeboard.dto.join.JoinRequestDtos.JoinRequestResponse;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.Board;
import com.realtimeboard.model.entity.BoardInvite;
import com.realtimeboard.model.entity.BoardJoinRequest;
import com.realtimeboard.model.entity.BoardJoinRequestStatus;
import com.realtimeboard.model.entity.BoardMember;
import com.realtimeboard.model.entity.BoardMemberRole;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.BoardJoinRequestRepository;
import com.realtimeboard.repository.BoardMemberRepository;
import com.realtimeboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardJoinRequestService {

  private final BoardJoinRequestRepository joinRequestRepository;
  private final BoardMemberRepository boardMemberRepository;
  private final BoardAccessService boardAccessService;
  private final UserRepository userRepository;

  public BoardJoinRequestService(
      BoardJoinRequestRepository joinRequestRepository,
      BoardMemberRepository boardMemberRepository,
      BoardAccessService boardAccessService,
      UserRepository userRepository) {
    this.joinRequestRepository = joinRequestRepository;
    this.boardMemberRepository = boardMemberRepository;
    this.boardAccessService = boardAccessService;
    this.userRepository = userRepository;
  }

  @Transactional
  public JoinBoardResponse requestJoin(Long userId, BoardInvite inv) {
    Board board = inv.getBoard();
    Long boardId = board.getId();

    if (board.getOwner().getId().equals(userId) || boardMemberRepository.existsByBoardIdAndUserId(boardId, userId)) {
      return JoinBoardResponse.builder()
          .boardId(boardId)
          .boardName(board.getName())
          .status("ALREADY_MEMBER")
          .build();
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

    // Ensure a single pending request (unique partial index enforces this too).
    BoardJoinRequest req =
        joinRequestRepository
            .findFirstByBoardIdAndUserIdAndStatus(boardId, userId, BoardJoinRequestStatus.PENDING)
            .orElseGet(BoardJoinRequest::new);

    req.setBoard(board);
    req.setUser(user);
    req.setInvite(inv);
    req.setStatus(BoardJoinRequestStatus.PENDING);
    req.setRequestedAt(Instant.now());
    req.setRespondedAt(null);
    req.setRespondedBy(null);
    joinRequestRepository.save(req);

    return JoinBoardResponse.builder()
        .boardId(boardId)
        .boardName(board.getName())
        .status("PENDING_APPROVAL")
        .build();
  }

  @Transactional(readOnly = true)
  public List<JoinRequestResponse> listPending(Long ownerUserId, Long boardId) {
    boardAccessService.requireOwner(boardId, ownerUserId);
    return joinRequestRepository.findByBoardIdAndStatusWithUser(boardId, BoardJoinRequestStatus.PENDING).stream()
        .map(
            r ->
                JoinRequestResponse.builder()
                    .id(r.getId())
                    .userId(r.getUser().getId())
                    .userName(r.getUser().getName())
                    .userEmail(r.getUser().getEmail())
                    .requestedAt(r.getRequestedAt())
                    .build())
        .toList();
  }

  @Transactional
  public void approve(Long ownerUserId, Long boardId, Long requestId) {
    boardAccessService.requireOwner(boardId, ownerUserId);
    BoardJoinRequest r =
        joinRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "Join request not found"));
    if (!r.getBoard().getId().equals(boardId)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "JOIN_REQUEST_BOARD_MISMATCH", "Join request does not match board");
    }
    if (r.getStatus() != BoardJoinRequestStatus.PENDING) return;

    if (!boardMemberRepository.existsByBoardIdAndUserId(boardId, r.getUser().getId())
        && !r.getBoard().getOwner().getId().equals(r.getUser().getId())) {
      BoardMember m = new BoardMember();
      m.setBoard(r.getBoard());
      m.setUser(r.getUser());
      m.setRole(BoardMemberRole.MEMBER);
      boardMemberRepository.save(m);
    }

    User owner = userRepository.findById(ownerUserId).orElse(null);
    r.setStatus(BoardJoinRequestStatus.APPROVED);
    r.setRespondedAt(Instant.now());
    r.setRespondedBy(owner);
    joinRequestRepository.save(r);
  }

  @Transactional
  public void reject(Long ownerUserId, Long boardId, Long requestId) {
    boardAccessService.requireOwner(boardId, ownerUserId);
    BoardJoinRequest r =
        joinRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new ApiException(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND", "Join request not found"));
    if (!r.getBoard().getId().equals(boardId)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "JOIN_REQUEST_BOARD_MISMATCH", "Join request does not match board");
    }
    if (r.getStatus() != BoardJoinRequestStatus.PENDING) return;

    User owner = userRepository.findById(ownerUserId).orElse(null);
    r.setStatus(BoardJoinRequestStatus.REJECTED);
    r.setRespondedAt(Instant.now());
    r.setRespondedBy(owner);
    joinRequestRepository.save(r);
  }
}

