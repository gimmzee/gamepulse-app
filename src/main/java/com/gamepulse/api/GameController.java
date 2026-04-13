package com.gamepulse.api;

import com.gamepulse.domain.game.Game;
import com.gamepulse.infra.es.GameDocument;
import com.gamepulse.infra.es.GameEsRepository;
import com.gamepulse.domain.game.GamePrice;
import com.gamepulse.service.GameService;
import com.gamepulse.service.ItadService;
import com.gamepulse.service.PriceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;
    private final ItadService itadService;
    private final PriceService priceService;
    private final GameEsRepository gameEsRepository;

    public GameController(GameService gameService, ItadService itadService, PriceService priceService, GameEsRepository gameEsRepository) {
        this.gameService = gameService;
        this.itadService = itadService;
        this.priceService = priceService;
        this.gameEsRepository = gameEsRepository;
    }

    // 기존 DB 게임을 ES에 일괄 인덱싱
    @PostMapping("/admin/reindex")
    public String reindex() {
        List<Game> allGames = gameService.getAllGames();
        List<GameDocument> docs = allGames.stream()
                .map(GameDocument::from)
                .collect(Collectors.toList());
        gameEsRepository.saveAll(docs);
        return "Reindexed " + docs.size() + " games to Elasticsearch";
    }

    @GetMapping("/search")
    public List<?> search(@RequestParam String q) {
        // DB에서 먼저 검색
        List<Game> dbResults = gameService.search(q);

        if (!dbResults.isEmpty()) {
            return dbResults;
        }

        // DB에 없으면 ITAD에서 검색
        return itadService.searchGames(q);
    }

    @GetMapping("/{appId}")
    public Game getGame(@PathVariable Long appId) {
        return gameService.getGame(appId);
    }

    @GetMapping("/{appId}/prices")
    public List<GamePrice> getPriceHistory(@PathVariable Long appId) {
        return gameService.getPriceHistory(appId);
    }

    @GetMapping("/recommend")
    public List<Game> recommend(@RequestParam List<Long> likedAppIds) {
        return gameService.recommend(likedAppIds);
    }

    @PostMapping("/{appId}/alert")
    public String setAlert(
            @PathVariable Long appId,
            @RequestParam Integer targetPrice,
            @RequestParam String email
    ) {
        gameService.setAlert(appId, targetPrice, email);
        return "Alert registered for " + email;
    }

    // 플랫폼별 현재 가격 비교
    @GetMapping("/{appId}/platform-prices")
    public Map<String, Object> getPlatformPrices(@PathVariable Long appId) {
        return itadService.getPlatformPrices(appId);
    }

    // 가격 개요 (현재 최저가 + 역대 최저가)
    @GetMapping("/{appId}/price-overview")
    public Map getPriceOverview(@PathVariable Long appId) {
        return itadService.getPriceOverview(appId);
    }

    // 관리자용: 즉시 인기 게임 수집 실행
    @PostMapping("/admin/collect-popular")
    public String collectPopularGames() {
        priceService.collectPopularGames();
        return "Popular games collection triggered";
    }

    @GetMapping("/{appId}/itad-history")
    public List<Map> getItadHistory(@PathVariable Long appId) {
        return itadService.getGamePriceHistory(appId);
    }

    // 인기 게임 (현재 플레이어 수 기준 - Steam charts 데이터 활용)
    @GetMapping("/popular")
    public List<Game> getPopularGames() {
        // Steam 인기 게임 순서대로 저장된 DB 데이터 반환
        // 매일 새벽 2시 스케줄러가 업데이트
        return gameService.getPopularGames();
    }

    // 최근 할인 게임 (ITAD에서 현재 할인 중인 게임)
    @GetMapping("/on-sale")
    public List<Map<String, Object>> getOnSaleGames() {
        return gameService.getOnSaleGames();
    }
}