package com.realtimeboard.repository;

import com.realtimeboard.model.entity.BoardInvite;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardInviteRepository extends JpaRepository<BoardInvite, Long> {
  Optional<BoardInvite> findByTokenAndRevokedFalse(String token);
}
