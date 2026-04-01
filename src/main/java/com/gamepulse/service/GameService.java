package com.gamepulse.service;

import com.gamepulse.domain.alert.AlertRepository;
import com.gamepulse.domain.alert.PriceAlert;
import com.gamepulse.domain.game.*;
import com.gamepulse.infra.cache.GameCacheService;
import com.gamepulse.infra.kafka.GameEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final GamePriceRepository gamePriceRepository;
    private final AlertRepository alertRepository;
    private final GameCacheService cacheService;
    private final GameEventProducer eventProducer;

    public GameService(GameRepository gameRepository,
                       GamePriceRepository gamePriceRepository,
                       AlertRepository alertRepository,
                       GameCacheService cacheService,
                       GameEventProducer eventProducer) {
        this.gameRepository = gameRepository;
        this.gamePriceRepository = gamePriceRepository;
        this.alertRepository = alertRepository;
        this.cacheService = cacheService;
        this.eventProducer = eventProducer;
    }

    // 게임 조회 — Redis 캐시 우선
    public Game getGame(Long appId) {
        Integer cachedPrice = cacheService.getCachedPrice(appId);
        Game game = gameRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + appId));

        // 캐시에 없으면 현재 가격을 캐시에 저장
        if (cachedPrice == null) {
            cacheService.cachePrice(appId, game.getCurrentPrice());
        }
        return game;
    }

    // 키워드 검색
    public List<Game> search(String keyword) {
        return gameRepository.findByTitleContainingIgnoreCase(keyword);
    }

    // 가격 이력
    public List<GamePrice> getPriceHistory(Long appId) {
        return gamePriceRepository
                .findByGameSteamAppIdOrderByRecordedAtDesc(appId);
    }

    // 전체 게임 목록 (스케줄러용)
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    // 취향 기반 추천 — 같은 장르 게임 반환 (ES 연동 전 임시)
    public List<Game> recommend(List<Long> likedAppIds) {
        return likedAppIds.stream()
                .map(id -> gameRepository.findById(id).orElse(null))
                .filter(g -> g != null)
                .flatMap(g -> gameRepository
                        .findByTitleContainingIgnoreCase(
                                g.getGenre() != null ? g.getGenre() : "")
                        .stream())
                .distinct()
                .limit(10)
                .toList();
    }

    // 가격 업데이트
    @Transactional
    public void updatePrice(Game game, Integer newPrice) {
        Integer oldPrice = game.getCurrentPrice();
        game.updatePrice(newPrice);
        gameRepository.save(game);

        // 가격 이력 저장
        gamePriceRepository.save(new GamePrice(game, newPrice));

        // Redis 캐시 갱신
        cacheService.cachePrice(game.getSteamAppId(), newPrice);

        // Kafka 이벤트 발행
        eventProducer.sendPriceEvent(game.getSteamAppId(), oldPrice, newPrice);

        // 알림 확인
        checkAlerts(game, newPrice);
    }

    // 알림 등록
    @Transactional
    public void setAlert(Long appId, Integer targetPrice, String email) {
        Game game = gameRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Game not found: " + appId));
        alertRepository.save(new PriceAlert(game, email, targetPrice));
    }

    // 최저가 달성 시 알림 처리
    private void checkAlerts(Game game, Integer newPrice) {
        List<PriceAlert> alerts = alertRepository
                .findByGameSteamAppIdAndNotifiedFalse(game.getSteamAppId());
        alerts.stream()
                .filter(alert -> newPrice <= alert.getTargetPrice())
                .forEach(alert -> {
                    // 실제 이메일 발송은 Kafka Consumer에서 처리
                    eventProducer.sendPriceEvent(
                            game.getSteamAppId(),
                            game.getCurrentPrice(),
                            newPrice
                    );
                    alert.markNotified();
                    alertRepository.save(alert);
                });
    }
}