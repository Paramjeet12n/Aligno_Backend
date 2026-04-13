package com.realtimeboard.dto.column;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

public final class ColumnDtos {
  private ColumnDtos() {}

  public record CreateColumnRequest(@NotNull Long boardId, @NotBlank String name, Integer position) {}

  public record UpdateColumnRequest(String name, Integer position) {}

  @Value
  @Builder
  public static class ColumnResponse {
    Long id;
    Long boardId;
    String name;
    Integer position;
  }
}

