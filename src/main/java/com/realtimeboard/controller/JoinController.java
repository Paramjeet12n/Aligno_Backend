package com.realtimeboard.controller;

import com.realtimeboard.dto.invite.InviteDtos.InvitePreviewResponse;
import com.realtimeboard.dto.invite.InviteDtos.JoinBoardRequest;
import com.realtimeboard.dto.invite.InviteDtos.JoinBoardResponse;
import com.realtimeboard.security.CurrentUser;
import com.realtimeboard.service.BoardInviteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/join")
public class JoinController {

  private final BoardInviteService boardInviteService;

  public JoinController(BoardInviteService boardInviteService) {
    this.boardInviteService = boardInviteService;
  }

  @GetMapping("/{token}/preview")
  public InvitePreviewResponse preview(@PathVariable String token) {
    return boardInviteService.preview(token);
  }

  @PostMapping
  public JoinBoardResponse join(@Valid @RequestBody JoinBoardRequest req) {
    return boardInviteService.join(CurrentUser.requireId(), req.token(), req.password());
  }
}
