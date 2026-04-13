package com.realtimeboard.controller;

import com.realtimeboard.dto.column.ColumnDtos.ColumnResponse;
import com.realtimeboard.dto.column.ColumnDtos.CreateColumnRequest;
import com.realtimeboard.dto.column.ColumnDtos.UpdateColumnRequest;
import com.realtimeboard.security.CurrentUser;
import com.realtimeboard.service.ColumnService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/columns")
public class ColumnController {

  private final ColumnService columnService;

  public ColumnController(ColumnService columnService) {
    this.columnService = columnService;
  }

  @PostMapping
  public ColumnResponse create(@Valid @RequestBody CreateColumnRequest req) {
    return columnService.create(CurrentUser.requireId(), req);
  }

  @PatchMapping("/{id}")
  public ColumnResponse update(
      @PathVariable Long id, @RequestBody(required = false) UpdateColumnRequest req) {
    if (req == null) {
      req = new UpdateColumnRequest(null, null);
    }
    return columnService.update(CurrentUser.requireId(), id, req);
  }
}

