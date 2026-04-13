package com.realtimeboard.repository;

import com.realtimeboard.model.entity.BoardColumn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {
  List<BoardColumn> findByBoardIdOrderByPositionAsc(Long boardId);

  @Query(
      """
      SELECT c FROM BoardColumn c JOIN FETCH c.board b WHERE b.id = :boardId ORDER BY c.position ASC
      """)
  List<BoardColumn> findByBoardIdOrderByPositionAscWithBoard(@Param("boardId") Long boardId);
}

