package com.realtimeboard.repository;

import com.realtimeboard.model.entity.BoardJoinRequest;
import com.realtimeboard.model.entity.BoardJoinRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardJoinRequestRepository extends JpaRepository<BoardJoinRequest, Long> {

  @Query(
      "SELECT r FROM BoardJoinRequest r JOIN FETCH r.user WHERE r.board.id = :boardId AND r.status = :status ORDER BY r.requestedAt ASC")
  List<BoardJoinRequest> findByBoardIdAndStatusWithUser(
      @Param("boardId") Long boardId, @Param("status") BoardJoinRequestStatus status);

  Optional<BoardJoinRequest> findFirstByBoardIdAndUserIdAndStatus(
      Long boardId, Long userId, BoardJoinRequestStatus status);
}

