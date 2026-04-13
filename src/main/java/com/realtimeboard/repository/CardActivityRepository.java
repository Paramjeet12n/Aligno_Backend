package com.realtimeboard.repository;

import com.realtimeboard.model.entity.CardActivity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {
  List<CardActivity> findByCardIdOrderByTimestampDesc(Long cardId);
}

