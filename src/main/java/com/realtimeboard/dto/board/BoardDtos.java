package com.realtimeboard.dto.board;

import com.realtimeboard.dto.card.CardDtos.CardResponse;
import com.realtimeboard.dto.column.ColumnDtos.ColumnResponse;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

public final class BoardDtos {
  private BoardDtos() {}

  public record CreateBoardRequest(@NotBlank String name) {}

  @Value
  @Builder
  public static class BoardSummaryResponse {
    Long id;
    String name;
    Instant createdAt;
    Long ownerId;
  }

  @Value
  @Builder
  public static class BoardDetailResponse {
    Long id;
    String name;
    Instant createdAt;
    Long ownerId;
    /** OWNER, ADMIN, or MEMBER — board membership role (not app-wide admin). */
    String yourRole;
    /** True if this user may create invite links (any board member or owner). */
    boolean canManageInvites;
    /** True if the requesting user is the board owner. */
    @com.fasterxml.jackson.annotation.JsonProperty("isOwner")
    boolean owner;
    List<ColumnWithCards> columns;
    List<MemberResponse> members;
  }

  @Value
  @Builder
  public static class ColumnWithCards {
    ColumnResponse column;
    List<CardResponse> cards;
  }

  @Value
  @Builder
  public static class MemberResponse {
    Long userId;
    String name;
    String email;
    String role;
  }
}


