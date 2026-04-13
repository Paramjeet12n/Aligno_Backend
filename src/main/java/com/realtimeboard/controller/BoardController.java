package com.realtimeboard.controller;

import com.realtimeboard.dto.board.BoardDtos.BoardDetailResponse;
import com.realtimeboard.dto.board.BoardDtos.BoardSummaryResponse;
import com.realtimeboard.dto.board.BoardDtos.CreateBoardRequest;
import com.realtimeboard.dto.invite.InviteDtos.CreateInviteRequest;
import com.realtimeboard.dto.invite.InviteDtos.CreateInviteResponse;
import com.realtimeboard.dto.join.JoinRequestDtos.JoinRequestResponse;
import com.realtimeboard.security.CurrentUser;
import com.realtimeboard.service.BoardInviteService;
import com.realtimeboard.service.BoardJoinRequestService;
import com.realtimeboard.service.BoardService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/boards")
public class BoardController {

  private final BoardService boardService;
  private final BoardInviteService boardInviteService;
  private final BoardJoinRequestService boardJoinRequestService;

  public BoardController(
      BoardService boardService,
      BoardInviteService boardInviteService,
      BoardJoinRequestService boardJoinRequestService) {
    this.boardService = boardService;
    this.boardInviteService = boardInviteService;
    this.boardJoinRequestService = boardJoinRequestService;
  }

  @GetMapping
  public List<BoardSummaryResponse> list() {
    return boardService.listBoards(CurrentUser.requireId());
  }

  @PostMapping
  public BoardSummaryResponse create(@Valid @RequestBody CreateBoardRequest req) {
    return boardService.createBoard(CurrentUser.requireId(), req);
  }

  @GetMapping("/{id}")
  public BoardDetailResponse get(@PathVariable Long id) {
    return boardService.getBoard(CurrentUser.requireId(), id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    boardService.deleteBoard(CurrentUser.requireId(), id);
  }

  @PostMapping("/{boardId}/invites")
  public CreateInviteResponse createInvite(
      @PathVariable Long boardId, @RequestBody(required = false) CreateInviteRequest req) {
    CreateInviteRequest body = req == null ? new CreateInviteRequest(null, null, null) : req;
    return boardInviteService.createInvite(CurrentUser.requireId(), boardId, body);
  }

  /** Remove a member from the board. Only the board owner can do this. */
  @DeleteMapping("/{boardId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void kickMember(@PathVariable Long boardId, @PathVariable Long userId) {
    boardService.kickMember(CurrentUser.requireId(), boardId, userId);
  }

  /** Owner-only: pending join requests requiring approval */
  @GetMapping("/{boardId}/join-requests")
  public List<JoinRequestResponse> listJoinRequests(@PathVariable Long boardId) {
    return boardJoinRequestService.listPending(CurrentUser.requireId(), boardId);
  }

  @PostMapping("/{boardId}/join-requests/{requestId}/approve")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void approveJoinRequest(@PathVariable Long boardId, @PathVariable Long requestId) {
    boardJoinRequestService.approve(CurrentUser.requireId(), boardId, requestId);
  }

  @PostMapping("/{boardId}/join-requests/{requestId}/reject")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void rejectJoinRequest(@PathVariable Long boardId, @PathVariable Long requestId) {
    boardJoinRequestService.reject(CurrentUser.requireId(), boardId, requestId);
  }
}
