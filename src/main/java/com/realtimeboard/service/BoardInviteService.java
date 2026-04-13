package com.realtimeboard.service;

import com.realtimeboard.dto.invite.InviteDtos.CreateInviteRequest;
import com.realtimeboard.dto.invite.InviteDtos.CreateInviteResponse;
import com.realtimeboard.dto.invite.InviteDtos.InvitePreviewResponse;
import com.realtimeboard.dto.invite.InviteDtos.JoinBoardResponse;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.Board;
import com.realtimeboard.model.entity.BoardInvite;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.BoardInviteRepository;
import com.realtimeboard.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardInviteService {

  private static final SecureRandom RANDOM = new SecureRandom();

  private final BoardInviteRepository inviteRepository;
  private final BoardAccessService boardAccessService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final BoardJoinRequestService boardJoinRequestService;

  public BoardInviteService(
      BoardInviteRepository inviteRepository,
      BoardAccessService boardAccessService,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      BoardJoinRequestService boardJoinRequestService) {
    this.inviteRepository = inviteRepository;
    this.boardAccessService = boardAccessService;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.boardJoinRequestService = boardJoinRequestService;
  }

  @Transactional
  public CreateInviteResponse createInvite(Long userId, Long boardId, CreateInviteRequest req) {
    boardAccessService.requireCanManageInvites(boardId, userId);
    Board board = boardAccessService.requireBoard(boardId);
    User creator =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    BoardInvite inv = new BoardInvite();
    inv.setBoard(board);
    inv.setToken(token);
    inv.setLabel(req.label() == null ? null : req.label().trim());
    inv.setPasswordHash(
        req.password() == null || req.password().isBlank()
            ? null
            : passwordEncoder.encode(req.password()));
    inv.setExpiresAt(
        req.expiresInDays() == null || req.expiresInDays() <= 0
            ? null
            : Instant.now().plus(req.expiresInDays(), ChronoUnit.DAYS));
    inv.setCreatedBy(creator);
    inv.setCreatedAt(Instant.now());
    inv.setRevoked(false);
    inviteRepository.save(inv);

    return CreateInviteResponse.builder().token(token).joinPath("/join/" + token).build();
  }

  @Transactional(readOnly = true)
  public InvitePreviewResponse preview(String token) {
    return inviteRepository
        .findByTokenAndRevokedFalse(token)
        .filter(this::notExpired)
        .map(
            inv ->
                InvitePreviewResponse.builder()
                    .boardName(inv.getBoard().getName())
                    .requiresPassword(inv.getPasswordHash() != null)
                    .active(true)
                    .build())
        .orElse(
            InvitePreviewResponse.builder()
                .boardName(null)
                .requiresPassword(false)
                .active(false)
                .build());
  }

  @Transactional
  public JoinBoardResponse join(Long userId, String token, String password) {
    BoardInvite inv =
        inviteRepository
            .findByTokenAndRevokedFalse(token)
            .filter(this::notExpired)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVITE_INVALID", "Invite is invalid or expired"));

    if (inv.getPasswordHash() != null) {
      if (password == null || !passwordEncoder.matches(password, inv.getPasswordHash())) {
        throw new ApiException(HttpStatus.UNAUTHORIZED, "INVITE_PASSWORD", "Incorrect invite password");
      }
    }

    return boardJoinRequestService.requestJoin(userId, inv);
  }

  private boolean notExpired(BoardInvite inv) {
    return inv.getExpiresAt() == null || inv.getExpiresAt().isAfter(Instant.now());
  }
}
