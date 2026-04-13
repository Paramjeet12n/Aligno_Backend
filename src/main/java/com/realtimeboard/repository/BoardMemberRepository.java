package com.realtimeboard.repository;

import com.realtimeboard.model.entity.BoardMember;
import com.realtimeboard.model.entity.BoardMemberRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {
  Optional<BoardMember> findByBoardIdAndUserId(Long boardId, Long userId);
  boolean existsByBoardIdAndUserId(Long boardId, Long userId);
  boolean existsByBoardIdAndUserIdAndRole(Long boardId, Long userId, BoardMemberRole role);

  @Query("SELECT bm FROM BoardMember bm JOIN FETCH bm.user WHERE bm.board.id = :boardId")
  List<BoardMember> findAllByBoardIdWithUser(@Param("boardId") Long boardId);

  void deleteByBoardIdAndUserId(Long boardId, Long userId);
}


