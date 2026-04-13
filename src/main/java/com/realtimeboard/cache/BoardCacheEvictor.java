package com.realtimeboard.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class BoardCacheEvictor {

  public static final String BOARDS = "boards";

  private final CacheManager cacheManager;

  public BoardCacheEvictor(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /** Evict after successful commit so readers cannot repopulate the cache with pre-commit data. */
  public void evictBoardAfterCommit(Long boardId) {
    if (boardId == null) return;
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      evictBoard(boardId);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            evictBoard(boardId);
          }
        });
  }

  void evictBoard(Long boardId) {
    Cache cache = cacheManager.getCache(BOARDS);
    if (cache == null) {
      return;
    }
    if (cache instanceof CaffeineCache cc) {
      String prefix = "board:" + boardId + ":user:";
      cc.getNativeCache().asMap().keySet().removeIf(k -> String.valueOf(k).startsWith(prefix));
      return;
    }
    cache.clear();
  }
}
