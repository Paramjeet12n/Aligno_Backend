package com.realtimeboard.repository;

import com.realtimeboard.model.entity.Board;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

  @Query(
      """
      select distinct b
      from Board b
      left join BoardMember bm on bm.board = b
      where b.owner.id = :userId or bm.user.id = :userId
      """)
  List<Board> findAllForUser(@Param("userId") Long userId);
}

