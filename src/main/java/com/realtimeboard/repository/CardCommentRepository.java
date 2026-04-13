package com.realtimeboard.repository;

import com.realtimeboard.model.entity.CardComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardCommentRepository extends JpaRepository<CardComment, Long> {
  
  @Query("SELECT c FROM CardComment c JOIN FETCH c.user WHERE c.card.id = :cardId ORDER BY c.timestamp ASC")
  List<CardComment> findAllByCardIdWithUser(@Param("cardId") Long cardId);
}
