package com.realtimeboard.repository;

import com.realtimeboard.model.entity.Card;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long> {
  List<Card> findByColumnIdOrderByPositionAsc(Long columnId);

  @Query(
      """
      SELECT c FROM Card c
      JOIN FETCH c.column col
      LEFT JOIN FETCH c.assignedUser
      WHERE col.board.id = :boardId
      ORDER BY col.position ASC, c.position ASC
      """)
  List<Card> findAllByBoardIdForDetail(@Param("boardId") Long boardId);

  /** Decrement positions above a removed card's position in a column (compact the gap). */
  @Modifying
  @Query("UPDATE Card c SET c.position = c.position - 1 WHERE c.column.id = :columnId AND c.position > :removedPosition")
  void compactPositionsAbove(@Param("columnId") Long columnId, @Param("removedPosition") int removedPosition);

  /** Increment positions from a given position onward to make room for an insert. */
  @Modifying
  @Query("UPDATE Card c SET c.position = c.position + 1 WHERE c.column.id = :columnId AND c.position >= :insertPosition")
  void shiftPositionsFrom(@Param("columnId") Long columnId, @Param("insertPosition") int insertPosition);

  /** Shift positions down in a range (for reorder within same column: moving card to higher index). */
  @Modifying
  @Query("UPDATE Card c SET c.position = c.position - 1 WHERE c.column.id = :columnId AND c.id <> :cardId AND c.position > :oldPos AND c.position <= :newPos")
  void shiftDownRange(@Param("columnId") Long columnId, @Param("cardId") Long cardId, @Param("oldPos") int oldPos, @Param("newPos") int newPos);

  /** Shift positions up in a range (for reorder within same column: moving card to lower index). */
  @Modifying
  @Query("UPDATE Card c SET c.position = c.position + 1 WHERE c.column.id = :columnId AND c.id <> :cardId AND c.position >= :newPos AND c.position < :oldPos")
  void shiftUpRange(@Param("columnId") Long columnId, @Param("cardId") Long cardId, @Param("newPos") int newPos, @Param("oldPos") int oldPos);

  /** Count cards in a column (for computing insert position at end). */
  int countByColumnId(Long columnId);
}


