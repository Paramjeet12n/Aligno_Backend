package com.realtimeboard.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  private final JdbcTemplate jdbcTemplate;

  public HealthController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Liveness for uptime monitors (e.g. Render ping every 5m). No auth, not rate-limited. Returns 200 as
   * soon as the app is serving — does not touch the DB so cold starts and pool issues do not fail the
   * probe.
   */
  @GetMapping("/health")
  public Map<String, Object> health() {
    return Map.of(
        "status", "UP",
        "service", "realtimeboard-backend",
        "timestamp", Instant.now().toString());
  }

  /**
   * Optional readiness: verifies DB connectivity. Use for stricter checks; returns 503 if DB is
   * unreachable.
   */
  @GetMapping("/health/ready")
  public ResponseEntity<Map<String, Object>> ready() {
    try {
      jdbcTemplate.queryForObject("select 1", Integer.class);
      return ResponseEntity.ok(
          Map.of("status", "UP", "db", "UP", "timestamp", Instant.now().toString()));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("status", "DOWN", "db", "DOWN", "timestamp", Instant.now().toString()));
    }
  }
}
