package com.realtimeboard.service;

import com.realtimeboard.dto.card.CardDtos.CommentResponse;
import com.realtimeboard.dto.card.CardDtos.CreateCommentRequest;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.Card;
import com.realtimeboard.model.entity.CardActionType;
import com.realtimeboard.model.entity.CardComment;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.CardCommentRepository;
import com.realtimeboard.repository.CardRepository;
import com.realtimeboard.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardCommentService {

  private final CardCommentRepository commentRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;
  private final CardActivityService cardActivityService;

  public CardCommentService(
      CardCommentRepository commentRepository,
      CardRepository cardRepository,
      UserRepository userRepository,
      CardActivityService cardActivityService) {
    this.commentRepository = commentRepository;
    this.cardRepository = cardRepository;
    this.userRepository = userRepository;
    this.cardActivityService = cardActivityService;
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> getCommentsForCard(Long cardId) {
    return commentRepository.findAllByCardIdWithUser(cardId).stream()
        .map(CardCommentService::toResponse)
        .toList();
  }

  @Transactional
  public CommentResponse addComment(Long userId, Long cardId, CreateCommentRequest req) {
    Card card =
        cardRepository
            .findById(cardId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card not found"));
            
    User author =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

    CardComment comment = new CardComment();
    comment.setCard(card);
    comment.setUser(author);
    comment.setText(req.text().trim());
    comment.setTimestamp(Instant.now());
    comment = commentRepository.save(comment);

    // Record activity
    cardActivityService.record(
        card,
        CardActionType.COMMENT_ADDED,
        author,
        "User " + author.getName() + " added a comment");

    return toResponse(comment);
  }

  private static CommentResponse toResponse(CardComment c) {
    return CommentResponse.builder()
        .id(c.getId())
        .cardId(c.getCard().getId())
        .userId(c.getUser().getId())
        .userName(c.getUser().getName())
        .text(c.getText())
        .timestamp(c.getTimestamp())
        .build();
  }
}
