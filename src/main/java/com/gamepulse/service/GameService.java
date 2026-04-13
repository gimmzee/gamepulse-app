package com.gamepulse.service;

import com.gamepulse.domain.alert.AlertRepository;
import com.gamepulse.domain.alert.PriceAlert;
import com.gamepulse.domain.game.*;
import com.gamepulse.infra.cache.GameCacheService;
import com.gamepulse.infra.es.GameEsRepository;
import com.gamepulse.infra.kafka.GameEventProducer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;
    private final GamePriceRepository gamePriceRepository;
    private final AlertRepository alertRepository;
    private final GameCacheService cacheService;
    private final GameEventProducer eventProducer;
    private final ItadService itadService;
    private final GameEsRepository gameEsRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public GameService(GameRepository gameRepository,
                       GamePriceRepository gamePriceRepository,
                       AlertRepository alertRepository,
                       GameCacheService cacheService,
                       GameEventProducer eventProducer,
                       ItadService itadService,
                       GameEsRepository gameEsRepository,
                       ElasticsearchOperations elasticsearchOperations, GameEsRepository gameEsRepository1, ElasticsearchOperations elasticsearchOperations1) {
        this.gameRepository = gameRepository;
        this.gamePriceRepository = gamePriceRepository;
        this.alertRepository = alertRepository;
        this.cacheService = cacheService;
        this.eventProducer = eventProducer;
        this.itadService = itadService;
        this.gameEsRepository = gameEsRepository1;
        this.elasticsearchOperations = elasticsearchOperations1;
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

    // 취향 기반 추천 — 같은 장르 게임 반환 (ES 연동)
    public List<Game> recommend(List<Long> likedAppIds) {

        if (likedAppIds == null || likedAppIds.isEmpty()) {
            return gameRepository.findAll(
                    PageRequest.of(0, 10)).getContent();
        }

        // 좋아하는 게임들의 장르/태그 수집
        List<Game> likedGames = likedAppIds.stream()
                .map(id -> gameRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (likedGames.isEmpty()) {
            return gameRepository.findAll(
                    PageRequest.of(0, 10)).getContent();
        }

        // More Like This 쿼리 생성
        // 좋아하는 게임들과 유사한 게임을 ES에서 검색
        List<String> likedIds = likedAppIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());

        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .moreLikeThis(mlt -> mlt
                                        .fields("title", "genre", "tags", "description")
                                        // 이 필드들을 기준으로 유사도 계산
                                        .like(like -> like
                                                .document(doc -> doc
                                                        .index("games")
                                                        .id(likedIds.get(0))))
                                        .minTermFreq(1)
                                        // 최소 1번 등장한 단어만 유사도 계산에 사용
                                        .minDocFreq(1)
                                        // 최소 1개 문서에 등장한 단어만 사용
                                        .maxQueryTerms(25)
                                // 최대 25개 단어로 유사도 계산
                        )
                )
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<Game> hits =
                elasticsearchOperations.search(query, Game.class);

        // 좋아하는 게임 제외하고 반환
        Set<Long> likedSet = new HashSet<>(likedAppIds);
        return hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .filter(g -> !likedSet.contains(g.getSteamAppId()))
                .collect(Collectors.toList());
    }

    // 가격 업데이트
    @Transactional
    public void updatePrice(Game game, Integer newPrice) {
        Integer oldPrice = game.getCurrentPrice();
        game.updatePrice(newPrice);
        // DB 저장
        gameRepository.save(game);
        // ES 인덱싱 (동기화)
        gameEsRepository.save(game);
        // DB에 저장할 때 ES에도 자동으로 인덱싱

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

    public List<Game> getPopularGames() {
        // steam_app_id 기준으로 최근 추가된 순서 = Steam charts 순서
        return gameRepository.findAll(
                PageRequest.of(0, 20,
                        Sort.by(Sort.Direction.DESC, "updatedAt"))
        ).getContent();
    }

    public List<Map<String, Object>> getOnSaleGames() {
        // ITAD deals API로 현재 할인 중인 게임 조회
        return itadService.getCurrentDeals();
    }
}