package com.realtimeboard.dto.invite;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

public final class InviteDtos {
  private InviteDtos() {}

  public record CreateInviteRequest(String password, String label, Integer expiresInDays) {}

  @Value
  @Builder
  public static class CreateInviteResponse {
    String token;
    String joinPath;
  }

  @Value
  @Builder
  public static class InvitePreviewResponse {
    String boardName;
    boolean requiresPassword;
    boolean active;
  }

  public record JoinBoardRequest(@NotBlank String token, String password) {}

  @Value
  @Builder
  public static class JoinBoardResponse {
    Long boardId;
    String boardName;
    /** ALREADY_MEMBER | PENDING_APPROVAL */
    String status;
  }
}
