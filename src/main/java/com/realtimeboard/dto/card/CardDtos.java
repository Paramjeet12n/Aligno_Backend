package com.realtimeboard.dto.card;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

public final class CardDtos {
  private CardDtos() {}

  public record CreateCardRequest(
      @NotNull Long columnId,
      @NotBlank String title,
      String description,
      Long assignedUserId,
      Integer position) {}

  /**
   * Mutable PATCH body so {@code assignedUserId} can be tri-state: omitted (no change), JSON {@code null}
   * (clear assignee), or a user id.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UpdateCardRequest {
    private String title;
    private String description;
    private Long assignedUserId;
    private boolean assignedUserIdPresent;
    private Long columnId;
    private Integer position;

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public Long getAssignedUserId() {
      return assignedUserId;
    }

    @JsonProperty("assignedUserId")
    public void setAssignedUserId(Long assignedUserId) {
      this.assignedUserId = assignedUserId;
      this.assignedUserIdPresent = true;
    }

    public boolean isAssignedUserIdPresent() {
      return assignedUserIdPresent;
    }

    public Long getColumnId() {
      return columnId;
    }

    public void setColumnId(Long columnId) {
      this.columnId = columnId;
    }

    public Integer getPosition() {
      return position;
    }

    public void setPosition(Integer position) {
      this.position = position;
    }
  }

  @Value
  @Builder
  public static class CardResponse {
    Long id;
    Long columnId;
    String title;
    String description;
    Long assignedUserId;
    Long createdById;
    Long updatedById;
    Integer position;
    Instant createdAt;
    Instant updatedAt;
  }

  public record CreateCommentRequest(@NotBlank String text) {}

  @Value
  @Builder
  public static class CommentResponse {
    Long id;
    Long cardId;
    Long userId;
    String userName;
    String text;
    Instant timestamp;
  }
}

