package com.realtimeboard.service;

import com.realtimeboard.dto.board.BoardDtos.BoardDetailResponse;
import com.realtimeboard.dto.board.BoardDtos.BoardSummaryResponse;
import com.realtimeboard.dto.board.BoardDtos.ColumnWithCards;
import com.realtimeboard.dto.board.BoardDtos.CreateBoardRequest;
import com.realtimeboard.dto.board.BoardDtos.MemberResponse;
import com.realtimeboard.dto.card.CardDtos.CardResponse;
import com.realtimeboard.dto.column.ColumnDtos.ColumnResponse;
import com.realtimeboard.exception.ApiException;
import com.realtimeboard.model.entity.Board;
import com.realtimeboard.model.entity.BoardColumn;
import com.realtimeboard.model.entity.BoardMember;
import com.realtimeboard.model.entity.BoardMemberRole;
import com.realtimeboard.model.entity.Card;
import com.realtimeboard.model.entity.User;
import com.realtimeboard.repository.BoardColumnRepository;
import com.realtimeboard.repository.BoardMemberRepository;
import com.realtimeboard.repository.BoardRepository;
import com.realtimeboard.repository.CardRepository;
import com.realtimeboard.repository.UserRepository;
import com.realtimeboard.cache.BoardCacheEvictor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

  private final BoardRepository boardRepository;
  private final BoardMemberRepository boardMemberRepository;
  private final BoardColumnRepository boardColumnRepository;
  private final CardRepository cardRepository;
  private final UserRepository userRepository;
  private final BoardAccessService boardAccessService;

  public BoardService(
      BoardRepository boardRepository,
      BoardMemberRepository boardMemberRepository,
      BoardColumnRepository boardColumnRepository,
      CardRepository cardRepository,
      UserRepository userRepository,
      BoardAccessService boardAccessService) {
    this.boardRepository = boardRepository;
    this.boardMemberRepository = boardMemberRepository;
    this.boardColumnRepository = boardColumnRepository;
    this.cardRepository = cardRepository;
    this.userRepository = userRepository;
    this.boardAccessService = boardAccessService;
  }

  @Transactional(readOnly = true)
  public List<BoardSummaryResponse> listBoards(Long userId) {
    return boardRepository.findAllForUser(userId).stream().map(BoardService::toSummary).toList();
  }

  @Transactional
  public BoardSummaryResponse createBoard(Long userId, CreateBoardRequest req) {
    User owner =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User not found"));

    Board b = new Board();
    b.setName(req.name().trim());
    b.setOwner(owner);
    b.setCreatedAt(Instant.now());
    b = boardRepository.save(b);

    BoardMember bm = new BoardMember();
    bm.setBoard(b);
    bm.setUser(owner);
    bm.setRole(BoardMemberRole.ADMIN);
    boardMemberRepository.save(bm);

    // seed basic columns
    seedDefaultColumns(b);

    return toSummary(b);
  }

  @Cacheable(
      cacheNames = BoardCacheEvictor.BOARDS,
      key = "'board:' + #boardId + ':user:' + #userId",
      sync = true)
  @Transactional(readOnly = true)
  public BoardDetailResponse getBoard(Long userId, Long boardId) {
    Board b = boardAccessService.requireBoard(boardId);
    boardAccessService.requireMemberOrOwner(boardId, userId);
    String yourRole = boardAccessService.viewerRole(b, userId);
    boolean canManageInvites = boardAccessService.canUserManageInvites(boardId, userId);
    boolean isOwner = b.getOwner().getId().equals(userId);

    List<BoardColumn> columns =
        boardColumnRepository.findByBoardIdOrderByPositionAscWithBoard(boardId);
    List<Card> allCards = cardRepository.findAllByBoardIdForDetail(boardId);

    Map<Long, List<Card>> cardsByColumn = new LinkedHashMap<>();
    for (Card card : allCards) {
      cardsByColumn
          .computeIfAbsent(card.getColumn().getId(), k -> new ArrayList<>())
          .add(card);
    }

    List<ColumnWithCards> colWithCards = new ArrayList<>();
    for (BoardColumn c : columns) {
      List<Card> cards = cardsByColumn.getOrDefault(c.getId(), List.of());
      colWithCards.add(
          ColumnWithCards.builder()
              .column(toColumnResponse(c))
              .cards(cards.stream().map(BoardService::toCardResponse).toList())
              .build());
    }

    // Build members list
    List<BoardMember> boardMembers = boardMemberRepository.findAllByBoardIdWithUser(boardId);
    List<MemberResponse> memberResponses = new ArrayList<>();

    // Add owner first
    User owner = b.getOwner();
    memberResponses.add(MemberResponse.builder()
        .userId(owner.getId())
        .name(owner.getName())
        .email(owner.getEmail())
        .role("OWNER")
        .build());

    // Add other members (skip owner if they're also in the members table)
    for (BoardMember bm : boardMembers) {
      if (bm.getUser().getId().equals(owner.getId())) continue;
      memberResponses.add(MemberResponse.builder()
          .userId(bm.getUser().getId())
          .name(bm.getUser().getName())
          .email(bm.getUser().getEmail())
          .role(bm.getRole().name())
          .build());
    }

    return BoardDetailResponse.builder()
        .id(b.getId())
        .name(b.getName())
        .createdAt(b.getCreatedAt())
        .ownerId(b.getOwner().getId())
        .yourRole(yourRole)
        .canManageInvites(canManageInvites)
        .owner(isOwner)
        .columns(colWithCards)
        .members(memberResponses)
        .build();
  }

  @Transactional
  public void kickMember(Long actorUserId, Long boardId, Long targetUserId) {
    Board b = boardAccessService.requireBoard(boardId);

    // Only the board owner can kick members
    if (!b.getOwner().getId().equals(actorUserId)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "NOT_OWNER", "Only the board owner can remove members");
    }

    // Can't kick yourself (the owner)
    if (actorUserId.equals(targetUserId)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_KICK_SELF", "You cannot remove yourself from your own board");
    }

    // Verify target is actually a member
    if (!boardMemberRepository.existsByBoardIdAndUserId(boardId, targetUserId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "User is not a member of this board");
    }

    boardMemberRepository.deleteByBoardIdAndUserId(boardId, targetUserId);
  }

  @Transactional
  public void deleteBoard(Long userId, Long boardId) {
    Board b = boardAccessService.requireBoard(boardId);
    boardAccessService.requireAdminOrOwner(boardId, userId);
    boardRepository.delete(b);
  }

  private void seedDefaultColumns(Board b) {
    String[] names = {"To do", "Doing", "Done"};
    for (int i = 0; i < names.length; i++) {
      BoardColumn c = new BoardColumn();
      c.setBoard(b);
      c.setName(names[i]);
      c.setPosition(i);
      boardColumnRepository.save(c);
    }
  }

  private static BoardSummaryResponse toSummary(Board b) {
    return BoardSummaryResponse.builder()
        .id(b.getId())
        .name(b.getName())
        .createdAt(b.getCreatedAt())
        .ownerId(b.getOwner().getId())
        .build();
  }

  private static ColumnResponse toColumnResponse(BoardColumn c) {
    return ColumnResponse.builder()
        .id(c.getId())
        .boardId(c.getBoard().getId())
        .name(c.getName())
        .position(c.getPosition())
        .build();
  }

  private static CardResponse toCardResponse(Card card) {
    return CardResponse.builder()
        .id(card.getId())
        .columnId(card.getColumn().getId())
        .title(card.getTitle())
        .description(card.getDescription())
        .assignedUserId(card.getAssignedUser() == null ? null : card.getAssignedUser().getId())
        .createdById(card.getCreatedBy() == null ? null : card.getCreatedBy().getId())
        .updatedById(card.getUpdatedBy() == null ? null : card.getUpdatedBy().getId())
        .position(card.getPosition())
        .createdAt(card.getCreatedAt())
        .updatedAt(card.getUpdatedAt())
        .build();
  }
}


